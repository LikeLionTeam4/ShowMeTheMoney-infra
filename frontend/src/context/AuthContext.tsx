"use client";

import { createContext, useContext, useEffect, useState } from "react";
import api from "@/lib/api";
import { User } from "@/types";

interface AuthContextValue {
  user: User | null;
  isLoggedIn: boolean;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function initAuth() {
      const token = localStorage.getItem("accessToken");
      if (!token) {
        setIsLoading(false);
        return;
      }
      try {
        const res = await api.get<{ data: User }>("/api/users/me");
        setUser(res.data.data);
      } catch {
        localStorage.removeItem("accessToken");
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    }
    initAuth();
  }, []);

  async function login(username: string, password: string) {
    console.log("[Auth] login start:", username);
    const res = await api.post<{ data: { accessToken: string } }>("/api/auth/login", {
      username,
      password,
    });
    console.log("[Auth] login response:", res.data);
    const { accessToken } = res.data.data;
    localStorage.setItem("accessToken", accessToken);
    console.log("[Auth] token saved");

    const meRes = await api.get<{ data: User }>("/api/users/me");
    console.log("[Auth] me response:", meRes.data);
    setUser(meRes.data.data);
    console.log("[Auth] user set:", meRes.data.data);
  }

  async function logout() {
    await api.post("/api/auth/logout").catch(() => {});
    localStorage.removeItem("accessToken");
    setUser(null);
  }

  return (
    <AuthContext.Provider
      value={{ user, isLoggedIn: !!user, isLoading, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
