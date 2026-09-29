import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { ignoreReportedErrors } from '../../../core/http/ignore-reported-errors';
import { NotificationService } from '../../../core/notification/notification-service';
import { ConfirmDialogService } from '../../../shared/confirm-dialog/confirm-dialog-service';
import { StateMessage } from '../../../shared/state-message/state-message';
import { Enrollment } from '../data/student';
import { StudentStore } from '../data/student-store';

@Component({
  selector: 'app-courses-tab',
  imports: [
    AsyncPipe,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatTooltipModule,
    StateMessage,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let enrollments = enrollments$ | async;
    @if (enrollments?.length) {
      <mat-list>
        @for (enrollment of enrollments; track enrollment.courseId) {
          <mat-list-item>
            <span matListItemTitle>{{ enrollment.code }} · {{ enrollment.title }}</span>
            <span matListItemLine>
              {{ enrollment.credits }} credits · enrolled
              {{ enrollment.enrolledAt | date: 'mediumDate' }}
            </span>
            <button
              mat-icon-button
              matListItemMeta
              (click)="unenroll(enrollment)"
              matTooltip="Unenroll"
              [attr.aria-label]="'Unenroll from ' + enrollment.code"
            >
              <mat-icon>remove_circle_outline</mat-icon>
            </button>
          </mat-list-item>
        }
      </mat-list>
    } @else {
      <app-state-message icon="menu_book" title="Not enrolled in any courses">
        <a mat-stroked-button routerLink="../enroll">Enroll in a course</a>
      </app-state-message>
    }
  `,
})
export class CoursesTab {
  private readonly store = inject(StudentStore);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly enrollments$ = this.store.enrollments$;

  protected unenroll(enrollment: Enrollment): void {
    this.confirmDialog
      .confirm({
        title: `Unenroll from ${enrollment.code}?`,
        message: `The student will be removed from "${enrollment.title}".`,
        confirmLabel: 'Unenroll',
        destructive: true,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.store.unenroll(enrollment.courseId).pipe(ignoreReportedErrors())),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.notifications.success(`Unenrolled from ${enrollment.code}`));
  }
}
