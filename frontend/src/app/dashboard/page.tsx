"use client";

import { useEffect, useState } from "react";
import { useTheme } from "next-themes";
import DashboardLayout from "@/components/layout/DashboardLayout";
import DailyBalanceChart from "@/components/dashboard/DailyBalanceChart";
import { formatCurrency, formatYearMonth, toYearMonth, formatDate } from "@/lib/format";
import { DashboardSummary, CategoryExpense, Transaction, DashboardDaily } from "@/types";
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
import { CATEGORY_COLORS, CATEGORY_COLORS_DARK } from "@/lib/constants";

export default function DashboardPage() {
  const { resolvedTheme } = useTheme();
  const isDark = resolvedTheme === "dark";
  const categoryColors = isDark ? CATEGORY_COLORS_DARK : CATEGORY_COLORS;
  const [yearMonth, setYearMonth] = useState(toYearMonth());
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [categoryExpenses, setCategoryExpenses] = useState<CategoryExpense[]>([]);
  const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([]);
  const [dailyBalances, setDailyBalances] = useState<DashboardDaily | null>(null);

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

    api
      .get<{ data: DashboardDaily }>(`/api/dashboard/daily?yearMonth=${yearMonthStr}`)
      .then((res) => setDailyBalances(res.data.data))
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
      <div className="p-6 text-foreground">
        {/* 헤더 */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-xl font-bold">이번 달 현금 흐름</h1>
            <p className="text-sm text-muted mt-0.5">
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
              className="btn-pill btn-pill-outline"
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
              className="btn-pill btn-pill-outline"
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
            color="text-foreground"
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
            color="text-accent"
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
                : "bg-accent/15 text-accent"
            }
          />
        </div>

        {/* 예산 진행바 */}
        {budgetRate !== null && (
          <div className="mb-6 card p-4">
            <div className="flex justify-between text-sm mb-2">
              <span className="text-muted">예산 사용률</span>
              <span className={summary?.isOverBudget ? "text-red-400" : "text-accent"}>
                {budgetRate.toFixed(1)}%
                {summary?.isOverBudget && " ⚠️ 예산 초과"}
              </span>
            </div>
            <div className="w-full bg-hairline rounded-full h-2">
              <div
                className={`h-2 rounded-full transition-all ${
                  summary?.isOverBudget ? "bg-red-500" : "bg-accent"
                }`}
                style={{ width: `${Math.min(budgetRate, 100)}%` }}
              />
            </div>
          </div>
        )}

        {/* 일별 누적 지출 추이 */}
        {dailyBalances && dailyBalances.days.length > 0 && (
          <div className="mb-6 card p-4">
            <h2 className="text-sm font-semibold mb-4">일별 누적 지출</h2>
            <DailyBalanceChart daily={dailyBalances} />
          </div>
        )}

        <div className="grid grid-cols-3 gap-4">
          {/* 카테고리 차트 */}
          <div className="col-span-2 card p-4">
            <h2 className="text-sm font-semibold mb-4">카테고리별 지출</h2>
            {categoryExpenses.length > 0 ? (
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={categoryExpenses} layout="vertical">
                  <XAxis type="number" hide />
                  <YAxis
                    type="category"
                    dataKey="categoryName"
                    width={80}
                    tick={{ fill: isDark ? "#9c9c9c" : "#a3a3a3", fontSize: 12 }}
                  />
                  <Tooltip
                    formatter={(v) => formatCurrency(Number(v))}
                    contentStyle={{
                      background: "var(--color-canvas)",
                      border: "1px solid var(--color-hairline)",
                      borderRadius: 8,
                      boxShadow: "none",
                    }}
                    labelStyle={{ color: "var(--color-foreground)" }}
                    itemStyle={{ color: "var(--color-foreground)" }}
                  />
                  <Bar dataKey="amount" name="금액" radius={[0, 4, 4, 0]}>
                    {categoryExpenses.map((entry) => (
                      <Cell
                        key={entry.categoryCode}
                        fill={categoryColors[entry.categoryCode] ?? (isDark ? "#9c9c9c" : "#a3a3a3")}
                      />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-muted text-sm text-center py-12">
                지출 내역이 없습니다.
              </p>
            )}
          </div>

          {/* 최근 거래 */}
          <div className="card p-4">
            <h2 className="text-sm font-semibold mb-4">최근 거래</h2>
            <div className="flex flex-col gap-3">
              {recentTransactions.length > 0 ? (
                recentTransactions.map((tx) => (
                  <div key={tx.id} className="flex justify-between items-center">
                    <div>
                      <p className="text-xs text-muted-secondary">{tx.categoryName}</p>
                      <p className="text-xs text-muted">{formatDate(tx.transactionAt)} · {tx.memo ?? "-"}</p>
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
                <p className="text-muted text-sm">거래 내역이 없습니다.</p>
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
    <div className="card p-4">
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs text-muted">{label}</span>
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
