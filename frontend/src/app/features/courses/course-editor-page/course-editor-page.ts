import { AsyncPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { finalize, startWith } from 'rxjs';
import { NotificationService } from '../../../core/notification/notification-service';
import { applyServerErrors, firstErrorMessage } from '../../../shared/forms/form-errors';
import { uniqueValue } from '../../../shared/forms/validators';
import { HasUnsavedChanges } from '../../../shared/guards/unsaved-changes-guard';
import { StateMessage } from '../../../shared/state-message/state-message';
import { Course, CourseRequest } from '../data/course';
import { CourseStore } from '../data/course-store';

const CODE_PATTERN = /^[A-Za-z]{2,4}\d{3}$/;

@Component({
  selector: 'app-course-editor-page',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    StateMessage,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './course-editor-page.html',
})
export class CourseEditorPage implements OnInit, HasUnsavedChanges {
  private readonly store = inject(CourseStore);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(NonNullableFormBuilder);

  /** Bound from the `:id` route parameter; absent on /courses/new. */
  readonly id = input<string>();

  protected readonly courseId = computed(() => (this.id() ? Number(this.id()) : null));
  protected readonly loadFailed = signal(false);
  protected readonly saving = signal(false);
  private loadedCode: string | undefined;

  protected readonly form = this.fb.group({
    code: this.fb.control('', {
      validators: [Validators.required, Validators.pattern(CODE_PATTERN)],
      asyncValidators: [
        uniqueValue(
          (code) => this.store.isCodeAvailable(code, this.courseId()),
          () => this.loadedCode,
        ),
      ],
    }),
    title: ['', [Validators.required, Validators.maxLength(150)]],
    credits: [3, [Validators.required, Validators.min(1), Validators.max(6)]],
    description: ['', Validators.maxLength(2000)],
  });

  protected readonly status$ = this.form.statusChanges.pipe(startWith(this.form.status));

  ngOnInit(): void {
    const id = this.courseId();
    if (id == null) {
      return;
    }
    this.store
      .find(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (course) => {
          this.loadedCode = course.code;
          this.form.reset(toFormValue(course));
        },
        error: () => this.loadFailed.set(true),
      });
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty && !this.saving();
  }

  protected errorFor(control: AbstractControl): string {
    return firstErrorMessage(control, {
      pattern: 'Use 2-4 letters followed by 3 digits, e.g. CS101',
      notUnique: 'Another course already uses this code',
    });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request: CourseRequest = { ...value, description: value.description.trim() || null };
    const id = this.courseId();

    this.saving.set(true);
    (id == null ? this.store.create(request) : this.store.update(id, request))
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (course) => {
          this.form.markAsPristine();
          this.notifications.success(`${course.code} saved`);
          this.router.navigate(['/courses']);
        },
        error: (error) => applyServerErrors(this.form, error),
      });
  }
}

function toFormValue(course: Course) {
  return {
    code: course.code,
    title: course.title,
    credits: course.credits,
    description: course.description ?? '',
  };
}
