import { FormControl, ValidationErrors } from '@angular/forms';
import { Observable, Subject, of, throwError } from 'rxjs';
import { notInFuture, pastDate, uniqueValue } from './validators';

describe('date validators', () => {
  const today = new Date();
  const tomorrow = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1);
  const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1);

  it('pastDate rejects today and later', () => {
    expect(pastDate(new FormControl(yesterday))).toBeNull();
    expect(pastDate(new FormControl(today))).toEqual({ pastDate: true });
  });

  it('notInFuture accepts today but not tomorrow', () => {
    expect(notInFuture(new FormControl(today))).toBeNull();
    expect(notInFuture(new FormControl(tomorrow))).toEqual({ notInFuture: true });
  });
});

describe('uniqueValue', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  function validate(validator: ReturnType<typeof uniqueValue>, value: string) {
    let result: ValidationErrors | null | undefined;
    (validator(new FormControl(value)) as Observable<ValidationErrors | null>).subscribe(
      (r) => (result = r),
    );
    return () => result;
  }

  it('debounces, then reports a taken value', () => {
    const isAvailable = vi.fn(() => of(false));
    const result = validate(uniqueValue(isAvailable), 'ada@x.edu');

    expect(isAvailable).not.toHaveBeenCalled();
    vi.advanceTimersByTime(400);
    expect(isAvailable).toHaveBeenCalledWith('ada@x.edu');
    expect(result()).toEqual({ notUnique: true });
  });

  it('skips the lookup for blank values and for the record’s own value', () => {
    const isAvailable = vi.fn(() => of(false));
    const validator = uniqueValue(isAvailable, () => 'ADA@x.edu');

    expect(validate(validator, '  ')()).toBeNull();
    expect(validate(validator, 'ada@x.edu')()).toBeNull();
    vi.advanceTimersByTime(400);
    expect(isAvailable).not.toHaveBeenCalled();
  });

  it('does not block the form when the lookup fails', () => {
    const result = validate(
      uniqueValue(() => throwError(() => new Error('offline'))),
      'ada@x.edu',
    );
    vi.advanceTimersByTime(400);
    expect(result()).toBeNull();
  });

  it('cancels the pending lookup when the subscription is dropped', () => {
    const response = new Subject<boolean>();
    const isAvailable = vi.fn(() => response);
    const subscription = (
      uniqueValue(isAvailable)(new FormControl('a@x.edu')) as Observable<unknown>
    ).subscribe();

    subscription.unsubscribe();
    vi.advanceTimersByTime(400);

    expect(isAvailable).not.toHaveBeenCalled();
  });
});
