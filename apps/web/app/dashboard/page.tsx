"use client";

import { api } from "@/lib/auth";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

type LinkRow = { id: string; code: string; shortUrl: string; destinationUrl: string };

export default function DashboardPage() {
  const [links, setLinks] = useState<LinkRow[]>([]);
  const [url, setUrl] = useState("");
  const [alias, setAlias] = useState("");
  const [clickLimit, setClickLimit] = useState("");
  const [showOpts, setShowOpts] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState<string | null>(null);
  const [qr, setQr] = useState<string | null>(null);

  const load = useCallback(async () => {
    const res = await api("/v1/links");
    if (res.ok) setLinks(await res.json());
  }, []);

  useEffect(() => { load(); }, [load]);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const body: Record<string, unknown> = { destinationUrl: url.trim() };
      if (alias.trim()) body.alias = alias.trim();
      if (clickLimit) body.clickLimit = Number(clickLimit);
      const res = await api("/v1/links", { method: "POST", body: JSON.stringify(body) });
      if (!res.ok) {
        const b = await res.json().catch(() => null);
        setError(b?.fieldErrors?.destinationUrl ?? b?.message ?? `Failed (${res.status})`);
        return;
      }
      setUrl(""); setAlias(""); setClickLimit("");
      await load();
    } finally {
      setLoading(false);
    }
  }

  async function del(id: string) {
    const res = await api(`/v1/links/${id}`, { method: "DELETE" });
    if (res.ok) setLinks((l) => l.filter((x) => x.id !== id));
    else if (res.status === 403) setError("Delete requires owner/admin.");
  }

  async function copy(u: string) {
    await navigator.clipboard.writeText(u);
    setCopied(u);
    setTimeout(() => setCopied(null), 1200);
  }

  async function showQr(id: string) {
    const res = await api(`/v1/links/${id}/qr?size=280`);
    if (res.ok) setQr(URL.createObjectURL(await res.blob()));
  }

  const input = "rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring";

  return (
    <div>
      <h1 className="mb-4 text-xl font-semibold">Links</h1>

      <form onSubmit={create} className="mb-2 flex flex-col gap-3 rounded-lg border bg-card p-4">
        <div className="flex gap-2">
          <input className={`flex-1 ${input}`} type="url" required placeholder="Paste a long URL…" value={url} onChange={(e) => setUrl(e.target.value)} />
          <button disabled={loading} className="rounded-md bg-primary px-5 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50">
            {loading ? "…" : "Shorten"}
          </button>
        </div>
        <button type="button" onClick={() => setShowOpts((v) => !v)} className="self-start text-xs text-muted-foreground hover:text-foreground">
          {showOpts ? "▾ Options" : "▸ Options"}
        </button>
        {showOpts && (
          <div className="grid grid-cols-2 gap-3">
            <input className={input} placeholder="custom alias" value={alias} onChange={(e) => setAlias(e.target.value)} />
            <input className={input} type="number" min={1} placeholder="click limit" value={clickLimit} onChange={(e) => setClickLimit(e.target.value)} />
          </div>
        )}
      </form>
      {error && <p className="mb-3 text-sm text-red-500">{error}</p>}

      <div className="overflow-hidden rounded-lg border">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-left text-xs uppercase text-muted-foreground">
            <tr><th className="px-4 py-2">Short link</th><th className="px-4 py-2">Destination</th><th className="px-4 py-2 text-right">Actions</th></tr>
          </thead>
          <tbody>
            {links.length === 0 && (
              <tr><td colSpan={3} className="px-4 py-8 text-center text-muted-foreground">No links yet — shorten one above.</td></tr>
            )}
            {links.map((l) => (
              <tr key={l.id} className="border-t">
                <td className="px-4 py-2">
                  <button onClick={() => copy(l.shortUrl)} className="font-mono text-blue-400 hover:underline">
                    {l.shortUrl.replace(/^https?:\/\//, "")}
                  </button>
                  {copied === l.shortUrl && <span className="ml-2 text-xs text-green-500">copied</span>}
                </td>
                <td className="max-w-[240px] truncate px-4 py-2 text-muted-foreground" title={l.destinationUrl}>{l.destinationUrl}</td>
                <td className="whitespace-nowrap px-4 py-2 text-right text-xs">
                  <button onClick={() => showQr(l.id)} className="text-muted-foreground hover:text-foreground">QR</button>
                  <Link href={`/analytics/${l.id}`} className="ml-3 text-muted-foreground hover:text-foreground">Analytics</Link>
                  <button onClick={() => del(l.id)} className="ml-3 text-red-500 hover:underline">Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {qr && (
        <div onClick={() => setQr(null)} className="fixed inset-0 flex items-center justify-center bg-black/60 p-4">
          <div className="rounded-lg bg-white p-4" onClick={(e) => e.stopPropagation()}>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={qr} alt="QR code" width={280} height={280} />
          </div>
        </div>
      )}
    </div>
  );
}
