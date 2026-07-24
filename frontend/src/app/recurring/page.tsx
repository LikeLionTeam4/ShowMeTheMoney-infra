"use client";

import { useEffect, useState, useCallback } from "react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { RecurringItem, TransactionType } from "@/types";
import { formatCurrency } from "@/lib/format";
import { EXPENSE_CATEGORIES, INCOME_CATEGORIES } from "@/lib/constants";
import api from "@/lib/api";

export default function RecurringPage() {
  const [items, setItems] = useState<RecurringItem[]>([]);
  const [typeFilter, setTypeFilter] = useState<TransactionType | "ALL">("ALL");
  const [showForm, setShowForm] = useState(false);

  const fetchItems = useCallback(() => {
    const params = new URLSearchParams();
    if (typeFilter !== "ALL") params.set("type", typeFilter === "INCOME" ? "1" : "0");

    api
      .get<{ data: RecurringItem[] }>(`/api/recurring-items?${params}`)
      .then((res) => setItems(res.data.data))
      .catch(() => {});
  }, [typeFilter]);

  useEffect(() => {
    fetchItems();
  }, [fetchItems]);

  async function handleToggle(item: RecurringItem) {
    await api.put(`/api/recurring-items/${item.id}`, {
      isActive: item.isActive === 1 ? false : true,
    });
    fetchItems();
  }

  async function handleDelete(id: number) {
    if (!confirm("삭제하시겠습니까?")) return;
    await api.delete(`/api/recurring-items/${id}`);
    fetchItems();
  }

  const filtered =
    typeFilter === "ALL" ? items : items.filter((i) => i.type === typeFilter);

  return (
    <DashboardLayout>
      <div className="p-6 text-foreground">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-xl font-bold">고정 수입/지출</h1>
            <p className="text-sm text-muted mt-0.5">
              매월 반복되는 수입과 지출을 관리하세요
            </p>
          </div>
          <button
            onClick={() => setShowForm(true)}
            className="btn-pill btn-pill-solid"
          >
            + 항목 추가
          </button>
        </div>

        {/* 필터 */}
        <div className="flex gap-1 mb-5">
          {(["ALL", "INCOME", "EXPENSE"] as const).map((t) => (
            <button
              key={t}
              onClick={() => setTypeFilter(t)}
              className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
                typeFilter === t
                  ? "bg-accent text-canvas"
                  : "bg-hairline text-muted-secondary hover:opacity-80"
              }`}
            >
              {t === "ALL" ? "전체" : t === "INCOME" ? "수입" : "지출"}
            </button>
          ))}
        </div>

        {/* 리스트 */}
        <div className="flex flex-col gap-3">
          {filtered.length > 0 ? (
            filtered.map((item) => (
              <div
                key={item.id}
                className={`card p-4 flex items-center justify-between ${
                  item.isActive === 0 ? "opacity-50" : ""
                }`}
              >
                <div className="flex items-center gap-4">
                  <div
                    className={`w-2 h-2 rounded-full ${
                      item.type === "INCOME" ? "bg-green-400" : "bg-red-400"
                    }`}
                  />
                  <div>
                    <p className="text-sm font-medium text-foreground">{item.name}</p>
                    <p className="text-xs text-muted">
                      {item.categoryName} · 매월 {item.billingDay}일
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-4">
                  <span
                    className={`text-sm font-medium ${
                      item.type === "INCOME" ? "text-green-400" : "text-red-400"
                    }`}
                  >
                    {item.type === "INCOME" ? "+" : "-"}
                    {formatCurrency(item.amount)}
                  </span>
                  <button
                    onClick={() => handleToggle(item)}
                    className={`text-xs px-2 py-1 rounded-full border transition-colors ${
                      item.isActive === 1
                        ? "border-green-500/30 text-green-400 hover:bg-green-500/10"
                        : "border-hairline text-muted hover:bg-hairline"
                    }`}
                  >
                    {item.isActive === 1 ? "활성" : "비활성"}
                  </button>
                  <button
                    onClick={() => handleDelete(item.id)}
                    className="text-xs text-muted hover:text-red-400"
                  >
                    삭제
                  </button>
                </div>
              </div>
            ))
          ) : (
            <div className="card p-12 text-center text-muted">
              등록된 고정 항목이 없습니다.
            </div>
          )}
        </div>
      </div>

      {showForm && (
        <RecurringForm
          onClose={() => setShowForm(false)}
          onSuccess={fetchItems}
        />
      )}
    </DashboardLayout>
  );
}

function RecurringForm({
  onClose,
  onSuccess,
}: {
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [type, setType] = useState<TransactionType>("EXPENSE");
  const [categoryCode, setCategoryCode] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [amount, setAmount] = useState("");
  const [billingDay, setBillingDay] = useState("1");
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
      await api.post("/api/recurring-items", {
        type: type === "INCOME" ? 1 : 0,
        categoryCode,
        name,
        amount: Number(amount),
        billingDay: Number(billingDay),
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
          <h2 className="text-base font-bold">고정 항목 등록</h2>
          <button onClick={onClose} className="text-muted hover:text-foreground">✕</button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* 유형 탭 */}
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

          {/* 항목명 */}
          <div className="flex flex-col gap-1">
            <label className="text-xs text-muted">항목명 *</label>
            <input
              type="text"
              placeholder="예: 넷플릭스 구독"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              className="bg-canvas border border-hairline rounded-lg px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-accent"
            />
          </div>

          <div className="flex gap-3">
            {/* 금액 */}
            <div className="flex-1 flex flex-col gap-1">
              <label className="text-xs text-muted">금액 *</label>
              <div className="relative">
                <input
                  type="number"
                  placeholder="금액"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  required
                  min={1}
                  className="w-full bg-canvas border border-hairline rounded-lg px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-accent pr-8"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted">원</span>
              </div>
            </div>

            {/* 결제일 */}
            <div className="w-24 flex flex-col gap-1">
              <label className="text-xs text-muted">결제일 *</label>
              <div className="relative">
                <input
                  type="number"
                  placeholder="1"
                  value={billingDay}
                  onChange={(e) => setBillingDay(e.target.value)}
                  required
                  min={1}
                  max={31}
                  className="w-full bg-canvas border border-hairline rounded-lg px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-accent pr-6"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-muted">일</span>
              </div>
            </div>
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
