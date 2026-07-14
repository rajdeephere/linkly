import { getApiStatus } from "@/lib/api";

export default async function Home() {
  const status = await getApiStatus();

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-8 p-8">
      <div className="text-center">
        <h1 className="text-5xl font-bold tracking-tight">
          🔗 Linkly
        </h1>
        <p className="mt-3 text-lg text-muted-foreground">
          Every link, everywhere, instantly.
        </p>
      </div>

      <div className="w-full max-w-md rounded-lg border bg-card p-6 text-card-foreground shadow-sm">
        <h2 className="mb-3 text-sm font-medium text-muted-foreground">
          API connection
        </h2>
        {status.ok ? (
          <div className="space-y-1 text-sm">
            <p>
              <span className="text-muted-foreground">app:</span>{" "}
              <span className="font-mono">{status.app}</span>
            </p>
            <p>
              <span className="text-muted-foreground">status:</span>{" "}
              <span className="font-mono text-green-500">{status.status}</span>
            </p>
            <p>
              <span className="text-muted-foreground">seeded links:</span>{" "}
              <span className="font-mono">{status.seededLinks}</span>
            </p>
          </div>
        ) : (
          <p className="text-sm text-red-500">
            Can&apos;t reach the API. Start it with{" "}
            <code className="font-mono">./mvnw spring-boot:run</code> in{" "}
            <code className="font-mono">apps/api</code>.
          </p>
        )}
      </div>

      <p className="text-xs text-muted-foreground">
        Day 1 skeleton · next up: the shorten → redirect loop (Day 2)
      </p>
    </main>
  );
}
