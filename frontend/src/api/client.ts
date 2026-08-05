import type { ApiEnvelope } from "./types";

export const TOKEN_KEY = "sales-service-access-token";

export class ApiError extends Error {
  constructor(public readonly code: string, message: string, public readonly status: number) {
    super(message);
    this.name = "ApiError";
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body !== undefined && !(init.body instanceof FormData)) headers.set("Content-Type", "application/json");

  const token = sessionStorage.getItem(TOKEN_KEY);
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const response = await fetch(`/api/v1${path}`, { ...init, headers });
  const text = await response.text();
  const envelope = text ? JSON.parse(text) as ApiEnvelope<T> : undefined;

  if (!response.ok || !envelope?.success) {
    const code = envelope?.error?.code ?? "NETWORK_ERROR";
    const message = envelope?.error?.message ?? "요청을 처리하지 못했습니다.";
    if (response.status === 401) {
      sessionStorage.removeItem(TOKEN_KEY);
      window.dispatchEvent(new Event("auth:expired"));
    }
    throw new ApiError(code, message, response.status);
  }

  return envelope.data as T;
}

export async function apiDownload(path: string): Promise<{ blob: Blob; filename: string }> {
  const headers = new Headers({ Accept: "application/octet-stream" });
  const token = sessionStorage.getItem(TOKEN_KEY);
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const response = await fetch(`/api/v1${path}`, { headers });
  if (!response.ok) {
    let code = "DOWNLOAD_ERROR";
    let message = "파일을 내려받지 못했습니다.";
    try {
      const envelope = await response.json() as ApiEnvelope<never>;
      code = envelope.error?.code ?? code;
      message = envelope.error?.message ?? message;
    } catch { /* 바이너리 오류 응답은 기본 메시지를 사용한다. */ }
    throw new ApiError(code, message, response.status);
  }
  const disposition = response.headers.get("Content-Disposition") ?? "";
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  const plain = disposition.match(/filename="?([^";]+)"?/i)?.[1];
  return { blob: await response.blob(), filename: encoded ? decodeURIComponent(encoded) : plain ?? "가계부_기본_양식.xlsx" };
}

export function jsonBody(value: unknown): Pick<RequestInit, "body"> {
  return { body: JSON.stringify(value) };
}
