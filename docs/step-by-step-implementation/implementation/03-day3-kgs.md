# Impl 03 — Day 3: Key Generation Service (non-enumerable codes)

**Outcome:** short codes come from a **KGS** — unique forever, non-sequential, O(1) claim, no write-path
collision check. Proven: a 500-code sequential enumeration walk finds **0** of 200 real links.
**Status:** ✅ shipped & verified. Replaces the Day-2 random+retry generator.

## Prerequisites
- Day 2 ([02-day2-shorten-resolve](./02-day2-shorten-resolve.md)); infra up incl. **Redis** (now
  load-bearing).

## Design (ADR-0002 · ARCHITECTURE §9)
A monotonic Redis counter drives generation; each counter value is mapped through an **affine
permutation** over the code space and base62-encoded; results are buffered in a Redis **pool** claimed by
atomic `SPOP`.

```
INCR kgs:counter → n
scramble(n) = (A·n + B) mod 62^7        # gcd(A, 62^7)=1 ⇒ bijection ⇒ every n → a distinct code
code        = base62(scramble(n), 7)     # scattered across the space (not sequential)
pool (SADD) ── claim: SPOP kgs:pool ── low-watermark refill
```
Bijection ⇒ **no code is ever regenerated** (uniqueness needs no lookup); large `A` ⇒ consecutive
counters scatter (**non-enumerable**).

## Build order (backend — `apps/api`)
1. `link/Base62` — fixed-width (7) base62 encode.
2. `link/KeyGenerationService` — counter `INCR kgs:counter`; `scramble` via `BigInteger`
   (A=2,654,435,761 — odd, not ÷31 → coprime to 62^7=2^7·31^7; B=1,013,904,223); pool `kgs:pool`
   (`SADD`/`SPOP`); `claim()` (SPOP → refill-if-empty → maybe-refill), `maybeRefill()` (< 200 →
   +1000), `warmUp()` on `ApplicationReadyEvent` (best-effort, try/catch so Redis-not-ready ≠ boot fail),
   `poolSize()`.
3. `link/LinkService.create` — now just `kgs.claim()` → insert (no pre-check, no retry loop). The
   `(code)` unique index stays as a backstop; on the should-never-happen violation, claim once more.
4. Delete `link/CodeGenerator` (Day-2 interim).
5. `web/PingController` — report `kgsPool` (pool depth) alongside `links`.
6. Web: `lib/api.ts` + `page.tsx` read the new `links` / `kgsPool` fields.

## Verify (the proof)
```bash
# create 200 links, capture codes → visibly scattered (not sequential)
for i in $(seq 1 200); do curl -s -X POST localhost:8081/v1/links \
  -H 'Content-Type: application/json' -d "{\"destinationUrl\":\"https://example.com/page/$i\"}" \
  | sed -n 's/.*"code":"\([^"]*\)".*/\1/p'; done
# ENUMERATION ATTACK: walk 500 sequential codes 0000000..0000499
for n in $(seq 0 499); do curl -s -o /dev/null -w "%{http_code}\n" \
  "localhost:8081/$(printf '%07d' $n)"; done | grep -c 302
```
Result: **0 / 500** sequential probes hit a real link (an auto-increment scheme would have all 200 in
that range). Real codes resolve (302). `kgsPool` warms to 1000 at startup, sits at 800 after 200 claims.

## Why (one line each)
Bijective scramble → unique-forever **without** any lookup, and non-sequential → not walkable.
Pool + `SPOP` → O(1) claim off the DB entirely (vs Day-2's `existsByCode` read + insert-retry).
Counter/pool in Redis (persistent) → codes never reused across restarts.

## Caveat (documented, not a bug)
The affine scramble defeats **naive sequential enumeration** (the real threat) but is
obfuscation-grade, not cryptographic — someone collecting many codes could, in principle, solve for A/B.
Hardening: a keyed format-preserving cipher (Feistel/FPE). Also: past 62^7 codes, widen `CODE_LENGTH`.

## Decisions referenced
- [ADR-0002 KGS / base62 codes](../../adr/0002-kgs-base62-codes.md)
