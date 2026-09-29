import { AsyncPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, ValidatorFn } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { combineLatest, concatMap, finalize, from, map, startWith } from 'rxjs';
import { ignoreReportedErrors } from '../../../core/http/ignore-reported-errors';
import { NotificationService } from '../../../core/notification/notification-service';
import { HasUnsavedChanges } from '../../../shared/guards/unsaved-changes-guard';
import { StateMessage } from '../../../shared/state-message/state-message';
import { CourseStore } from '../../courses/data/course-store';
import { MAX_COURSES_PER_STUDENT } from '../data/student';
import { StudentStore } from '../data/student-store';

@Component({
  selector: 'app-enroll-tab',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatListModule,
    MatProgressSpinnerModule,
    StateMessage,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './enroll-tab.html',
  styles: `
    .hint {
      margin-top: 0;
      color: var(--mat-sys-on-surface-variant);
    }
    .hint.error {
      color: var(--mat-sys-error);
    }
  `,
})
export class EnrollTab implements OnInit, HasUnsavedChanges {
  private readonly studentStore = inject(StudentStore);
  private readonly courseStore = inject(CourseStore);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly remainingSlots = toSignal(
    this.studentStore.enrollments$.pipe(map((e) => MAX_COURSES_PER_STUDENT - e.length)),
    { initialValue: MAX_COURSES_PER_STUDENT },
  );
  protected readonly availableCourses$ = combineLatest([
    this.courseStore.options$,
    this.studentStore.enrollments$,
  ]).pipe(
    map(([courses, enrollments]) => {
      const enrolled = new Set(enrollments.map((e) => e.courseId));
      return courses.filter((course) => !enrolled.has(course.id));
    }),
  );
  protected readonly optionsFailed = signal(false);
  protected readonly enrolling = signal(false);

  private readonly withinRemainingSlots: ValidatorFn = (control) => {
    const max = this.remainingSlots();
    return control.value.length > max ? { tooMany: { max } } : null;
  };

  protected readonly form = inject(NonNullableFormBuilder).group({
    courseIds: [[] as number[], this.withinRemainingSlots],
  });
  protected readonly status$ = this.form.statusChanges.pipe(startWith(this.form.status));

  ngOnInit(): void {
    this.courseStore
      .loadOptions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ error: () => this.optionsFailed.set(true) });
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty && this.form.controls.courseIds.value.length > 0;
  }

  protected enroll(): void {
    const control = this.form.controls.courseIds;
    const ids = control.value;
    if (!ids.length || this.form.invalid) {
      return;
    }
    const enrolled: number[] = [];
    this.enrolling.set(true);
    from(ids)
      .pipe(
        concatMap((courseId) => this.studentStore.enroll(courseId)),
        ignoreReportedErrors(),
        finalize(() => {
          this.enrolling.set(false);
          const remaining = ids.filter((id) => !enrolled.includes(id));
          this.form.reset({ courseIds: remaining });
          if (remaining.length) {
            control.markAsDirty();
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (enrollment) => enrolled.push(enrollment.courseId),
        complete: () => {
          if (enrolled.length === ids.length) {
            this.notifications.success(`Enrolled in ${enrolled.length} course(s)`);
          }
        },
      });
  }
}
