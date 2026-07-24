"use client";

import { useTheme } from "next-themes";
import {
  ComposedChart,
  Area,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ReferenceLine,
  ResponsiveContainer,
  Label,
} from "recharts";
import { DashboardDaily } from "@/types";
import { formatCurrency } from "@/lib/format";

const ACCENT = { light: "#ff6000", dark: "#ff7a33" };
const OVER_BUDGET = { light: "#e34948", dark: "#e66767" };
const INK = { light: "#a3a3a3", dark: "#9c9c9c" };

function compactCurrency(amount: number): string {
  if (Math.abs(amount) >= 10000) {
    return `${Math.round(amount / 10000)}만`;
  }
  return formatCurrency(amount);
}

export default function DailyBalanceChart({ daily }: { daily: DashboardDaily }) {
  const { resolvedTheme } = useTheme();
  const isDark = resolvedTheme === "dark";
  const ink = isDark ? INK.dark : INK.light;

  const lastDay = daily.days[daily.days.length - 1];
  const isOverBudget = daily.budgetAmount != null && (lastDay?.cumulativeExpense ?? 0) > daily.budgetAmount;
  const lineColor = isOverBudget ? (isDark ? OVER_BUDGET.dark : OVER_BUDGET.light) : (isDark ? ACCENT.dark : ACCENT.light);

  const data = daily.days.map((d) => ({ ...d, day: Number(d.date.slice(-2)) }));

  return (
    <ResponsiveContainer width="100%" height={220}>
      <ComposedChart data={data} margin={{ top: 8, right: 48, left: 0, bottom: 0 }}>
        <defs>
          <linearGradient id="dailyExpenseFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={lineColor} stopOpacity={0.1} />
            <stop offset="100%" stopColor={lineColor} stopOpacity={0} />
          </linearGradient>
        </defs>
        <XAxis
          dataKey="day"
          tickLine={false}
          axisLine={{ stroke: "var(--color-hairline)" }}
          tick={{ fill: ink, fontSize: 11 }}
          interval={Math.ceil(data.length / 10)}
        />
        <YAxis
          tickLine={false}
          axisLine={false}
          tick={{ fill: ink, fontSize: 11 }}
          tickFormatter={compactCurrency}
          width={48}
        />
        <Tooltip
          formatter={(value) => [formatCurrency(Number(Array.isArray(value) ? value[0] : value)), "누적 지출"]}
          labelFormatter={(day) => `${daily.yearMonth} ${day}일`}
          contentStyle={{
            background: "var(--color-canvas)",
            border: "1px solid var(--color-hairline)",
            borderRadius: 8,
            boxShadow: "none",
          }}
          labelStyle={{ color: "var(--color-foreground)" }}
          itemStyle={{ color: "var(--color-foreground)" }}
        />
        {daily.budgetAmount != null && (
          <ReferenceLine
            y={daily.budgetAmount}
            stroke="var(--color-muted-secondary)"
            strokeDasharray="4 4"
            ifOverflow="extendDomain"
          >
            <Label
              value={`예산 ${compactCurrency(daily.budgetAmount)}`}
              position="insideTopRight"
              fill="var(--color-muted-secondary)"
              fontSize={11}
            />
          </ReferenceLine>
        )}
        <Area
          type="monotone"
          dataKey="cumulativeExpense"
          stroke="none"
          fill="url(#dailyExpenseFill)"
          isAnimationActive={false}
        />
        <Line
          type="monotone"
          dataKey="cumulativeExpense"
          stroke={lineColor}
          strokeWidth={2}
          dot={false}
          activeDot={{ r: 4, stroke: "var(--color-canvas)", strokeWidth: 2 }}
          isAnimationActive={false}
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
}
