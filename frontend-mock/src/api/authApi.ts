import { apiRequest } from "./client";

export type LoginRequest = {
  email: string;
  password: string;
  deviceInfo: string;
};

export type TokenResponse = {
  accessToken: string;
};

export function login(
  request: LoginRequest,
): Promise<TokenResponse> {
  return apiRequest<TokenResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(request),
    credentials: "include",
  });
}