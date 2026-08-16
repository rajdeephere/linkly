"use client";

import { api } from "@/lib/auth";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

type BioPage = { id: string; slug: string; title: string; theme: string; blocks: unknown[] };

const input =
  "rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring";

export default function BioListPage() {
  const [pages, setPages] = useState<BioPage[]>([]);
  const [slug, setSlug] = useState("");
  const [title, setTitle] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    const res = await api("/v1/bio");
    if (res.ok) setPages(await res.json());
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api("/v1/bio", {
        method: "POST",
        body: JSON.stringify({ slug: slug.trim().toLowerCase(), title: title.trim(), theme: "dark" }),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => null);
        setError(b?.fieldErrors?.slug ?? b?.message ?? `Failed (${res.status})`);
        return;
      }
      setSlug("");
      setTitle("");
      await load();
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <h1 className="mb-1 text-xl font-semibold">Link-in-bio pages</h1>
      <p className="mb-4 text-sm text-muted-foreground">
        A single public page (<code>/bio/&lt;slug&gt;</code>) that hosts a list of links — for social profiles.
      </p>

      <form onSubmit={create} className="mb-2 flex flex-col gap-3 rounded-lg border bg-card p-4 sm:flex-row">
        <input
          className={`sm:w-48 ${input}`}
          placeholder="slug (e.g. acme)"
          value={slug}
          onChange={(e) => setSlug(e.target.value)}
          required
        />
        <input
          className={`flex-1 ${input}`}
          placeholder="Page title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <button
          disabled={busy}
          className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50"
        >
          {busy ? "…" : "Create page"}
        </button>
      </form>
      {error && <p className="mb-3 text-sm text-red-500">{error}</p>}

      <div className="flex flex-col gap-3">
        {pages.length === 0 && (
          <p className="rounded-lg border px-4 py-8 text-center text-sm text-muted-foreground">
            No bio pages yet.
          </p>
        )}
        {pages.map((p) => (
          <div key={p.id} className="flex items-center justify-between rounded-lg border p-4">
            <div>
              <div className="font-medium">{p.title || p.slug}</div>
              <div className="font-mono text-xs text-muted-foreground">
                /bio/{p.slug} · {p.blocks.length} block{p.blocks.length === 1 ? "" : "s"}
              </div>
            </div>
            <div className="flex items-center gap-3 text-sm">
              <a href={`/bio/${p.slug}`} target="_blank" rel="noopener" className="text-muted-foreground hover:text-foreground">
                View ↗
              </a>
              <Link
                href={`/dashboard/bio/${p.id}`}
                className="rounded-md border px-3 py-1 hover:bg-accent hover:text-accent-foreground"
              >
                Edit
              </Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
