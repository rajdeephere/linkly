"use client";

import { useState } from "react";

type LinkResponse = {
  code: string;
  shortUrl: string;
  destinationUrl: string;
};

export function Shortener() {
  const [url, setUrl] = useState("");
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
      const res = await fetch("/api/v1/links", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ destinationUrl: url.trim() }),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => null);
        const field = body?.fieldErrors?.destinationUrl;
        setError(field ?? body?.message ?? `Request failed (${res.status})`);
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

  return (
    <div className="w-full max-w-xl">
      <form onSubmit={onSubmit} className="flex gap-2">
        <input
          type="url"
          required
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="Paste a long URL…"
          className="flex-1 rounded-md border border-input bg-background px-4 py-2.5 text-sm outline-none ring-offset-background focus-visible:ring-2 focus-visible:ring-ring"
        />
        <button
          type="submit"
          disabled={loading}
          className="rounded-md bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-50"
        >
          {loading ? "Shortening…" : "Shorten"}
        </button>
      </form>

      {error && <p className="mt-3 text-sm text-red-500">{error}</p>}

      {result && (
        <div className="mt-4 flex items-center justify-between gap-3 rounded-lg border bg-card p-4 text-card-foreground">
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
      )}
    </div>
  );
}
