"use client";

import Link from "next/link";
import { useState } from "react";

type LinkResponse = {
  id: string;
  code: string;
  shortUrl: string;
  destinationUrl: string;
};

// Display-only base for the live alias preview; the server owns the real short URL.
const PREVIEW_BASE = "localhost:8081";

export function Shortener() {
  const [url, setUrl] = useState("");
  const [alias, setAlias] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [clickLimit, setClickLimit] = useState("");
  const [showAdvanced, setShowAdvanced] = useState(false);

  const [result, setResult] = useState<LinkResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);
    setCopied(false);
    try {
      const body: Record<string, unknown> = { destinationUrl: url.trim() };
      if (alias.trim()) body.alias = alias.trim();
      if (expiresAt) body.expiresAt = new Date(expiresAt).toISOString();
      if (clickLimit) body.clickLimit = Number(clickLimit);

      const res = await fetch("/api/v1/links", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => null);
        setError(b?.fieldErrors?.destinationUrl ?? b?.message ?? `Request failed (${res.status})`);
        return;
      }
      setResult((await res.json()) as LinkResponse);
    } catch {
      setError("Couldn't reach the API. Is it running on :8081?");
    } finally {
      setLoading(false);
    }
  }

  async function copy() {
    if (!result) return;
    await navigator.clipboard.writeText(result.shortUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  const inputCls =
    "w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring";

  return (
    <div className="w-full max-w-xl">
      <form onSubmit={onSubmit} className="flex flex-col gap-3">
        <div className="flex gap-2">
          <input
            type="url"
            required
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="Paste a long URL…"
            className={`flex-1 ${inputCls} px-4 py-2.5`}
          />
          <button
            type="submit"
            disabled={loading}
            className="rounded-md bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-50"
          >
            {loading ? "Shortening…" : "Shorten"}
          </button>
        </div>

        <button
          type="button"
          onClick={() => setShowAdvanced((v) => !v)}
          className="self-start text-xs text-muted-foreground hover:text-foreground"
        >
          {showAdvanced ? "▾ Options" : "▸ Options"}
        </button>

        {showAdvanced && (
          <div className="grid grid-cols-1 gap-3 rounded-lg border bg-card p-4 sm:grid-cols-2">
            <label className="flex flex-col gap-1 text-xs text-muted-foreground">
              Custom alias
              <input
                value={alias}
                onChange={(e) => setAlias(e.target.value)}
                placeholder="my-launch"
                pattern="[0-9A-Za-z]{1,64}"
                className={inputCls}
              />
              {alias.trim() && (
                <span className="font-mono text-[11px] text-blue-400">
                  {PREVIEW_BASE}/{alias.trim()}
                </span>
              )}
            </label>
            <label className="flex flex-col gap-1 text-xs text-muted-foreground">
              Click limit
              <input
                type="number"
                min={1}
                value={clickLimit}
                onChange={(e) => setClickLimit(e.target.value)}
                placeholder="unlimited"
                className={inputCls}
              />
            </label>
            <label className="flex flex-col gap-1 text-xs text-muted-foreground sm:col-span-2">
              Expires at
              <input
                type="datetime-local"
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
                className={inputCls}
              />
            </label>
          </div>
        )}
      </form>

      {error && <p className="mt-3 text-sm text-red-500">{error}</p>}

      {result && (
        <div className="mt-4 rounded-lg border bg-card p-4 text-card-foreground">
          <div className="flex items-center justify-between gap-3">
            <a
              href={result.shortUrl}
              target="_blank"
              rel="noreferrer"
              className="truncate font-mono text-sm text-blue-400 hover:underline"
            >
              {result.shortUrl}
            </a>
            <button
              onClick={copy}
              className="shrink-0 rounded-md border px-3 py-1.5 text-xs font-medium hover:bg-accent hover:text-accent-foreground"
            >
              {copied ? "Copied ✓" : "Copy"}
            </button>
          </div>
          <Link
            href={`/analytics/${result.id}`}
            className="mt-2 inline-block text-xs text-muted-foreground hover:text-foreground"
          >
            View analytics →
          </Link>
        </div>
      )}
    </div>
  );
}
