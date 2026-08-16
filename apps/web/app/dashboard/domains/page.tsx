"use client";

import { api } from "@/lib/auth";
import { useCallback, useEffect, useState } from "react";

type Domain = {
  id: string;
  hostname: string;
  verified: boolean;
  tlsStatus: string;
  dnsRecordName: string | null;
  dnsRecordValue: string | null;
};

const input =
  "rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring";

export default function DomainsPage() {
  const [domains, setDomains] = useState<Domain[]>([]);
  const [hostname, setHostname] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    const res = await api("/v1/domains");
    if (res.ok) setDomains(await res.json());
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function add(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api("/v1/domains", {
        method: "POST",
        body: JSON.stringify({ hostname: hostname.trim().toLowerCase() }),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => null);
        setError(b?.message ?? `Failed (${res.status})`);
        return;
      }
      setHostname("");
      await load();
    } finally {
      setBusy(false);
    }
  }

  async function simulate(id: string) {
    await api(`/v1/domains/${id}/dns/simulate`, { method: "POST" });
    setError(null);
  }

  async function verify(id: string) {
    const res = await api(`/v1/domains/${id}/verify`, { method: "POST" });
    if (!res.ok) {
      const b = await res.json().catch(() => null);
      setError(b?.message ?? "Verification failed — DNS record not found yet.");
    }
    await load();
  }

  return (
    <div>
      <h1 className="mb-1 text-xl font-semibold">Custom domains</h1>
      <p className="mb-4 text-sm text-muted-foreground">
        Bring your own domain (e.g. <code>go.acme.com</code>). Add it, publish the TXT record, then verify.
      </p>

      <form onSubmit={add} className="mb-2 flex gap-2 rounded-lg border bg-card p-4">
        <input
          className={`flex-1 ${input}`}
          placeholder="go.acme.com"
          value={hostname}
          onChange={(e) => setHostname(e.target.value)}
          required
        />
        <button
          disabled={busy}
          className="rounded-md bg-primary px-5 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50"
        >
          {busy ? "…" : "Add domain"}
        </button>
      </form>
      {error && <p className="mb-3 text-sm text-red-500">{error}</p>}

      <div className="flex flex-col gap-3">
        {domains.length === 0 && (
          <p className="rounded-lg border px-4 py-8 text-center text-sm text-muted-foreground">
            No custom domains yet.
          </p>
        )}
        {domains.map((d) => (
          <div key={d.id} className="rounded-lg border p-4">
            <div className="flex items-center justify-between">
              <span className="font-mono">{d.hostname}</span>
              <div className="flex items-center gap-2 text-xs">
                <span
                  className={`rounded-full px-2 py-0.5 ${
                    d.verified ? "bg-green-500/15 text-green-500" : "bg-yellow-500/15 text-yellow-600"
                  }`}
                >
                  {d.verified ? "verified" : "pending"}
                </span>
                <span className="rounded-full bg-muted px-2 py-0.5 text-muted-foreground">TLS: {d.tlsStatus}</span>
              </div>
            </div>

            {!d.verified && d.dnsRecordName && (
              <div className="mt-3 rounded-md bg-muted/50 p-3 text-xs">
                <p className="mb-2 text-muted-foreground">Add this TXT record at your DNS provider:</p>
                <div className="font-mono">
                  <div>
                    <span className="text-muted-foreground">name </span>
                    {d.dnsRecordName}
                  </div>
                  <div className="break-all">
                    <span className="text-muted-foreground">value </span>
                    {d.dnsRecordValue}
                  </div>
                </div>
                <div className="mt-3 flex gap-2">
                  <button
                    onClick={() => simulate(d.id)}
                    className="rounded-md border px-3 py-1 hover:bg-accent hover:text-accent-foreground"
                    title="Dev-only: stands in for you publishing the TXT record"
                  >
                    Simulate DNS
                  </button>
                  <button
                    onClick={() => verify(d.id)}
                    className="rounded-md bg-primary px-3 py-1 font-medium text-primary-foreground hover:opacity-90"
                  >
                    Verify
                  </button>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
