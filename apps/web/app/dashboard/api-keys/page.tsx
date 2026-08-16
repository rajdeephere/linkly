"use client";

import { api } from "@/lib/auth";
import { useCallback, useEffect, useState } from "react";

type ApiKey = { id: string; name: string; prefix: string; role: string; createdAt: string };
type Created = { id: string; name: string; key: string; prefix: string };

const input =
  "rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring";

export default function ApiKeysPage() {
  const [keys, setKeys] = useState<ApiKey[]>([]);
  const [name, setName] = useState("");
  const [busy, setBusy] = useState(false);
  const [created, setCreated] = useState<Created | null>(null);
  const [copied, setCopied] = useState(false);

  const load = useCallback(async () => {
    const res = await api("/v1/api-keys");
    if (res.ok) setKeys(await res.json());
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    try {
      const res = await api("/v1/api-keys", { method: "POST", body: JSON.stringify({ name: name.trim() }) });
      if (res.ok) {
        setCreated(await res.json());
        setName("");
        await load();
      }
    } finally {
      setBusy(false);
    }
  }

  async function copy() {
    if (!created) return;
    await navigator.clipboard.writeText(created.key);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  return (
    <div>
      <h1 className="mb-1 text-xl font-semibold">API keys</h1>
      <p className="mb-4 text-sm text-muted-foreground">
        Use a key as a Bearer token to call the Linkly API programmatically. The secret is shown once.
      </p>

      <form onSubmit={create} className="mb-4 flex gap-2 rounded-lg border bg-card p-4">
        <input
          className={`flex-1 ${input}`}
          placeholder="Key name (e.g. ci-pipeline)"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <button
          disabled={busy}
          className="rounded-md bg-primary px-5 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50"
        >
          {busy ? "…" : "Create key"}
        </button>
      </form>

      {created && (
        <div className="mb-4 rounded-lg border border-green-500/40 bg-green-500/10 p-4">
          <p className="mb-2 text-sm font-medium text-green-600">
            Copy your key now — you won’t see it again.
          </p>
          <div className="flex items-center gap-2">
            <code className="flex-1 break-all rounded-md bg-background px-3 py-2 font-mono text-sm">{created.key}</code>
            <button onClick={copy} className="rounded-md border px-3 py-2 text-sm hover:bg-accent">
              {copied ? "Copied" : "Copy"}
            </button>
          </div>
          <button onClick={() => setCreated(null)} className="mt-2 text-xs text-muted-foreground hover:text-foreground">
            Dismiss
          </button>
        </div>
      )}

      <div className="overflow-hidden rounded-lg border">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-left text-xs uppercase text-muted-foreground">
            <tr>
              <th className="px-4 py-2">Name</th>
              <th className="px-4 py-2">Prefix</th>
              <th className="px-4 py-2">Role</th>
            </tr>
          </thead>
          <tbody>
            {keys.length === 0 && (
              <tr>
                <td colSpan={3} className="px-4 py-8 text-center text-muted-foreground">
                  No API keys yet.
                </td>
              </tr>
            )}
            {keys.map((k) => (
              <tr key={k.id} className="border-t">
                <td className="px-4 py-2">{k.name || <span className="text-muted-foreground">—</span>}</td>
                <td className="px-4 py-2 font-mono text-muted-foreground">{k.prefix}…</td>
                <td className="px-4 py-2">{k.role}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
