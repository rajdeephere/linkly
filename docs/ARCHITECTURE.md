# Linkly — Architecture

Diagrams + **Architecture Decision Records (ADRs)** for the Linkly platform.
Diagrams are [Mermaid](https://mermaid.js.org/) and render natively on GitHub.

> **Design thesis:** the product is the *redirect engine + the data it generates*, not the "shorten" form. Every decision below optimizes the **read hot path**, keeps **analytics off it**, and stays **correct under cache staleness** and **safe under abuse**.

**Contents**
1. [System context](#1-system-context)
2. [Component architecture](#2-component-architecture)
3. [Resolve — hot path (edge hit)](#3-resolve--hot-path-edge-hit)
4. [Resolve — cold / complex path (edge miss → origin)](#4-resolve--cold--complex-path-edge-miss--origin)
5. [Create a link (write path)](#5-create-a-link-write-path)
6. [Edit / delete — cache invalidation](#6-edit--delete--cache-invalidation)
7. [Custom domain onboarding + TLS](#7-custom-domain-onboarding--tls)
8. [Analytics pipeline](#8-analytics-pipeline)
9. [Short-code generation (KGS)](#9-short-code-generation-kgs)
10. [Data model](#10-data-model)
11. [Deployment (AWS)](#11-deployment-aws)
12. [Architecture Decision Records](#12-architecture-decision-records-adrs)

---

## 1. System context

```mermaid
flowchart LR
    visitor([Visitor clicking a short link])
    creator([Creator / marketer / developer])

    subgraph Linkly
        edge[Edge Resolver<br/>global KV]
        origin[Origin Resolver<br/>Spring Boot]
        api[Management API<br/>Spring Boot]
        web[Web App<br/>Next.js]
        analytics[(Analytics<br/>ClickHouse)]
    end

    sb[[Google Safe Browsing]]
    dns[[Custom-domain DNS + ACME TLS]]

    visitor -->|GET /code| edge
    edge -->|302 redirect| visitor
    edge -.miss.-> origin
    creator -->|manage links| web
    web -->|REST| api
    api --> sb
    api --> dns
    api --> analytics
    edge -.click events.-> analytics
```

---

## 2. Component architecture

The **resolve path** (edge + origin) is physically separate from the **management API** so a viral link can never starve the dashboard (ADR-1). Metadata lives in Postgres; clicks live in ClickHouse (ADR-4, ADR-7).

```mermaid
flowchart TB
    subgraph HotPath["Resolve path (reads, 100:1)"]
        direction TB
        EDGE["Edge Resolver<br/>(Cloudflare Workers / Vercel Edge)<br/>Edge KV — hot links"]
        ORIGIN["Origin Resolver (Spring Boot)<br/>full rule eval · password · expiry"]
        REDIS[("Redis<br/>cache-aside · KGS pool · rate limits")]
        EDGE -->|cache miss| ORIGIN
        ORIGIN --> REDIS
    end

    subgraph MgmtPath["Management path (writes / admin)"]
        direction TB
        WEB["Web App (Next.js)"]
        API["Management API (Spring Boot)<br/>auth · links · domains · rules · keys · bulk"]
        KGS["Key Generation Service<br/>base62 pool"]
        WEB -->|REST| API
        API --> KGS
    end

    PG[("PostgreSQL<br/>links · domains · rules · workspaces")]
    STREAM{{"Kafka / Kinesis<br/>click events"}}
    CONSUMER["Analytics consumer<br/>enrich geo / device / bot"]
    CH[("ClickHouse<br/>click events (append-only)")]

    ORIGIN --> PG
    API --> PG
    API -->|warm / purge| EDGE
    EDGE -. fire-and-forget .-> STREAM
    ORIGIN -. fire-and-forget .-> STREAM
    STREAM --> CONSUMER --> CH
    API -->|query| CH
```

---

## 3. Resolve — hot path (edge hit)

The 99% case: hot link served entirely at the edge in <10ms; the click event is emitted **after** the redirect and never awaited (ADR-3, ADR-4).

```mermaid
sequenceDiagram
    autonumber
    actor V as Visitor
    participant E as Edge Resolver (KV)
    participant S as Event Stream (Kafka)

    V->>E: GET go.acme.com/launch
    E->>E: lookup code in Edge KV (HIT)
    E->>E: evaluate simple rule (geo / device from edge ctx)
    E-->>V: 302 Location: destination
    Note over E,S: after responding — non-blocking
    E--)S: click event {code, ts, geo, ua, ref}
```

---

## 4. Resolve — cold / complex path (edge miss → origin)

Cold link, or one needing full rule evaluation / password / expiry checks. Origin resolves from Redis→Postgres, then **warms the edge** so the next hit is hot (ADR-3, ADR-8).

```mermaid
sequenceDiagram
    autonumber
    actor V as Visitor
    participant E as Edge Resolver
    participant O as Origin Resolver (Spring Boot)
    participant R as Redis
    participant P as Postgres
    participant S as Event Stream

    V->>E: GET /code
    E->>E: Edge KV lookup (MISS)
    E->>O: forward resolve(code, host, ctx)
    O->>R: GET link:{host}:{code}
    alt cache hit
        R-->>O: link record
    else cache miss
        O->>P: SELECT link WHERE domain,code
        P-->>O: link record
        O->>R: SET link:{host}:{code} (TTL)
    end
    O->>O: check expiry / clickLimit / password
    O->>O: evaluate routing rules (geo / device / A-B sticky)
    alt expired or capped
        O-->>V: 410 Gone (or 302 expiresUrl)
    else ok
        O-->>V: 302 Location: destination
        O--)E: warm Edge KV (write-back)
        O--)S: click event
    end
```

---

## 5. Create a link (write path)

Not on the hot path, so it can afford a Safe-Browsing check and code allocation. Codes come from the KGS pool — no collision-retry loop on the write (ADR-2, ADR-9).

```mermaid
sequenceDiagram
    autonumber
    actor U as User / API client
    participant API as Management API
    participant SB as Safe Browsing
    participant KGS as Key Gen Service
    participant P as Postgres
    participant E as Edge KV

    U->>API: POST /v1/links {destinationUrl, alias?, domainId?}
    API->>API: validate URL scheme / format
    API->>SB: lookup destination (phishing / malware?)
    alt flagged
        SB-->>API: THREAT
        API-->>U: 422 rejected (unsafe URL)
    else clean
        SB-->>API: safe
        alt custom alias requested
            API->>P: INSERT link (unique domain+code)
            Note over API,P: unique constraint → clean 409 on clash
        else auto code
            API->>KGS: claim next base62 code
            KGS-->>API: code (non-sequential)
            API->>P: INSERT link
        end
        API--)E: warm Edge KV
        API-->>U: 201 {shortUrl, code}
    end
```

---

## 6. Edit / delete — cache invalidation

The correctness-critical path: an edited destination must not keep serving the old URL. Write-through + **active edge purge** + short TTL backstop (ADR-5, ADR-8).

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant API as Management API
    participant P as Postgres
    participant R as Redis
    participant E as Edge KV

    U->>API: PATCH /v1/links/{id} {destinationUrl}
    API->>P: UPDATE link
    API->>R: DEL link:{host}:{code}
    API->>E: PURGE key (fan-out to edge locations)
    Note over E: brief eventual-consistency window;<br/>short TTL is the backstop
    API-->>U: 200 updated
    Note over U,E: 302 (never 301) guarantees future clicks<br/>re-resolve through us — analytics + edits keep working
```

---

## 7. Custom domain onboarding + TLS

Branded domains are the #1 paid feature. Ownership is proven via DNS before any cert is issued (prevents cert-mining), then TLS is provisioned per hostname (ADR-6).

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant API as Management API
    participant P as Postgres
    participant DNS as DNS (user-controlled)
    participant TLS as ACME / Caddy on-demand

    U->>API: POST /v1/domains {hostname: go.acme.com}
    API->>P: INSERT domain (verified=false, token)
    API-->>U: TXT/CNAME record to add
    U->>DNS: add verification record
    U->>API: POST /v1/domains/{id}/verify
    API->>DNS: resolve TXT/CNAME
    alt record present
        API->>P: mark verified=true
        API->>TLS: request cert for go.acme.com
        TLS-->>API: cert issued (tlsStatus=active)
        API-->>U: domain live over HTTPS
    else missing
        API-->>U: 400 not verified yet
    end
```

---

## 8. Analytics pipeline

Fire-and-forget from the resolver → stream → enrichment → columnar OLAP. An analytics outage can never take down redirects (ADR-4, ADR-7).

```mermaid
flowchart LR
    R["Resolver<br/>(edge / origin)"] -->|"click event<br/>(non-blocking)"| K{{Kafka / Kinesis}}
    K --> C["Enrichment consumer<br/>geo · device · browser · bot flag<br/>IP → hashed (GDPR)"]
    C -->|batch insert| CH[("ClickHouse<br/>click_events")]
    CH --> Q["Aggregation queries<br/>timeseries · byCountry · byDevice · byReferrer"]
    Q --> API[Management API] --> DASH["Dashboard (Next.js)"]
```

**Why ClickHouse:** click analytics are `COUNT / GROUP BY country, day, device` over billions of append-only rows — a columnar store scans only needed columns and compresses hugely; a row store chokes at scale.

---

## 9. Short-code generation (KGS)

The classic interview question, decided: a **Key Generation Service** hands out unique base62 codes from a pre-allocated pool — beating hash-truncate (collisions) and raw auto-increment (enumerable) (ADR-2).

```mermaid
flowchart TB
    subgraph Offline / background
        GEN["Generate base62 keys<br/>(shuffled counter → non-sequential)"]
        POOL[("Key pool in Redis/DB<br/>status: available | used")]
        GEN --> POOL
    end

    subgraph On create
        REQ[Create-link request] --> CLAIM["Claim next available key<br/>(atomic pop)"]
        POOL --> CLAIM
        CLAIM --> ASSIGN["Assign to link · mark used"]
    end

    REFILL{{"Low-watermark refill<br/>keeps pool topped up"}}
    POOL -.->|below threshold| REFILL -.-> GEN
```

**Properties:** O(1) allocation · no collision retry on the write path · non-sequential ⇒ not enumerable (defends against scraping) · widen code length gracefully as space fills.

---

## 10. Data model

Relational metadata in Postgres; the immutable click fact table in ClickHouse. Uniqueness is `(domain, code)` — two tenants can both own `/launch`.

```mermaid
erDiagram
    WORKSPACE ||--o{ MEMBERSHIP : has
    USER ||--o{ MEMBERSHIP : in
    WORKSPACE ||--o{ DOMAIN : owns
    WORKSPACE ||--o{ LINK : owns
    DOMAIN ||--o{ LINK : hosts
    LINK ||--o{ ROUTING_RULE : has
    WORKSPACE ||--o{ API_KEY : has
    WORKSPACE ||--o{ BIO_PAGE : has
    LINK ||--o{ CLICK_EVENT : generates

    WORKSPACE {
        uuid id PK
        string name
        string plan
    }
    USER {
        uuid id PK
        string email
        string name
    }
    MEMBERSHIP {
        uuid workspace_id FK
        uuid user_id FK
        string role "owner|admin|member"
    }
    DOMAIN {
        uuid id PK
        uuid workspace_id FK
        string hostname
        bool verified
        string tls_status
    }
    LINK {
        uuid id PK
        uuid workspace_id FK
        uuid domain_id FK
        string code "unique per domain"
        string destination_url
        timestamp expires_at
        int click_limit
        string password_hash
    }
    ROUTING_RULE {
        uuid id PK
        uuid link_id FK
        string type "geo|device|os|time|ab"
        string condition
        string destination_url
        int weight
    }
    API_KEY {
        uuid id PK
        uuid workspace_id FK
        string hashed_key
    }
    BIO_PAGE {
        uuid id PK
        uuid workspace_id FK
        string slug
    }
    CLICK_EVENT {
        uuid link_id "denormalized, in ClickHouse"
        timestamp ts
        string ip_hash
        string country
        string device
        string referrer
        bool is_bot
    }
```

---

## 11. Deployment (AWS)

```mermaid
flowchart TB
    subgraph Global Edge
        CF["Edge Resolver<br/>Cloudflare Workers / Vercel Edge + KV"]
        VER["Next.js web + link-in-bio<br/>(Vercel)"]
    end

    subgraph AWS Region
        ALB[Application Load Balancer]
        subgraph ECS/EKS
            RES["resolver pods<br/>(autoscaled — read heavy)"]
            APIP["api pods"]
            CONS["analytics consumer pods"]
        end
        RDS[("RDS Postgres<br/>Multi-AZ")]
        EC[("ElastiCache Redis")]
        MSK{{"MSK / Kinesis"}}
        CHC[("ClickHouse<br/>(self-managed / Cloud)")]
        S3[("S3 — QR / exports")]
    end

    users([Visitors]) --> CF
    CF -.miss.-> ALB --> RES --> EC --> RDS
    creators([Creators]) --> VER --> ALB --> APIP --> RDS
    APIP --> EC
    APIP --> S3
    CF -.events.-> MSK
    RES -.events.-> MSK
    MSK --> CONS --> CHC
    APIP --> CHC

    OTEL["OpenTelemetry → Prometheus / Grafana"]
    RES -.traces.-> OTEL
    APIP -.traces.-> OTEL
```

---

## 12. Architecture Decision Records (ADRs)

Each is a defensible decision with a real trade-off. *(Mirrors §7 of the project spec.)*

| # | Decision | Why (short) | Key cost / trade-off |
|---|---|---|---|
| **ADR-1** | Redirect resolver is a **separate service** from the management API | Reads outrun writes 100:1+ with a totally different profile; a viral link must not starve the dashboard | Two deployables; shared lookup code must be factored carefully |
| **ADR-2** | Short codes via a **Key Generation Service** (pre-allocated base62), not hash-truncate or raw auto-increment | O(1) allocation, guaranteed unique, **non-enumerable** (shuffled counter) → no scraping, no collision-retry on write | A pool to run + refill; must not re-issue a used key after restart |
| **ADR-3** | **Hybrid** edge KV + Spring Boot origin resolve | Edge = <10ms global for hot links; origin = source of truth + complex/cold links. Defends both the modern-edge and enterprise-JVM story | Two resolve implementations to keep in sync |
| **ADR-4** | Analytics **off the hot path**: fire-and-forget → stream → ClickHouse | Redirect latency/availability must not depend on the analytics store | Eventually consistent (secs); at-least-once ⇒ dedup on `(linkId, ts, ipHash)` |
| **ADR-5** | Default **302** (temporary), not 301 | 301 is cached forever by browsers → analytics silently stop + edits never propagate. 302 keeps every click flowing through us | Marginally less SEO juice (offer 301 opt-in per link) |
| **ADR-6** | Multi-tenant **custom domains** with DNS verification + on-demand per-host TLS | #1 paid feature; issue certs only after proving ownership (anti cert-mining); route by `Host` | Auto cert issuance is an attack surface; DNS-propagation UX is fiddly |
| **ADR-7** | **ClickHouse** for analytics, not Postgres/Timescale | Columnar store scans only needed columns over billions of append-only rows; row store chokes | Extra store; denormalize (no cross-store joins in query) |
| **ADR-8** | Cache invalidation via **write-through + explicit edge purge + short TTL** | An edited link serving the old URL is a correctness bug; TTL alone leaves stale windows too long | Purge fan-out is eventually consistent; brief stale window |
| **ADR-9** | **Safe-Browsing scan** on create + aggressive rate limits | Shorteners are phishing magnets; a blocklisted domain kills the product | External dependency + latency on create (acceptable — not the hot path) |
| **ADR-10** | Smart **routing rules evaluated at resolve time**, A-B with **sticky bucketing** | Rules must run on the hot path near-zero-latency; A-B without sticky buckets corrupts the experiment | Rule logic duplicated edge+origin (see ADR-3); geo accuracy depends on edge provider |
| **ADR-11** | **Next.js App Router** frontend on Vercel, decoupled from the resolver | Sets the modern SaaS aesthetic bar; server components; cheap edge hosting for link-in-bio; shows full-stack range | Second ecosystem (TS); auth spans NextAuth session ↔ API JWT |

> Full narrative form (alternatives considered, "revisit if" triggers) lives in the project spec, §7, in the `random-thoughts` repo.
