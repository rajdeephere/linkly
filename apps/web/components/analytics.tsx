// Presentational analytics pieces. Single-series → single accent hue (no categorical palette),
// identity carried by text labels, recessive tracks, rounded data-ends, theme-aware surfaces.

export type Bucket = { label: string; count: number };
export type Point = { date: string; count: number };

export function StatTile({ label, value, hint }: { label: string; value: number | string; hint?: string }) {
  return (
    <div className="rounded-lg border bg-card p-4 text-card-foreground">
      <div className="text-xs uppercase tracking-wide text-muted-foreground">{label}</div>
      <div className="mt-1 text-3xl font-semibold tabular-nums">{value}</div>
      {hint && <div className="mt-0.5 text-xs text-muted-foreground">{hint}</div>}
    </div>
  );
}

/** Daily column chart (change-over-time). One series, so the title names it — no legend. */
export function ColumnChart({ title, points }: { title: string; points: Point[] }) {
  const max = Math.max(1, ...points.map((p) => p.count));
  return (
    <div className="rounded-lg border bg-card p-4 text-card-foreground">
      <div className="mb-3 text-sm font-medium">{title}</div>
      {points.length === 0 ? (
        <Empty />
      ) : (
        <div className="flex h-40 items-end gap-1">
          {points.map((p) => (
            <div
              key={p.date}
              title={`${p.date}: ${p.count}`}
              className="flex-1 rounded-t-[4px] bg-blue-500/80 transition-colors hover:bg-blue-400"
              style={{ height: `${Math.max(3, (p.count / max) * 100)}%` }}
            />
          ))}
        </div>
      )}
      {points.length > 0 && (
        <div className="mt-2 flex justify-between text-[11px] text-muted-foreground">
          <span>{points[0].date}</span>
          <span>{points[points.length - 1].date}</span>
        </div>
      )}
    </div>
  );
}

/** Horizontal bar list (magnitude by category). Label + proportional bar + count. */
export function BarList({ title, data }: { title: string; data: Bucket[] }) {
  const max = Math.max(1, ...data.map((d) => d.count));
  return (
    <div className="rounded-lg border bg-card p-4 text-card-foreground">
      <div className="mb-3 text-sm font-medium">{title}</div>
      {data.length === 0 ? (
        <Empty />
      ) : (
        <div className="flex flex-col gap-2">
          {data.map((d) => (
            <div key={d.label} className="flex items-center gap-2 text-sm">
              <span className="w-28 shrink-0 truncate text-muted-foreground" title={d.label}>
                {d.label}
              </span>
              <div className="h-2.5 flex-1 rounded-full bg-muted">
                <div
                  className="h-full rounded-full bg-blue-500/80"
                  style={{ width: `${(d.count / max) * 100}%` }}
                />
              </div>
              <span className="w-10 shrink-0 text-right tabular-nums">{d.count}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function Empty() {
  return <div className="py-6 text-center text-sm text-muted-foreground">No data yet</div>;
}
