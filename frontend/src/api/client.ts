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
  if (init.body !== undefined) headers.set("Content-Type", "application/json");

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

export function jsonBody(value: unknown): Pick<RequestInit, "body"> {
  return { body: JSON.stringify(value) };
}
