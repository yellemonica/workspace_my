import { HttpErrorResponse } from '@angular/common/http';

export interface FieldViolation {
  field: string;
  message: string;
}

/** RFC 7807 body returned by the API; `code` and `errors` are the backend's extension members. */
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  code?: string;
  errors?: FieldViolation[];
}

export function problemDetailOf(error: unknown): ProblemDetail | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }
  const body: unknown = error.error;
  return typeof body === 'object' && body !== null && 'status' in body && 'title' in body
    ? (body as ProblemDetail)
    : null;
}

export function describeHttpError(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'Cannot reach the server. Check that the backend is running.';
  }
  const problem = problemDetailOf(error);
  return problem?.detail ?? problem?.title ?? `Request failed (${error.status})`;
}
