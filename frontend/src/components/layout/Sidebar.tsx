"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useTheme } from "next-themes";
import { useAuth } from "@/context/AuthContext";

const NAV_ITEMS = [
  { label: "대시보드", href: "/dashboard", icon: "▦" },
  { label: "거래 내역", href: "/transactions", icon: "≡" },
  { label: "고정 수입/지출", href: "/recurring", icon: "↻" },
  { label: "예산 설정", href: "/budget", icon: "◎" },
];

export default function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout } = useAuth();
  const { theme, setTheme } = useTheme();

  async function handleLogout() {
    await logout();
    router.push("/auth/login");
  }

  return (
    <aside className="w-56 min-h-screen bg-gray-900 dark:bg-gray-950 flex flex-col text-white border-r border-gray-200 dark:border-white/5">
      <div className="px-6 py-5 border-b border-white/10">
        <span className="font-bold text-base tracking-tight">MoneyFlowOps</span>
      </div>

      <nav className="flex-1 px-3 py-4 flex flex-col gap-1">
        {NAV_ITEMS.map((item) => {
          const active = pathname.startsWith(item.href);
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors ${
                active
                  ? "bg-blue-600 text-white"
                  : "text-gray-400 hover:bg-white/10 hover:text-white"
              }`}
            >
              <span className="text-base">{item.icon}</span>
              {item.label}
            </Link>
          );
        })}
      </nav>

      <div className="px-4 py-4 border-t border-white/10 flex flex-col gap-2">
        <button
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          className="flex items-center gap-2 text-xs text-gray-400 hover:text-white transition-colors"
        >
          <span>{theme === "dark" ? "☀️" : "🌙"}</span>
          {theme === "dark" ? "라이트 모드" : "다크 모드"}
        </button>

        <p className="text-xs text-gray-400">{user?.name}</p>
        <button
          onClick={handleLogout}
          className="text-xs text-gray-500 hover:text-white transition-colors text-left"
        >
          로그아웃
        </button>
      </div>
    </aside>
  );
}
