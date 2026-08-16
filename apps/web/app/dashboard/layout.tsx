import { Nav } from "@/components/nav";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <Nav />
      <div className="mx-auto max-w-5xl px-4 py-8">{children}</div>
    </>
  );
}
