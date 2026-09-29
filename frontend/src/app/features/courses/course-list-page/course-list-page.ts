import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, filter, switchMap, take } from 'rxjs';
import { ignoreReportedErrors } from '../../../core/http/ignore-reported-errors';
import { NotificationService } from '../../../core/notification/notification-service';
import { ConfirmDialogService } from '../../../shared/confirm-dialog/confirm-dialog-service';
import { StateMessage } from '../../../shared/state-message/state-message';
import { CourseCard } from '../course-card/course-card';
import { Course } from '../data/course';
import { CourseStore } from '../data/course-store';

@Component({
  selector: 'app-course-list-page',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    CourseCard,
    StateMessage,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './course-list-page.html',
  styleUrl: './course-list-page.scss',
})
export class CourseListPage implements OnInit {
  private readonly store = inject(CourseStore);
  private readonly router = inject(Router);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly notifications = inject(NotificationService);

  protected readonly courses$ = this.store.courses$;
  protected readonly status$ = this.store.status$;
  protected readonly error$ = this.store.error$;
  protected readonly query$ = this.store.query$;
  protected readonly total$ = this.store.totalElements$;
  protected readonly searchControl = inject(NonNullableFormBuilder).control('');

  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.query$
      .pipe(take(1))
      .subscribe((q) => this.searchControl.setValue(q.search, { emitEvent: false }));
    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((term) => this.store.search(term));
  }

  ngOnInit(): void {
    this.store.load();
  }

  protected reload(): void {
    this.store.load();
  }

  protected onPage(event: PageEvent): void {
    this.store.load({ page: event.pageIndex, size: event.pageSize });
  }

  protected onEdit(course: Course): void {
    this.router.navigate(['/courses', course.id, 'edit']);
  }

  protected onDelete(course: Course): void {
    const enrolled = course.enrolledCount;
    this.confirmDialog
      .confirm({
        title: `Delete ${course.code}?`,
        message:
          enrolled > 0
            ? `${enrolled} student(s) are enrolled in "${course.title}". Deleting it will also remove those enrollments.`
            : `"${course.title}" will be permanently deleted.`,
        confirmLabel: 'Delete',
        destructive: true,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.store.remove(course).pipe(ignoreReportedErrors())),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.notifications.success(`${course.code} deleted`));
  }
}
