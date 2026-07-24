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
      <div className="bg-canvas rounded-2xl w-full max-w-md p-6 border border-hairline text-foreground">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-base font-bold">거래 등록</h2>
          <button onClick={onClose} className="text-muted hover:text-foreground">✕</button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* 거래 유형 탭 */}
          <div>
            <p className="text-xs text-muted mb-2">거래 유형 *</p>
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
                        : "bg-green-500 border-green-500 text-white"
                      : "border-hairline text-muted-secondary hover:bg-hairline"
                  }`}
                >
                  {t === "EXPENSE" ? "지출" : "수입"}
                </button>
              ))}
            </div>
          </div>

          {/* 카테고리 */}
          <div>
            <p className="text-xs text-muted mb-2">카테고리 *</p>
            <div className="flex flex-wrap gap-2">
              {categories.map((cat) => (
                <button
                  key={cat.code}
                  type="button"
                  onClick={() => setCategoryCode(cat.code)}
                  className={`px-3 py-1.5 rounded-lg text-xs border transition-colors ${
                    categoryCode === cat.code
                      ? "bg-foreground border-foreground text-canvas"
                      : "border-hairline text-muted-secondary hover:bg-hairline"
                  }`}
                >
                  {cat.name}
                </button>
              ))}
            </div>
          </div>

          {/* 금액 */}
          <div className="flex flex-col gap-1">
            <label className="text-xs text-muted">금액 *</label>
            <div className="relative">
              <input
                type="number"
                placeholder="금액을 입력하세요"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
                min={1}
                className="w-full bg-canvas border border-hairline rounded-lg px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-accent pr-8"
              />
              <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted">원</span>
            </div>
          </div>

          {/* 날짜 */}
          <div className="flex flex-col gap-1">
            <label className="text-xs text-muted">날짜</label>
            <input
              type="date"
              value={transactionAt}
              onChange={(e) => setTransactionAt(e.target.value)}
              className="bg-canvas border border-hairline rounded-lg px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-accent"
            />
          </div>

          {/* 메모 */}
          <div className="flex flex-col gap-1">
            <label className="text-xs text-muted">메모</label>
            <input
              type="text"
              placeholder="메모를 입력하세요"
              value={memo}
              onChange={(e) => setMemo(e.target.value)}
              className="bg-canvas border border-hairline rounded-lg px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-accent"
            />
          </div>

          {error && (
            <p className="text-xs text-red-400 bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2">
              {error}
            </p>
          )}

          <div className="flex gap-2 mt-1">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 btn-pill btn-pill-outline"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 btn-pill btn-pill-solid font-medium"
            >
              {loading ? "등록 중..." : "등록"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
