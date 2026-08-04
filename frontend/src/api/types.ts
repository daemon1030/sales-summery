export type TransactionType = "INCOME" | "EXPENSE";
export type CostType = "FIXED" | "VARIABLE" | "NONE";
export type Frequency = "DAILY" | "MONTHLY" | "IRREGULAR";

export interface ApiErrorBody { code: string; message: string }
export interface ApiEnvelope<T> { success: boolean; data?: T; error?: ApiErrorBody }
export interface MessageResponse { message: string }

export interface LoginResponse { accessToken: string; tokenType: string; expiresInSeconds: number }
export interface User {
  userId: number;
  loginId: string;
  name: string;
  status: "ACTIVE" | "SUSPENDED" | "WITHDRAWN";
  settlementStartDay: number;
}
export interface Category {
  categoryId: number;
  categoryName: string;
  transactionType: TransactionType;
  costType: CostType;
  frequency: Frequency;
  active: boolean;
}
export interface FinancialRecord {
  recordId: number;
  categoryId: number;
  categoryName: string;
  transactionType: TransactionType;
  recordDate: string;
  amount: number;
  memo: string | null;
  createdAt: string;
}
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
export interface ProfitSummary { totalIncome: number; totalExpense: number; netProfit: number }
export interface PeriodSummary { startDate: string; endDate: string; summary: ProfitSummary }
export interface CategoryTotal {
  categoryId: number;
  categoryName: string;
  transactionType: TransactionType;
  totalAmount: number;
}
export interface DailyNote {
  noteId: number;
  noteDate: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}
