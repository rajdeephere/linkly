"use client";

import { clearSession, getSession } from "@/lib/auth";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

const TABS = [
  { href: "/dashboard", label: "Links" },
  { href: "/dashboard/domains", label: "Domains" },
  { href: "/dashboard/bio", label: "Bio pages" },
  { href: "/dashboard/api-keys", label: "API keys" },
];

/** Top nav for the authed dashboard. Also guards: no session → redirect to /login. */
export function Nav() {
  const router = useRouter();
  const pathname = usePathname();
  const [email, setEmail] = useState<string | null>(null);

  useEffect(() => {
    const s = getSession();
    if (!s) {
      router.replace("/login");
      return;
    }
    setEmail(s.email);
  }, [router]);

  function logout() {
    clearSession();
    router.replace("/login");
  }

  return (
    <header className="border-b">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <div className="flex items-center gap-6">
          <Link href="/dashboard" className="font-semibold">🔗 Linkly</Link>
          <nav className="flex gap-1 text-sm">
            {TABS.map((t) => {
              const active = pathname === t.href;
              return (
                <Link
                  key={t.href}
                  href={t.href}
                  className={`rounded-md px-3 py-1.5 ${active ? "bg-accent text-accent-foreground" : "text-muted-foreground hover:text-foreground"}`}
                >
                  {t.label}
                </Link>
              );
            })}
          </nav>
        </div>
        <div className="flex items-center gap-3 text-sm text-muted-foreground">
          {email && <span className="hidden sm:inline">{email}</span>}
          <button onClick={logout} className="rounded-md border px-2.5 py-1 hover:bg-accent hover:text-accent-foreground">
            Sign out
          </button>
        </div>
      </div>
    </header>
  );
}
