export interface Course {
  id: number;
  code: string;
  title: string;
  credits: number;
  description: string | null;
  enrolledCount: number;
}

export interface CourseRequest {
  code: string;
  title: string;
  credits: number;
  description: string | null;
}
