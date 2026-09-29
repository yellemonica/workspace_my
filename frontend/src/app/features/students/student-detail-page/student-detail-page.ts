import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { ignoreReportedErrors } from '../../../core/http/ignore-reported-errors';
import { NotificationService } from '../../../core/notification/notification-service';
import { ConfirmDialogService } from '../../../shared/confirm-dialog/confirm-dialog-service';
import { StateMessage } from '../../../shared/state-message/state-message';
import { MAX_COURSES_PER_STUDENT, Student } from '../data/student';
import { StudentStore } from '../data/student-store';

/**
 * Shell for the student tabs. Tabs are child routes rendered through a tab nav bar, so switching
 * tabs is a navigation and the same CanDeactivate guard protects both tab changes and leaving.
 */
@Component({
  selector: 'app-student-detail-page',
  imports: [
    AsyncPipe,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatTooltipModule,
    StateMessage,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './student-detail-page.html',
  styleUrl: './student-detail-page.scss',
})
export class StudentDetailPage {
  private readonly store = inject(StudentStore);
  private readonly router = inject(Router);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input.required<string>();

  protected readonly maxCourses = MAX_COURSES_PER_STUDENT;
  protected readonly tabs = [
    { path: 'profile', label: 'Profile' },
    { path: 'courses', label: 'Courses' },
    { path: 'enroll', label: 'Enroll' },
  ];
  protected readonly student$ = this.store.selected$;
  protected readonly status$ = this.store.selectedStatus$;
  protected readonly error$ = this.store.selectedError$;
  protected readonly enrollments$ = this.store.enrollments$;
  protected readonly totalCredits$ = this.store.totalCredits$;

  constructor() {
    toObservable(this.id)
      .pipe(takeUntilDestroyed())
      .subscribe((id) => this.store.open(Number(id)));
  }

  protected retry(): void {
    this.store.open(Number(this.id()));
  }

  protected onDelete(student: Student): void {
    const name = `${student.firstName} ${student.lastName}`;
    this.confirmDialog
      .confirm({
        title: `Delete ${name}?`,
        message: 'The student and all of their enrollments will be permanently removed.',
        confirmLabel: 'Delete',
        destructive: true,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.store.remove(student.id).pipe(ignoreReportedErrors())),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.notifications.success(`${name} deleted`);
        this.router.navigate(['/students']);
      });
  }
}
