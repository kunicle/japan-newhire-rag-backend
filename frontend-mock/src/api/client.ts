const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ??
  "http://localhost:8080";

type ErrorPayload = {
  status?: number;
  code?: string;
  message?: string;
};

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);

    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

export function getAccessToken(): string | null {
  return sessionStorage.getItem("accessToken");
}

export function saveAccessToken(token: string): void {
  sessionStorage.setItem("accessToken", token);
}

export function removeAccessToken(): void {
  sessionStorage.removeItem("accessToken");
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  const token = getAccessToken();

  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  const responseText = await response.text();

  if (!response.ok) {
    let errorPayload: ErrorPayload = {};

    if (responseText) {
      try {
        errorPayload = JSON.parse(responseText) as ErrorPayload;
      } catch {
        errorPayload = {
          message: responseText,
        };
      }
    }

    throw new ApiError(
      response.status,
      errorPayload.code ?? "UNKNOWN_ERROR",
      errorPayload.message ?? "API 요청에 실패했습니다.",
    );
  }

  if (!responseText) {
    return undefined as T;
  }

  return JSON.parse(responseText) as T;
}