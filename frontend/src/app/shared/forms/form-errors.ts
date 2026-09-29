import { AbstractControl, FormGroup } from '@angular/forms';
import { problemDetailOf } from '../../core/api/problem-detail';

const MESSAGES: Record<string, (error: any) => string> = {
  required: () => 'This field is required',
  email: () => 'Enter a valid email address',
  maxlength: (e) => `At most ${e.requiredLength} characters`,
  min: (e) => `Must be at least ${e.min}`,
  max: (e) => `Must be at most ${e.max}`,
  pattern: () => 'Invalid format',
  pastDate: () => 'Must be in the past',
  notInFuture: () => 'Cannot be in the future',
  matDatepickerParse: () => 'Enter a valid date',
  notUnique: () => 'Already in use',
  server: (message: string) => message,
};

export function firstErrorMessage(
  control: AbstractControl,
  overrides: Record<string, string> = {},
): string {
  const [key, value] = Object.entries(control.errors ?? {})[0] ?? [];
  if (!key) {
    return '';
  }
  return overrides[key] ?? MESSAGES[key]?.(value) ?? 'Invalid value';
}

/**
 * Copies field-level violations from a ProblemDetail response onto the matching controls.
 * Returns true when at least one violation could be attached to a control.
 */
export function applyServerErrors(form: FormGroup, error: unknown): boolean {
  const violations = problemDetailOf(error)?.errors ?? [];
  let applied = false;
  for (const { field, message } of violations) {
    const control = form.get(field);
    if (control) {
      control.setErrors({ ...control.errors, server: message });
      control.markAsTouched();
      applied = true;
    }
  }
  return applied;
}
