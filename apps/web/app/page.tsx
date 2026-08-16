"use client";

import { getSession } from "@/lib/auth";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

export default function Home() {
  const router = useRouter();
  useEffect(() => {
    router.replace(getSession() ? "/dashboard" : "/login");
  }, [router]);
  return null;
}
