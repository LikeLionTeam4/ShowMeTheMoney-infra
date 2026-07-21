export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat("ko-KR", {
    style: "currency",
    currency: "KRW",
  }).format(amount);
}

export function formatYearMonth(yearMonth: number): string {
  const str = String(yearMonth);
  return `${str.slice(0, 4)}년 ${str.slice(4)}월`;
}

export function toYearMonth(date: Date = new Date()): number {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  return Number(`${y}${m}`);
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}
