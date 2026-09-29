import { EMPTY, MonoTypeOperatorFunction, catchError } from 'rxjs';

/**
 * For fire-and-forget actions whose failures problemDetailInterceptor has already shown to the
 * user: completes quietly instead of surfacing an unhandled error.
 */
export function ignoreReportedErrors<T>(): MonoTypeOperatorFunction<T> {
  return catchError(() => EMPTY);
}
