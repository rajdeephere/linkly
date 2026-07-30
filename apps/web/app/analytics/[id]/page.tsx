"use client";

import { BarList, ColumnChart, StatTile, type Bucket, type Point } from "@/components/analytics";
import Link from "next/link";
import { useEffect, useState } from "react";

type Analytics = {
  code: string;
  totalClicks: number;
  humanClicks: number;
  botClicks: number;
  days: number;
  timeseries: Point[];
  byDevice: Bucket[];
  byBrowser: Bucket[];
  byCountry: Bucket[];
  byReferrer: Bucket[];
};

const RANGES = [7, 30, 90];

export default function AnalyticsPage({ params }: { params: { id: string } }) {
  const [days, setDays] = useState(30);
  const [data, setData] = useState<Analytics | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    setError(null);
    fetch(`/api/v1/links/${params.id}/analytics?days=${days}`)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(`HTTP ${r.status}`))))
      .then((d: Analytics) => setData(d))
      .catch(() => setError("Couldn't load analytics for this link."))
      .finally(() => setLoading(false));
  }, [params.id, days]);

  return (
    <main className="mx-auto max-w-4xl p-6 md:p-10">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <Link href="/" className="text-sm text-muted-foreground hover:text-foreground">
            ← Linkly
          </Link>
          <h1 className="mt-1 text-2xl font-semibold">
            Analytics{data ? <span className="font-mono text-muted-foreground"> · /{data.code}</span> : ""}
          </h1>
        </div>
        <div className="flex gap-1 rounded-md border p-1">
          {RANGES.map((r) => (
            <button
              key={r}
              onClick={() => setDays(r)}
              className={`rounded px-3 py-1 text-xs ${
                days === r ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground"
              }`}
            >
              {r}d
            </button>
          ))}
        </div>
      </div>

      {error && <p className="text-sm text-red-500">{error}</p>}
      {loading && !data && <p className="text-sm text-muted-foreground">Loading…</p>}

      {data && (
        <div className="flex flex-col gap-4">
          <div className="grid grid-cols-3 gap-4">
            <StatTile label="Total clicks" value={data.totalClicks} hint={`last ${data.days} days`} />
            <StatTile label="Human" value={data.humanClicks} />
            <StatTile label="Bots" value={data.botClicks} />
          </div>

          <ColumnChart title="Clicks over time" points={data.timeseries} />

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <BarList title="Devices" data={data.byDevice} />
            <BarList title="Browsers" data={data.byBrowser} />
            <BarList title="Referrers" data={data.byReferrer} />
            <BarList title="Countries" data={data.byCountry} />
          </div>

          <p className="text-xs text-muted-foreground">
            Geo is edge-provided (Phase 2) — countries show “Unknown” until then. Everything else is live
            from the Day-6 pipeline.
          </p>
        </div>
      )}
    </main>
  );
}
