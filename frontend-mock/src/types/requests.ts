export type CoursePublicationStatus =
  | "DRAFT"
  | "PUBLIC"
  | "PRIVATE";

export type CourseCreateRequest = {
  courseName: string;
  courseDescription: string;
  required: boolean;
  trainingStartDate: string;
  trainingEndDate: string;
};

export type CourseUpdateRequest =
  CourseCreateRequest;

export type CoursePublicationUpdateRequest = {
  publicationStatus: CoursePublicationStatus;
};