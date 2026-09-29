import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { BACKGROUND_REQUEST } from '../api/api-config';
import { describeHttpError } from '../api/problem-detail';
import { NotificationService } from '../notification/notification-service';

/**
 * Surfaces every failed foreground request as a snackbar, then rethrows so callers can still react
 * (e.g. map field errors onto a form).
 */
export const problemDetailInterceptor: HttpInterceptorFn = (req, next) => {
  const notifications = inject(NotificationService);
  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && !req.context.get(BACKGROUND_REQUEST)) {
        notifications.error(describeHttpError(error));
      }
      return throwError(() => error);
    }),
  );
};
