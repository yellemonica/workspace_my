import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { applyServerErrors, firstErrorMessage } from './form-errors';

describe('applyServerErrors', () => {
  const problem = (errors: { field: string; message: string }[]) =>
    new HttpErrorResponse({
      status: 409,
      error: {
        type: 'urn:problem-type:duplicate-resource',
        title: 'Duplicate resource',
        status: 409,
        errors,
      },
    });

  it('attaches field violations to matching controls and marks them touched', () => {
    const form = new FormGroup({
      email: new FormControl('ada@x.edu'),
      name: new FormControl('Ada'),
    });

    const applied = applyServerErrors(
      form,
      problem([{ field: 'email', message: 'already exists' }]),
    );

    expect(applied).toBe(true);
    expect(form.controls.email.errors).toEqual({ server: 'already exists' });
    expect(form.controls.email.touched).toBe(true);
    expect(form.controls.name.errors).toBeNull();
    expect(firstErrorMessage(form.controls.email)).toBe('already exists');
  });

  it('ignores violations for unknown fields and non-problem errors', () => {
    const form = new FormGroup({ email: new FormControl('') });

    expect(applyServerErrors(form, problem([{ field: 'courseId', message: 'x' }]))).toBe(false);
    expect(applyServerErrors(form, new HttpErrorResponse({ status: 0 }))).toBe(false);
    expect(applyServerErrors(form, new Error('boom'))).toBe(false);
  });
});

describe('firstErrorMessage', () => {
  it('prefers caller overrides and formats parameterised errors', () => {
    const control = new FormControl('toolong', Validators.maxLength(3));

    expect(firstErrorMessage(control)).toBe('At most 3 characters');
    expect(firstErrorMessage(control, { maxlength: 'Too long' })).toBe('Too long');
    expect(firstErrorMessage(new FormControl('ok'))).toBe('');
  });
});
