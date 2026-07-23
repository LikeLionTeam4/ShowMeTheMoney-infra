"use client";

import { useEffect, useState, useCallback } from "react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Budget } from "@/types";
import { formatCurrency, formatYearMonth, toYearMonth } from "@/lib/format";
import api from "@/lib/api";

export default function BudgetPage() {
  const [yearMonth, setYearMonth] = useState(toYearMonth());
  const [budget, setBudget] = useState<Budget | null>(null);
  const [amount, setAmount] = useState("");
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const fetchBudget = useCallback(() => {
    api
      .get<{ data: Budget }>(`/api/budgets?yearMonth=${yearMonth}`)
      .then((res) => {
        setBudget(res.data.data);
        setAmount(String(res.data.data.amount));
      })
      .catch(() => {
        setBudget(null);
        setAmount("");
      });
  }, [yearMonth]);

  useEffect(() => {
    fetchBudget();
  }, [fetchBudget]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      if (budget) {
        await api.put(`/api/budgets/${budget.id}`, {
          amount: Number(amount),
        });
      } else {
        await api.post("/api/budgets", {
          yearMonth: String(yearMonth),
          amount: Number(amount),
        });
      }
      fetchBudget();
      setIsEditing(false);
    } catch {
      setError("예산 저장에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <DashboardLayout>
      <div className="p-6 text-gray-900 dark:text-white">
        <div className="mb-6">
          <h1 className="text-xl font-bold">월별 예산 설정</h1>
          <p className="text-sm text-gray-400 mt-0.5">
            월별 지출 예산을 설정하고 관리하세요
          </p>
        </div>

        {/* 월 선택 */}
        <div className="flex items-center gap-3 mb-6">
          <button
            onClick={() => {
              const d = new Date(Math.floor(yearMonth / 100), (yearMonth % 100) - 2);
              setYearMonth(toYearMonth(d));
              setIsEditing(false);
            }}
            className="px-3 py-1.5 bg-gray-200 dark:bg-gray-800 hover:bg-gray-300 dark:hover:bg-gray-700 rounded-lg text-sm text-gray-800 dark:text-white"
          >
            이전 달
          </button>
          <span className="text-sm font-medium">{formatYearMonth(yearMonth)}</span>
          <button
            onClick={() => {
              const d = new Date(Math.floor(yearMonth / 100), yearMonth % 100);
              setYearMonth(toYearMonth(d));
              setIsEditing(false);
            }}
            className="px-3 py-1.5 bg-gray-200 dark:bg-gray-800 hover:bg-gray-300 dark:hover:bg-gray-700 rounded-lg text-sm text-gray-800 dark:text-white"
          >
            다음 달
          </button>
        </div>

        <div className="max-w-md">
          {/* 현재 예산 표시 */}
          {budget && !isEditing ? (
            <div className="bg-white dark:bg-gray-800 rounded-xl p-6 mb-4">
              <p className="text-xs text-gray-400 mb-1">{formatYearMonth(yearMonth)} 예산</p>
              <p className="text-3xl font-bold text-blue-400 mb-4">
                {formatCurrency(budget.amount)}
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setIsEditing(true)}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg text-sm"
                >
                  수정
                </button>
              </div>
            </div>
          ) : (
            <div className="bg-white dark:bg-gray-800 rounded-xl p-6">
              <p className="text-sm font-medium mb-4">
                {budget ? "예산 수정" : `${formatYearMonth(yearMonth)} 예산 등록`}
              </p>
              <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                <div className="flex flex-col gap-1">
                  <label className="text-xs text-gray-400">예산 금액 *</label>
                  <div className="relative">
                    <input
                      type="number"
                      placeholder="예산 금액을 입력하세요"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      required
                      min={1}
                      className="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 pr-8 text-white"
                    />
                    <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-gray-400">원</span>
                  </div>
                </div>

                {error && (
                  <p className="text-xs text-red-400 bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2">
                    {error}
                  </p>
                )}

                <div className="flex gap-2">
                  {isEditing && (
                    <button
                      type="button"
                      onClick={() => setIsEditing(false)}
                      className="flex-1 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg text-sm"
                    >
                      취소
                    </button>
                  )}
                  <button
                    type="submit"
                    disabled={loading}
                    className="flex-1 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg text-sm font-medium disabled:opacity-50"
                  >
                    {loading ? "저장 중..." : budget ? "수정 완료" : "예산 등록"}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
