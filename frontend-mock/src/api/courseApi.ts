import { apiRequest } from "./client";
import type {
  CourseCreateRequest,
  CoursePublicationUpdateRequest,
  CourseUpdateRequest,
} from "../types/requests";

export function getCourses(
  page = 0,
  size = 20,
): Promise<unknown> {
  return apiRequest(
    `/api/hr/courses?page=${page}&size=${size}`,
  );
}

export function getCourse(
  courseId: number,
): Promise<unknown> {
  return apiRequest(`/api/hr/courses/${courseId}`);
}

export function createCourse(
  request: CourseCreateRequest,
): Promise<unknown> {
  return apiRequest("/api/hr/courses", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function updateCourse(
  courseId: number,
  request: CourseUpdateRequest,
): Promise<unknown> {
  return apiRequest(`/api/hr/courses/${courseId}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export function updateCoursePublication(
  courseId: number,
  request: CoursePublicationUpdateRequest,
): Promise<unknown> {
  return apiRequest(
    `/api/hr/courses/${courseId}/publication`,
    {
      method: "PATCH",
      body: JSON.stringify(request),
    },
  );
}

export function deleteCourse(
  courseId: number,
): Promise<void> {
  return apiRequest(`/api/hr/courses/${courseId}`, {
    method: "DELETE",
  });
}