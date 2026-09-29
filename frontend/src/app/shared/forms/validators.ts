import { AbstractControl, AsyncValidatorFn, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Observable, catchError, map, of, switchMap, timer } from 'rxjs';

const ASYNC_DEBOUNCE_MS = 400;

function startOfToday(): Date {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

export const pastDate: ValidatorFn = (control) =>
  control.value instanceof Date && control.value >= startOfToday() ? { pastDate: true } : null;

export const notInFuture: ValidatorFn = (control) => {
  const tomorrow = startOfToday();
  tomorrow.setDate(tomorrow.getDate() + 1);
  return control.value instanceof Date && control.value >= tomorrow ? { notInFuture: true } : null;
};

/**
 * Async uniqueness check. Angular unsubscribes from the previous validation run on every change,
 * so the leading timer acts as a debounce and only the last value hits the server. A failed
 * lookup is not treated as a conflict: the server enforces uniqueness on submit anyway.
 * Values equal to `original()` (the record's own current value) are accepted without a request.
 */
export function uniqueValue(
  isAvailable: (value: string) => Observable<boolean>,
  original: () => string | undefined = () => undefined,
): AsyncValidatorFn {
  return (control: AbstractControl<string>): Observable<ValidationErrors | null> => {
    const value = control.value?.trim();
    if (!value || value.toLowerCase() === original()?.toLowerCase()) {
      return of(null);
    }
    return timer(ASYNC_DEBOUNCE_MS).pipe(
      switchMap(() => isAvailable(value)),
      map((available) => (available ? null : { notUnique: true })),
      catchError(() => of(null)),
    );
  };
}
