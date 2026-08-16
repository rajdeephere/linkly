// Public link-in-bio page (server-rendered for SEO; no auth). Fetches the page by slug from the API.

type Block = { id: string; label: string; url: string; position: number };
type Bio = {
  slug: string;
  title?: string;
  avatarUrl?: string;
  bio?: string;
  theme: string;
  blocks: Block[];
};

const API = process.env.API_URL ?? "http://localhost:8081";

const THEMES: Record<string, { wrap: string; card: string; sub: string }> = {
  default: { wrap: "bg-neutral-50 text-neutral-900", card: "bg-white border-neutral-200 hover:border-neutral-400", sub: "text-neutral-500" },
  dark: { wrap: "bg-neutral-950 text-neutral-100", card: "bg-neutral-900 border-neutral-800 hover:border-neutral-600", sub: "text-neutral-400" },
  sunset: { wrap: "bg-gradient-to-b from-orange-500 to-pink-600 text-white", card: "bg-white/15 border-white/30 hover:bg-white/25 backdrop-blur", sub: "text-white/80" },
};

async function getBio(slug: string): Promise<Bio | null> {
  try {
    const res = await fetch(`${API}/bio/${encodeURIComponent(slug)}`, { cache: "no-store" });
    return res.ok ? ((await res.json()) as Bio) : null;
  } catch {
    return null;
  }
}

export default async function BioPage({ params }: { params: { slug: string } }) {
  const bio = await getBio(params.slug);
  const t = THEMES[bio?.theme ?? "default"] ?? THEMES.default;

  if (!bio) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-neutral-950 text-neutral-400">
        <p className="text-sm">This page doesn’t exist.</p>
      </main>
    );
  }

  return (
    <main className={`flex min-h-screen flex-col items-center px-4 py-16 ${t.wrap}`}>
      <div className="w-full max-w-md text-center">
        {bio.avatarUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={bio.avatarUrl} alt="" className="mx-auto h-20 w-20 rounded-full object-cover" />
        )}
        <h1 className="mt-4 text-2xl font-semibold">{bio.title ?? `@${bio.slug}`}</h1>
        {bio.bio && <p className={`mt-2 text-sm ${t.sub}`}>{bio.bio}</p>}

        <div className="mt-8 flex flex-col gap-3">
          {bio.blocks.length === 0 && <p className={`text-sm ${t.sub}`}>No links yet.</p>}
          {bio.blocks.map((b) => (
            <a
              key={b.id}
              href={b.url}
              target="_blank"
              rel="noreferrer"
              className={`rounded-xl border px-4 py-3 text-sm font-medium transition-colors ${t.card}`}
            >
              {b.label}
            </a>
          ))}
        </div>

        <p className={`mt-10 text-xs ${t.sub}`}>🔗 Linkly</p>
      </div>
    </main>
  );
}
