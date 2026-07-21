import { Category } from "@/types";

export const CATEGORIES: Category[] = [
  { uuid: 1, type: "EXPENSE", code: "FOOD", name: "식비" },
  { uuid: 2, type: "EXPENSE", code: "CAFE", name: "카페" },
  { uuid: 3, type: "EXPENSE", code: "TRANSPORT", name: "교통" },
  { uuid: 4, type: "EXPENSE", code: "SHOPPING", name: "쇼핑" },
  { uuid: 5, type: "EXPENSE", code: "HOUSING", name: "주거 및 통신" },
  { uuid: 6, type: "EXPENSE", code: "MEDICAL", name: "의료건강" },
  { uuid: 7, type: "EXPENSE", code: "CULTURE", name: "문화생활" },
  { uuid: 8, type: "EXPENSE", code: "EDUCATION", name: "교육" },
  { uuid: 9, type: "EXPENSE", code: "ETC", name: "기타" },
  { uuid: 101, type: "INCOME", code: "SALARY", name: "급여" },
  { uuid: 102, type: "INCOME", code: "INVESTMENT", name: "투자수익" },
  { uuid: 103, type: "INCOME", code: "ETC", name: "기타" },
  { uuid: 104, type: "INCOME", code: "PART_TIME", name: "아르바이트" },
];

export const EXPENSE_CATEGORIES = CATEGORIES.filter(
  (c) => c.type === "EXPENSE"
);
export const INCOME_CATEGORIES = CATEGORIES.filter((c) => c.type === "INCOME");

export const CATEGORY_COLORS: Record<string, string> = {
  FOOD: "#FF6384",
  CAFE: "#FF9F40",
  TRANSPORT: "#FFCD56",
  SHOPPING: "#4BC0C0",
  HOUSING: "#36A2EB",
  MEDICAL: "#9966FF",
  CULTURE: "#FF6384",
  EDUCATION: "#C9CBCF",
  ETC: "#AAAAAA",
};
