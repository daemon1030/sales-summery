import { apiDownload, apiRequest, jsonBody } from "./client";
import type {
  Category, CategoryBreakdown, CategoryTotal, CostType, DailyNote, FinancialRecord, Frequency,
  LoginResponse, MessageResponse, PageResponse, PeriodSummary, ProfitSummary,
  ProfitTrendPoint, TransactionType, TrendUnit, User, ImportAnalysis, ImportColumnMapping, ImportResult,
} from "./types";

const query = <T extends object>(params: T) => {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") search.set(key, String(value));
  });
  return search.toString();
};

export const authApi = {
  login: (body: { loginId: string; password: string }) =>
    apiRequest<LoginResponse>("/auth/login", { method: "POST", ...jsonBody(body) }),
  signup: (body: { loginId: string; password: string; name: string }) =>
    apiRequest<User>("/auth/signup", { method: "POST", ...jsonBody(body) }),
};

export const userApi = {
  me: () => apiRequest<User>("/users/me"),
  changeName: (name: string) => apiRequest<User>("/users/me/name", { method: "PATCH", ...jsonBody({ name }) }),
  changeSettlementDay: (settlementStartDay: number) => apiRequest<{ settlementStartDay: number }>(
    "/users/me/settlement-start-day", { method: "PATCH", ...jsonBody({ settlementStartDay }) }),
  changePassword: (currentPassword: string, newPassword: string) => apiRequest<MessageResponse>(
    "/users/me/password", { method: "PATCH", ...jsonBody({ currentPassword, newPassword }) }),
  withdraw: (currentPassword: string) => apiRequest<MessageResponse>(
    "/users/me", { method: "DELETE", ...jsonBody({ currentPassword }) }),
};

export const categoryApi = {
  list: (activeOnly = false) => apiRequest<Category[]>(`/categories?activeOnly=${activeOnly}`),
  create: (body: { categoryName: string; transactionType: TransactionType; costType: CostType; frequency: Frequency }) =>
    apiRequest<Category>("/categories", { method: "POST", ...jsonBody(body) }),
  rename: (id: number, categoryName: string) => apiRequest<Category>(
    `/categories/${id}/name`, { method: "PATCH", ...jsonBody({ categoryName }) }),
  activate: (id: number) => apiRequest<Category>(`/categories/${id}/activate`, { method: "PATCH" }),
  deactivate: (id: number) => apiRequest<Category>(`/categories/${id}/deactivate`, { method: "PATCH" }),
};

export interface RecordSearch {
  startDate: string;
  endDate: string;
  categoryId?: number;
  transactionType?: TransactionType;
  page?: number;
  size?: number;
  sort?: string;
}
export interface RecordPayload { categoryId: number; recordDate: string; amount: number; memo: string | null }

export const recordApi = {
  search: (params: RecordSearch) => apiRequest<PageResponse<FinancialRecord>>(`/financial-records?${query(params)}`),
  create: (body: RecordPayload) => apiRequest<FinancialRecord>("/financial-records", { method: "POST", ...jsonBody(body) }),
  update: (id: number, body: RecordPayload) => apiRequest<FinancialRecord>(
    `/financial-records/${id}`, { method: "PATCH", ...jsonBody(body) }),
  remove: (id: number) => apiRequest<MessageResponse>(`/financial-records/${id}`, { method: "DELETE" }),
};

export const dashboardApi = {
  today: () => apiRequest<ProfitSummary>("/dashboard/today"),
  current: () => apiRequest<PeriodSummary>("/dashboard/current-settlement"),
  summary: (startDate: string, endDate: string) => apiRequest<ProfitSummary>(
    `/dashboard/summary?${query({ startDate, endDate })}`),
  categoryTotals: (startDate: string, endDate: string) => apiRequest<CategoryTotal[]>(
    `/dashboard/category-totals?${query({ startDate, endDate })}`),
  recent: () => apiRequest<FinancialRecord[]>("/dashboard/recent"),
  profitTrend: (params: {
    unit: TrendUnit;
    startDate?: string;
    endDate?: string;
    year?: number;
    startYear?: number;
    endYear?: number;
  }) => apiRequest<ProfitTrendPoint[]>(`/dashboard/profit-trend?${query(params)}`),
  categoryBreakdown: (year: number, month: number, transactionType: TransactionType) =>
    apiRequest<CategoryBreakdown>(`/dashboard/category-breakdown?${query({ year, month, transactionType })}`),
};

export const noteApi = {
  list: (startDate: string, endDate: string) => apiRequest<DailyNote[]>(
    `/daily-notes?${query({ startDate, endDate })}`),
  create: (body: { noteDate: string; content: string }) => apiRequest<DailyNote>(
    "/daily-notes", { method: "POST", ...jsonBody(body) }),
  update: (id: number, content: string) => apiRequest<DailyNote>(
    `/daily-notes/${id}`, { method: "PATCH", ...jsonBody({ content }) }),
  remove: (id: number) => apiRequest<MessageResponse>(`/daily-notes/${id}`, { method: "DELETE" }),
};

export const importApi = {
  template: () => apiDownload("/financial-record-imports/template"),
  analyze: (files: File[]) => {
    const body = new FormData();
    files.forEach(file => body.append("files", file));
    return apiRequest<ImportAnalysis>("/financial-record-imports/analyze", { method: "POST", body });
  },
  confirm: (importToken: string, mappings: ImportColumnMapping[]) => apiRequest<ImportResult>(
    `/financial-record-imports/${importToken}/confirm`, { method: "POST", ...jsonBody({ mappings }) }),
};
