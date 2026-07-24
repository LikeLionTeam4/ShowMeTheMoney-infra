export type TransactionType = "INCOME" | "EXPENSE";

export type ExpenseCategoryCode =
  | "FOOD"
  | "CAFE"
  | "TRANSPORT"
  | "SHOPPING"
  | "HOUSING"
  | "MEDICAL"
  | "CULTURE"
  | "EDUCATION"
  | "ETC";

export type IncomeCategoryCode = "SALARY" | "INVESTMENT" | "ETC" | "PART_TIME";

export type CategoryCode = ExpenseCategoryCode | IncomeCategoryCode;

export interface Category {
  uuid: number;
  type: TransactionType;
  code: CategoryCode;
  name: string;
}

export interface User {
  id: number;
  username: string;
  name: string;
  email: string;
  grade: string;
}

export interface Transaction {
  id: number;
  type: TransactionType;
  categoryCode: CategoryCode;
  categoryName: string;
  amount: number;
  memo?: string;
  transactionAt: string;
}

export interface RecurringItem {
  id: number;
  type: TransactionType;
  categoryCode: CategoryCode;
  categoryName: string;
  name: string;
  amount: number;
  billingDay: number;
  isActive: 0 | 1;
}

export interface Budget {
  id: number;
  yearMonth: string;
  amount: number;
}

export interface DashboardSummary {
  yearMonth: string;
  totalIncome: number;
  totalExpense: number;
  balance: number;
  budgetAmount: number | null;
  budgetUsageRate: number | null;
  isOverBudget: boolean;
}

export interface CategoryExpense {
  categoryCode: CategoryCode;
  categoryName: string;
  amount: number;
  ratio: number;
}

export interface DailyBalancePoint {
  date: string;
  income: number;
  expense: number;
  cumulativeExpense: number;
  cumulativeBalance: number;
}

export interface DashboardDaily {
  yearMonth: string;
  budgetAmount: number | null;
  days: DailyBalancePoint[];
}

export interface TransactionPageResponse {
  content: Transaction[];
  totalElements: number;
  totalPages: number;
  totalIncome: number;
  totalExpense: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}
