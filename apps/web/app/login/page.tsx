"use client";

import { saveSession, type Session } from "@/lib/auth";
import { useRouter } from "next/navigation";
import { useState } from "react";

export default function LoginPage() {
  const router = useRouter();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const body =
        mode === "register" ? { email, name, password } : { email, password };
      const res = await fetch(`/api/v1/auth/${mode}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => null);
        setError(b?.message ?? (res.status === 401 ? "Invalid email or password" : `Failed (${res.status})`));
        return;
      }
      saveSession((await res.json()) as Session);
      router.push("/dashboard");
    } catch {
      setError("Couldn't reach the API.");
    } finally {
      setLoading(false);
    }
  }

  const input =
    "w-full rounded-md border border-input bg-background px-3 py-2.5 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring";

  return (
    <main className="flex min-h-screen items-center justify-center p-6">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold tracking-tight">🔗 Linkly</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {mode === "login" ? "Sign in to your workspace" : "Create your workspace"}
          </p>
        </div>

        <form onSubmit={submit} className="flex flex-col gap-3">
          {mode === "register" && (
            <input className={input} placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} />
          )}
          <input className={input} type="email" required placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
          <input className={input} type="password" required placeholder="Password (8+ chars)" value={password} onChange={(e) => setPassword(e.target.value)} />
          <button
            type="submit"
            disabled={loading}
            className="mt-1 rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50"
          >
            {loading ? "…" : mode === "login" ? "Sign in" : "Create account"}
          </button>
        </form>

        {error && <p className="mt-3 text-center text-sm text-red-500">{error}</p>}

        <p className="mt-6 text-center text-sm text-muted-foreground">
          {mode === "login" ? "No account?" : "Have an account?"}{" "}
          <button
            onClick={() => { setMode(mode === "login" ? "register" : "login"); setError(null); }}
            className="text-foreground underline"
          >
            {mode === "login" ? "Create one" : "Sign in"}
          </button>
        </p>
      </div>
    </main>
  );
}
