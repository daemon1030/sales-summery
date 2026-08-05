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
export type TrendUnit = "DAILY" | "MONTHLY" | "YEARLY";
export type ProfitMetric = "INCOME" | "EXPENSE" | "NET_PROFIT";
export interface ProfitTrendPoint {
  period: string;
  totalIncome: number;
  totalExpense: number;
  netProfit: number;
}
export interface CategoryBreakdownItem {
  categoryId: number;
  categoryName: string;
  amount: number;
  percentage: number;
}
export interface CategoryBreakdown {
  year: number;
  month: number;
  transactionType: TransactionType;
  startDate: string;
  endDate: string;
  totalAmount: number;
  items: CategoryBreakdownItem[];
}
export interface DailyNote {
  noteId: number;
  noteDate: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export type ImportFileStatus = "READY" | "DUPLICATE" | "INVALID";
export type ImportColumnStatus = "AUTO_MATCHED" | "NEEDS_MAPPING";
export type ImportMappingAction = "EXISTING" | "CREATE" | "IGNORE";

export interface ImportFileAnalysis {
  fileName: string;
  status: ImportFileStatus;
  rowCount: number;
  recordCount: number;
  noteCount: number;
  errors: string[];
}

export interface ImportColumnAnalysis {
  header: string;
  status: ImportColumnStatus;
  categoryId: number | null;
  categoryName: string | null;
  valueCount: number;
  totalAmount: number;
}

export interface ImportAnalysis {
  importToken: string;
  files: ImportFileAnalysis[];
  columns: ImportColumnAnalysis[];
  requiresMapping: boolean;
  readyFileCount: number;
  duplicateFileCount: number;
  invalidFileCount: number;
}

export interface ImportColumnMapping {
  header: string;
  action: ImportMappingAction;
  categoryId: number | null;
  categoryName: string | null;
  transactionType: TransactionType | null;
  costType: CostType | null;
  frequency: Frequency | null;
}

export interface ImportResult {
  importedFileCount: number;
  duplicateFileCount: number;
  createdCategoryCount: number;
  createdRecordCount: number;
  createdNoteCount: number;
  skippedNoteCount: number;
}
