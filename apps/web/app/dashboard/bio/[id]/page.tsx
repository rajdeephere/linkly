"use client";

import { api } from "@/lib/auth";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

type Block = { id: string; label: string; url: string; position: number };
type Bio = {
  id: string;
  slug: string;
  title: string;
  avatarUrl: string | null;
  bio: string | null;
  theme: string;
  blocks: Block[];
};

const input =
  "rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring";

export default function BioEditorPage({ params }: { params: { id: string } }) {
  const [page, setPage] = useState<Bio | null>(null);
  const [label, setLabel] = useState("");
  const [url, setUrl] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    const res = await api(`/v1/bio/${params.id}`);
    if (res.ok) setPage(await res.json());
    else setError("Couldn't load this page.");
  }, [params.id]);

  useEffect(() => {
    load();
  }, [load]);

  async function addBlock(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api(`/v1/bio/${params.id}/blocks`, {
        method: "POST",
        body: JSON.stringify({ label: label.trim(), url: url.trim() }),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => null);
        setError(b?.fieldErrors?.url ?? b?.message ?? `Failed (${res.status})`);
        return;
      }
      setLabel("");
      setUrl("");
      setPage(await res.json());
    } finally {
      setBusy(false);
    }
  }

  async function deleteBlock(blockId: string) {
    const res = await api(`/v1/bio/${params.id}/blocks/${blockId}`, { method: "DELETE" });
    if (res.ok) setPage(await res.json());
  }

  /** Move a block one slot up/down: reorder the id list locally, then PUT the new order. */
  async function move(blockId: string, dir: -1 | 1) {
    if (!page) return;
    const ordered = page.blocks.slice().sort((a, b) => a.position - b.position);
    const i = ordered.findIndex((b) => b.id === blockId);
    const j = i + dir;
    if (i < 0 || j < 0 || j >= ordered.length) return;
    [ordered[i], ordered[j]] = [ordered[j], ordered[i]];
    const res = await api(`/v1/bio/${params.id}/blocks/order`, {
      method: "PUT",
      body: JSON.stringify({ order: ordered.map((b) => b.id) }),
    });
    if (res.ok) setPage(await res.json());
  }

  if (!page) {
    return (
      <div className="text-sm text-muted-foreground">
        {error ?? "Loading…"}
        <div className="mt-2">
          <Link href="/dashboard/bio" className="hover:text-foreground">
            ← Bio pages
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div>
      <Link href="/dashboard/bio" className="text-sm text-muted-foreground hover:text-foreground">
        ← Bio pages
      </Link>
      <div className="mb-4 mt-1 flex items-center justify-between">
        <h1 className="text-xl font-semibold">{page.title || page.slug}</h1>
        <a
          href={`/bio/${page.slug}`}
          target="_blank"
          rel="noopener"
          className="text-sm text-muted-foreground hover:text-foreground"
        >
          View public page ↗
        </a>
      </div>

      <form onSubmit={addBlock} className="mb-4 flex flex-col gap-3 rounded-lg border bg-card p-4 sm:flex-row">
        <input
          className={`sm:w-48 ${input}`}
          placeholder="Label (e.g. Website)"
          value={label}
          onChange={(e) => setLabel(e.target.value)}
          required
        />
        <input
          className={`flex-1 ${input}`}
          type="url"
          placeholder="https://…"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          required
        />
        <button
          disabled={busy}
          className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50"
        >
          {busy ? "…" : "Add link"}
        </button>
      </form>
      {error && <p className="mb-3 text-sm text-red-500">{error}</p>}

      <div className="flex flex-col gap-2">
        {page.blocks.length === 0 && (
          <p className="rounded-lg border px-4 py-8 text-center text-sm text-muted-foreground">
            No links on this page yet — add one above.
          </p>
        )}
        {page.blocks
          .slice()
          .sort((a, b) => a.position - b.position)
          .map((blk, idx, arr) => (
            <div key={blk.id} className="flex items-center justify-between gap-3 rounded-lg border p-3">
              <div className="min-w-0">
                <div className="font-medium">{blk.label}</div>
                <div className="max-w-md truncate font-mono text-xs text-muted-foreground">{blk.url}</div>
              </div>
              <div className="flex shrink-0 items-center gap-1 text-xs">
                <button
                  onClick={() => move(blk.id, -1)}
                  disabled={idx === 0}
                  aria-label="Move up"
                  className="rounded-md border px-2 py-1 hover:bg-accent disabled:opacity-30"
                >
                  ↑
                </button>
                <button
                  onClick={() => move(blk.id, 1)}
                  disabled={idx === arr.length - 1}
                  aria-label="Move down"
                  className="rounded-md border px-2 py-1 hover:bg-accent disabled:opacity-30"
                >
                  ↓
                </button>
                <button
                  onClick={() => deleteBlock(blk.id)}
                  className="ml-1 rounded-md border border-red-500/40 px-2 py-1 text-red-500 hover:bg-red-500/10"
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
      </div>
    </div>
  );
}
