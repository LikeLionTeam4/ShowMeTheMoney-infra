"use client";

import { useState } from "react";
import api from "@/lib/api";
import { TransactionType } from "@/types";
import { EXPENSE_CATEGORIES, INCOME_CATEGORIES } from "@/lib/constants";

interface Props {
  onClose: () => void;
  onSuccess: () => void;
}

export default function TransactionModal({ onClose, onSuccess }: Props) {
  const [type, setType] = useState<TransactionType>("EXPENSE");
  const [categoryCode, setCategoryCode] = useState<string | null>(null);
  const [amount, setAmount] = useState("");
  const [memo, setMemo] = useState("");
  const [transactionAt, setTransactionAt] = useState(
    new Date().toISOString().slice(0, 10)
  );
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const categories = type === "EXPENSE" ? EXPENSE_CATEGORIES : INCOME_CATEGORIES;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!categoryCode) {
      setError("카테고리를 선택해주세요.");
      return;
    }
    setError("");
    setLoading(true);
    try {
      await api.post("/api/transactions", {
        type: type === "INCOME" ? 1 : 0,
        categoryCode,
        amount: Number(amount),
        memo: memo || undefined,
        transactionAt,
      });
      onSuccess();
      onClose();
    } catch {
      setError("등록에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
      <div className="bg-white rounded-2xl w-full max-w-md p-6 shadow-xl text-gray-900">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-base font-bold">거래 등록</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">✕</button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* 거래 유형 탭 */}
          <div>
            <p className="text-xs text-gray-500 mb-2">거래 유형 *</p>
            <div className="flex gap-2">
              {(["EXPENSE", "INCOME"] as TransactionType[]).map((t) => (
                <button
                  key={t}
                  type="button"
                  onClick={() => { setType(t); setCategoryCode(null); }}
                  className={`flex-1 py-2 rounded-lg text-sm font-medium border transition-colors ${
                    type === t
                      ? t === "EXPENSE"
                        ? "bg-red-500 border-red-500 text-white"
                        : "bg-blue-500 border-blue-500 text-white"
                      : "border-gray-200 text-gray-500 hover:bg-gray-50"
                  }`}
                >
                  {t === "EXPENSE" ? "지출" : "수입"}
                </button>
              ))}
            </div>
          </div>

          {/* 카테고리 */}
          <div>
            <p className="text-xs text-gray-500 mb-2">카테고리 *</p>
            <div className="flex flex-wrap gap-2">
              {categories.map((cat) => (
                <button
                  key={cat.code}
                  type="button"
                  onClick={() => setCategoryCode(cat.code)}
                  className={`px-3 py-1.5 rounded-lg text-xs border transition-colors ${
                    categoryCode === cat.code
                      ? "bg-gray-800 border-gray-800 text-white"
                      : "border-gray-200 text-gray-600 hover:bg-gray-50"
                  }`}
                >
                  {cat.name}
                </button>
              ))}
            </div>
          </div>

          {/* 금액 */}
          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">금액 *</label>
            <div className="relative">
              <input
                type="number"
                placeholder="금액을 입력하세요"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
                min={1}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 pr-8"
              />
              <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-gray-400">원</span>
            </div>
          </div>

          {/* 날짜 */}
          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">날짜</label>
            <input
              type="date"
              value={transactionAt}
              onChange={(e) => setTransactionAt(e.target.value)}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* 메모 */}
          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">메모</label>
            <input
              type="text"
              placeholder="메모를 입력하세요"
              value={memo}
              onChange={(e) => setMemo(e.target.value)}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {error && (
            <p className="text-xs text-red-500 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
              {error}
            </p>
          )}

          <div className="flex gap-2 mt-1">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 border border-gray-200 rounded-lg text-sm text-gray-600 hover:bg-gray-50"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium disabled:opacity-50"
            >
              {loading ? "등록 중..." : "등록"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
