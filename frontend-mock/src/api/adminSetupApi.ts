import { apiRequest } from "./client";

export type RoleType =
  | "EMPLOYEE"
  | "MANAGER"
  | "HR_MANAGER"
  | "SYSTEM_ADMIN";

export type MyProfileResponse = {
  appUserId: number;
  employeeId: number;
  employeeName: string;
  email: string;
  roles: RoleType[];
};

export type UserRolesResponse = {
  appUserId: number;
  roles: RoleType[];
};

export function getMyProfile(): Promise<MyProfileResponse> {
  return apiRequest<MyProfileResponse>("/api/me");
}

export function updateUserRoles(
  appUserId: number,
  roles: RoleType[],
): Promise<UserRolesResponse> {
  return apiRequest<UserRolesResponse>(
    `/api/admin/users/${appUserId}/roles`,
    {
      method: "PATCH",
      body: JSON.stringify({ roles }),
    },
  );
}