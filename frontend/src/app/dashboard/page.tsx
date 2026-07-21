"use client";

import { useEffect, useState } from "react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { formatCurrency, formatYearMonth, toYearMonth, formatDate } from "@/lib/format";
import { DashboardSummary, CategoryExpense, Transaction } from "@/types";
import api from "@/lib/api";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from "recharts";
import { CATEGORY_COLORS } from "@/lib/constants";

export default function DashboardPage() {
  const [yearMonth, setYearMonth] = useState(toYearMonth());
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [categoryExpenses, setCategoryExpenses] = useState<CategoryExpense[]>([]);
  const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([]);

  const yearMonthStr = String(yearMonth);

  useEffect(() => {
    api
      .get<{ data: DashboardSummary }>(`/api/dashboard/summary?yearMonth=${yearMonthStr}`)
      .then((res) => setSummary(res.data.data))
      .catch(() => {});

    api
      .get<{ data: CategoryExpense[] }>(`/api/dashboard/categories?yearMonth=${yearMonthStr}&type=0`)
      .then((res) => setCategoryExpenses(res.data.data))
      .catch(() => {});

    const period = `${yearMonthStr.slice(0, 4)}-${yearMonthStr.slice(4)}`;
    api
      .get<{ data: { content: Transaction[] } }>(`/api/transactions?period=${period}&size=5&sort=desc`)
      .then((res) => setRecentTransactions(res.data.data.content))
      .catch(() => {});
  }, [yearMonthStr]);

  const budgetRate = summary?.budgetUsageRate ?? null;

  return (
    <DashboardLayout>
      <div className="p-6 text-gray-900 dark:text-white">
        {/* 헤더 */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-xl font-bold">이번 달 현금 흐름</h1>
            <p className="text-sm text-gray-400 mt-0.5">
              {formatYearMonth(yearMonth)} 기준
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => {
                const d = new Date(
                  Math.floor(yearMonth / 100),
                  (yearMonth % 100) - 2
                );
                setYearMonth(toYearMonth(d));
              }}
              className="px-3 py-1.5 bg-gray-200 dark:bg-gray-800 hover:bg-gray-300 dark:hover:bg-gray-700 rounded-lg text-sm text-gray-800 dark:text-white"
            >
              이전 달
            </button>
            <button
              onClick={() => {
                const d = new Date(
                  Math.floor(yearMonth / 100),
                  yearMonth % 100
                );
                setYearMonth(toYearMonth(d));
              }}
              className="px-3 py-1.5 bg-gray-200 dark:bg-gray-800 hover:bg-gray-300 dark:hover:bg-gray-700 rounded-lg text-sm text-gray-800 dark:text-white"
            >
              다음 달
            </button>
          </div>
        </div>

        {/* 지표 카드 4개 */}
        <div className="grid grid-cols-4 gap-4 mb-6">
          <MetricCard
            label="총 잔액"
            value={formatCurrency(summary?.balance ?? 0)}
            color="text-gray-900 dark:text-white"
          />
          <MetricCard
            label="총 수입"
            value={formatCurrency(summary?.totalIncome ?? 0)}
            color="text-green-400"
            badge="수입"
            badgeColor="bg-green-500/20 text-green-400"
          />
          <MetricCard
            label="총 지출"
            value={formatCurrency(summary?.totalExpense ?? 0)}
            color="text-red-400"
            badge="지출"
            badgeColor="bg-red-500/20 text-red-400"
          />
          <MetricCard
            label="월 예산"
            value={summary?.budgetAmount ? formatCurrency(summary.budgetAmount) : "미설정"}
            color="text-blue-400"
            badge={
              summary?.isOverBudget
                ? "초과"
                : budgetRate !== null
                ? `${budgetRate.toFixed(0)}%`
                : undefined
            }
            badgeColor={
              summary?.isOverBudget
                ? "bg-red-500/20 text-red-400"
                : "bg-blue-500/20 text-blue-400"
            }
          />
        </div>

        {/* 예산 진행바 */}
        {budgetRate !== null && (
          <div className="mb-6 bg-white dark:bg-gray-800 rounded-xl p-4">
            <div className="flex justify-between text-sm mb-2">
              <span className="text-gray-500 dark:text-gray-400">예산 사용률</span>
              <span className={summary?.isOverBudget ? "text-red-400" : "text-blue-400"}>
                {budgetRate.toFixed(1)}%
                {summary?.isOverBudget && " ⚠️ 예산 초과"}
              </span>
            </div>
            <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
              <div
                className={`h-2 rounded-full transition-all ${
                  summary?.isOverBudget ? "bg-red-500" : "bg-blue-500"
                }`}
                style={{ width: `${Math.min(budgetRate, 100)}%` }}
              />
            </div>
          </div>
        )}

        <div className="grid grid-cols-3 gap-4">
          {/* 카테고리 차트 */}
          <div className="col-span-2 bg-white dark:bg-gray-800 rounded-xl p-4">
            <h2 className="text-sm font-semibold mb-4">카테고리별 지출</h2>
            {categoryExpenses.length > 0 ? (
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={categoryExpenses} layout="vertical">
                  <XAxis type="number" hide />
                  <YAxis
                    type="category"
                    dataKey="categoryName"
                    width={80}
                    tick={{ fill: "#9CA3AF", fontSize: 12 }}
                  />
                  <Tooltip
                    formatter={(v) => formatCurrency(Number(v))}
                    contentStyle={{
                      background: "#1F2937",
                      border: "none",
                      borderRadius: 8,
                      color: "#fff",
                    }}
                  />
                  <Bar dataKey="amount" radius={[0, 4, 4, 0]}>
                    {categoryExpenses.map((entry) => (
                      <Cell
                        key={entry.categoryCode}
                        fill={CATEGORY_COLORS[entry.categoryCode] ?? "#6B7280"}
                      />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-gray-500 text-sm text-center py-12">
                지출 내역이 없습니다.
              </p>
            )}
          </div>

          {/* 최근 거래 */}
          <div className="bg-white dark:bg-gray-800 rounded-xl p-4">
            <h2 className="text-sm font-semibold mb-4">최근 거래</h2>
            <div className="flex flex-col gap-3">
              {recentTransactions.length > 0 ? (
                recentTransactions.map((tx) => (
                  <div key={tx.id} className="flex justify-between items-center">
                    <div>
                      <p className="text-xs text-gray-600 dark:text-gray-300">{tx.categoryName}</p>
                      <p className="text-xs text-gray-400 dark:text-gray-500">{formatDate(tx.transactionAt)} · {tx.memo ?? "-"}</p>
                    </div>
                    <span
                      className={`text-sm font-medium ${
                        tx.type === "INCOME" ? "text-green-400" : "text-red-400"
                      }`}
                    >
                      {tx.type === "INCOME" ? "+" : "-"}
                      {formatCurrency(tx.amount)}
                    </span>
                  </div>
                ))
              ) : (
                <p className="text-gray-500 text-sm">거래 내역이 없습니다.</p>
              )}
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}

function MetricCard({
  label,
  value,
  color,
  badge,
  badgeColor,
}: {
  label: string;
  value: string;
  color: string;
  badge?: string;
  badgeColor?: string;
}) {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl p-4">
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs text-gray-400">{label}</span>
        {badge && (
          <span className={`text-xs px-2 py-0.5 rounded-full ${badgeColor}`}>
            {badge}
          </span>
        )}
      </div>
      <p className={`text-xl font-bold ${color}`}>{value}</p>
    </div>
  );
}
