import { Shortener } from "@/components/shortener";
import { getApiStatus } from "@/lib/api";

export default async function Home() {
  const status = await getApiStatus();

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-8 p-8">
      <div className="text-center">
        <h1 className="text-5xl font-bold tracking-tight">🔗 Linkly</h1>
        <p className="mt-3 text-lg text-muted-foreground">
          Every link, everywhere, instantly.
        </p>
      </div>

      <Shortener />

      <p className="text-xs text-muted-foreground">
        {status.ok
          ? `API connected · ${status.seededLinks} links`
          : "API offline — start it in apps/api (./mvnw spring-boot:run)"}
        {" · "}Day 2: shorten → 302 redirect
      </p>
    </main>
  );
}
