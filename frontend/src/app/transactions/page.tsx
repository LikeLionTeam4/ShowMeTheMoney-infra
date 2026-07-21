"use client";

import { useEffect, useState, useCallback } from "react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import TransactionModal from "@/components/ui/TransactionModal";
import { Transaction, TransactionType, TransactionPageResponse } from "@/types";
import { formatCurrency, formatDate, toYearMonth, formatYearMonth } from "@/lib/format";
import api from "@/lib/api";

const PAGE_SIZE = 10;

export default function TransactionsPage() {
  const [yearMonth, setYearMonth] = useState(toYearMonth());
  const [typeFilter, setTypeFilter] = useState<TransactionType | "ALL">("ALL");
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalIncome, setTotalIncome] = useState(0);
  const [totalExpense, setTotalExpense] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [sort, setSort] = useState<"asc" | "desc">("desc");
  const [showModal, setShowModal] = useState(false);

  const fetchTransactions = useCallback(() => {
    const ym = String(yearMonth);
    const period = `${ym.slice(0, 4)}-${ym.slice(4)}`;
    const params = new URLSearchParams({
      period,
      page: String(page),
      size: String(PAGE_SIZE),
      sort,
    });
    if (typeFilter !== "ALL") params.set("type", typeFilter === "INCOME" ? "1" : "0");

    api
      .get<{ data: TransactionPageResponse }>(`/api/transactions?${params}`)
      .then((res) => {
        const d = res.data.data;
        setTransactions(d.content);
        setTotalElements(d.totalElements);
        setTotalPages(d.totalPages);
        setTotalIncome(d.totalIncome);
        setTotalExpense(d.totalExpense);
      })
      .catch(() => {});
  }, [yearMonth, page, sort, typeFilter]);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  async function handleDelete(id: number) {
    if (!confirm("삭제하시겠습니까?")) return;
    await api.delete(`/api/transactions/${id}`);
    fetchTransactions();
  }

  return (
    <DashboardLayout>
      <div className="p-6 text-gray-900 dark:text-white">
        {/* 헤더 */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-xl font-bold">거래 내역 관리</h1>
            <p className="text-sm text-gray-400 mt-0.5">
              수입과 지출 내역을 확인하고 관리하세요
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setSort((s) => (s === "desc" ? "asc" : "desc"))}
              className="px-3 py-1.5 bg-gray-200 dark:bg-gray-800 hover:bg-gray-300 dark:hover:bg-gray-700 rounded-lg text-sm text-gray-800 dark:text-white"
            >
              {sort === "desc" ? "최신순 ↓" : "오래된순 ↑"}
            </button>
            <button
              onClick={() => setShowModal(true)}
              className="px-4 py-1.5 bg-blue-600 hover:bg-blue-700 rounded-lg text-sm font-medium"
            >
              + 거래 등록
            </button>
          </div>
        </div>

        {/* 월 선택 + 타입 필터 */}
        <div className="flex items-center gap-3 mb-5">
          <button
            onClick={() => {
              const d = new Date(Math.floor(yearMonth / 100), (yearMonth % 100) - 2);
              setYearMonth(toYearMonth(d));
              setPage(0);
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
              setPage(0);
            }}
            className="px-3 py-1.5 bg-gray-200 dark:bg-gray-800 hover:bg-gray-300 dark:hover:bg-gray-700 rounded-lg text-sm text-gray-800 dark:text-white"
          >
            다음 달
          </button>

          <div className="ml-4 flex gap-1">
            {(["ALL", "INCOME", "EXPENSE"] as const).map((t) => (
              <button
                key={t}
                onClick={() => { setTypeFilter(t); setPage(0); }}
                className={`px-3 py-1 rounded-lg text-xs font-medium transition-colors ${
                  typeFilter === t
                    ? "bg-blue-600 text-white"
                    : "bg-gray-200 dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-gray-300 dark:hover:bg-gray-700"
                }`}
              >
                {t === "ALL" ? "전체" : t === "INCOME" ? "수입" : "지출"}
              </button>
            ))}
          </div>
        </div>

        {/* 요약 카드 */}
        <div className="grid grid-cols-3 gap-4 mb-5">
          <div className="bg-white dark:bg-gray-800 rounded-xl p-4">
            <p className="text-xs text-gray-400 mb-1">총 건수</p>
            <p className="text-xl font-bold">{totalElements}건</p>
          </div>
          <div className="bg-white dark:bg-gray-800 rounded-xl p-4">
            <p className="text-xs text-gray-400 mb-1">총 수입</p>
            <p className="text-xl font-bold text-green-400">{formatCurrency(totalIncome)}</p>
          </div>
          <div className="bg-white dark:bg-gray-800 rounded-xl p-4">
            <p className="text-xs text-gray-400 mb-1">총 지출</p>
            <p className="text-xl font-bold text-red-400">{formatCurrency(totalExpense)}</p>
          </div>
        </div>

        {/* 테이블 */}
        <div className="bg-white dark:bg-gray-800 rounded-xl overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-gray-400 border-b border-gray-200 dark:border-gray-700">
                <th className="px-4 py-3 text-left">날짜</th>
                <th className="px-4 py-3 text-left">유형</th>
                <th className="px-4 py-3 text-left">카테고리</th>
                <th className="px-4 py-3 text-left">메모</th>
                <th className="px-4 py-3 text-right">금액</th>
                <th className="px-4 py-3 text-center">관리</th>
              </tr>
            </thead>
            <tbody>
              {transactions.length > 0 ? (
                transactions.map((tx) => (
                  <tr key={tx.id} className="border-b border-gray-100 dark:border-gray-700/50 hover:bg-gray-50 dark:hover:bg-gray-700/30">
                    <td className="px-4 py-3 text-gray-600 dark:text-gray-300">{formatDate(tx.transactionAt)}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`text-xs px-2 py-0.5 rounded-full ${
                          tx.type === "INCOME"
                            ? "bg-green-500/20 text-green-400"
                            : "bg-red-500/20 text-red-400"
                        }`}
                      >
                        {tx.type === "INCOME" ? "수입" : "지출"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-600 dark:text-gray-300">{tx.categoryName}</td>
                    <td className="px-4 py-3 text-gray-400 dark:text-gray-400">{tx.memo ?? "-"}</td>
                    <td className={`px-4 py-3 text-right font-medium ${
                      tx.type === "INCOME" ? "text-green-400" : "text-red-400"
                    }`}>
                      {tx.type === "INCOME" ? "+" : "-"}{formatCurrency(tx.amount)}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <button
                        onClick={() => handleDelete(tx.id)}
                        className="text-xs text-gray-500 hover:text-red-400"
                      >
                        삭제
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center text-gray-500">
                    거래 내역이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div className="flex justify-center gap-2 py-4">
              {Array.from({ length: totalPages }, (_, i) => (
                <button
                  key={i}
                  onClick={() => setPage(i)}
                  className={`w-8 h-8 rounded-lg text-xs ${
                    page === i
                      ? "bg-blue-600 text-white"
                      : "bg-gray-200 dark:bg-gray-700 text-gray-500 dark:text-gray-400 hover:bg-gray-300 dark:hover:bg-gray-600"
                  }`}
                >
                  {i + 1}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {showModal && (
        <TransactionModal
          onClose={() => setShowModal(false)}
          onSuccess={fetchTransactions}
        />
      )}
    </DashboardLayout>
  );
}
