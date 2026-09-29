/** Mirrors EnrollmentService.MAX_COURSES_PER_STUDENT; the server remains the authority. */
export const MAX_COURSES_PER_STUDENT = 6;

export interface Student {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  dateOfBirth: string;
  enrollmentDate: string;
}

export interface StudentRequest {
  firstName: string;
  lastName: string;
  email: string;
  dateOfBirth: string;
  enrollmentDate: string | null;
}

export interface Enrollment {
  courseId: number;
  code: string;
  title: string;
  credits: number;
  enrolledAt: string;
}
