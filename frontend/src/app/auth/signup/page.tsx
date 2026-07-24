"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import api from "@/lib/api";

export default function SignupPage() {
  const router = useRouter();

  const [form, setForm] = useState({
    name: "",
    username: "",
    email: "",
    password: "",
    passwordConfirm: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");

    if (form.password.length < 8) {
      setError("비밀번호는 8자 이상이어야 합니다.");
      return;
    }
    if (form.password !== form.passwordConfirm) {
      setError("비밀번호가 일치하지 않습니다.");
      return;
    }

    setLoading(true);
    try {
      await api.post("/api/auth/signup", {
        name: form.name,
        username: form.username,
        email: form.email,
        password: form.password,
      });
      router.push("/auth/login");
    } catch {
      setError("회원가입에 실패했습니다. 이미 사용 중인 아이디나 이메일일 수 있습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-page flex items-center justify-center">
      <div className="bg-canvas rounded-2xl border border-hairline w-full max-w-sm px-8 py-10 text-foreground">
        <h1 className="text-2xl font-bold text-center mb-1 text-foreground">MoneyFlowOps</h1>
        <p className="text-xs text-muted text-center mb-8">
          새 계정을 만들어 관리를 시작하세요
        </p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {[
            { label: "이름", name: "name", type: "text", placeholder: "이름을 입력하세요" },
            { label: "아이디", name: "username", type: "text", placeholder: "아이디를 입력하세요" },
            { label: "이메일", name: "email", type: "email", placeholder: "이메일을 입력하세요" },
            { label: "비밀번호 (8자 이상)", name: "password", type: "password", placeholder: "8자 이상 입력하세요" },
            { label: "비밀번호 확인", name: "passwordConfirm", type: "password", placeholder: "비밀번호를 한 번 더 입력하세요" },
          ].map((field) => (
            <div key={field.name} className="flex flex-col gap-1">
              <label className="text-sm font-medium text-muted-secondary">{field.label}</label>
              <input
                type={field.type}
                name={field.name}
                placeholder={field.placeholder}
                value={form[field.name as keyof typeof form]}
                onChange={handleChange}
                required
                className="bg-canvas border border-hairline rounded-lg px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-accent"
              />
            </div>
          ))}

          {error && (
            <p className="text-xs text-red-400 bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="btn-pill btn-pill-solid font-medium transition-colors"
          >
            {loading ? "가입 중..." : "회원가입"}
          </button>
        </form>

        <p className="text-xs text-center text-muted mt-6">
          이미 계정이 있으신가요?{" "}
          <Link href="/auth/login" className="text-accent hover:underline">
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}
