"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import Sidebar from "./Sidebar";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isLoggedIn, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    console.log("[Dashboard] isLoading:", isLoading, "isLoggedIn:", isLoggedIn);
    if (!isLoading && !isLoggedIn) {
      console.log("[Dashboard] redirecting to login");
      router.push("/auth/login");
    }
  }, [isLoggedIn, isLoading, router]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-page text-foreground">
        로딩 중...
      </div>
    );
  }

  if (!isLoggedIn) return null;

  return (
    <div className="flex min-h-screen bg-page">
      <Sidebar />
      <main className="flex-1 overflow-auto text-foreground">
        {children}
      </main>
    </div>
  );
}
