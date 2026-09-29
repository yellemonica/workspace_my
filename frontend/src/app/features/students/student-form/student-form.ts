import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, output } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { startWith } from 'rxjs';
import { applyServerErrors, firstErrorMessage } from '../../../shared/forms/form-errors';
import { notInFuture, pastDate, uniqueValue } from '../../../shared/forms/validators';
import { fromIsoDate, toIsoDate } from '../../../shared/util/iso-date';
import { Student, StudentRequest } from '../data/student';
import { StudentStore } from '../data/student-store';

/**
 * Student form shared by the create page, the edit page and the Profile tab. It owns validation
 * and form state; the host decides what saving means and feeds server errors back in.
 */
@Component({
  selector: 'app-student-form',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './student-form.html',
})
export class StudentForm {
  private readonly store = inject(StudentStore);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly student = input<Student | null>(null);
  readonly saving = input(false);
  readonly submitLabel = input('Save');
  readonly save = output<StudentRequest>();

  protected readonly today = new Date();

  readonly form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: this.fb.control('', {
      validators: [Validators.required, Validators.email, Validators.maxLength(254)],
      asyncValidators: [
        uniqueValue(
          (email) => this.store.isEmailAvailable(email, this.student()?.id ?? null),
          () => this.student()?.email,
        ),
      ],
    }),
    dateOfBirth: this.fb.control<Date | null>(null, [Validators.required, pastDate]),
    enrollmentDate: this.fb.control<Date | null>(null, notInFuture),
  });

  protected readonly status$ = this.form.statusChanges.pipe(startWith(this.form.status));

  constructor() {
    effect(() => this.reset(this.student()));
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  applyServerErrors(error: unknown): void {
    applyServerErrors(this.form, error);
  }

  markSaved(): void {
    this.form.markAsPristine();
  }

  protected discard(): void {
    this.reset(this.student());
  }

  protected errorFor(control: AbstractControl): string {
    return firstErrorMessage(control, { notUnique: 'Another student already uses this email' });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { firstName, lastName, email, dateOfBirth, enrollmentDate } = this.form.getRawValue();
    this.save.emit({
      firstName,
      lastName,
      email,
      dateOfBirth: toIsoDate(dateOfBirth!),
      enrollmentDate: enrollmentDate ? toIsoDate(enrollmentDate) : null,
    });
  }

  private reset(student: Student | null): void {
    this.form.reset({
      firstName: student?.firstName ?? '',
      lastName: student?.lastName ?? '',
      email: student?.email ?? '',
      dateOfBirth: student ? fromIsoDate(student.dateOfBirth) : null,
      enrollmentDate: student ? fromIsoDate(student.enrollmentDate) : null,
    });
  }
}
