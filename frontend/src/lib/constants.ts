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

// 8-hue categorical palette (validated: scripts/validate_palette.js in the
// dataviz skill, CVD-safe adjacent-pair order). ETC is deliberately excluded
// from the identity palette and folds to a neutral "Other" gray instead of a
// generated 9th hue.
export const CATEGORY_COLORS: Record<string, string> = {
  FOOD: "#eb6834",
  CAFE: "#1baf7a",
  TRANSPORT: "#eda100",
  SHOPPING: "#e87ba4",
  HOUSING: "#008300",
  MEDICAL: "#4a3aa7",
  CULTURE: "#e34948",
  EDUCATION: "#2a78d6",
  ETC: "#a3a3a3",
};

export const CATEGORY_COLORS_DARK: Record<string, string> = {
  FOOD: "#d95926",
  CAFE: "#199e70",
  TRANSPORT: "#c98500",
  SHOPPING: "#d55181",
  HOUSING: "#008300",
  MEDICAL: "#9085e9",
  CULTURE: "#e66767",
  EDUCATION: "#3987e5",
  ETC: "#9c9c9c",
};
