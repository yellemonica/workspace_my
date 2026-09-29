import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, filter, map, switchMap, take } from 'rxjs';
import { ignoreReportedErrors } from '../../../core/http/ignore-reported-errors';
import { NotificationService } from '../../../core/notification/notification-service';
import { ConfirmDialogService } from '../../../shared/confirm-dialog/confirm-dialog-service';
import { StateMessage } from '../../../shared/state-message/state-message';
import { Student } from '../data/student';
import { StudentStore } from '../data/student-store';

@Component({
  selector: 'app-student-list-page',
  imports: [
    AsyncPipe,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
    StateMessage,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './student-list-page.html',
  styleUrl: './student-list-page.scss',
})
export class StudentListPage implements OnInit {
  private readonly store = inject(StudentStore);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly columns = ['name', 'email', 'enrollmentDate', 'actions'];
  protected readonly students$ = this.store.students$;
  protected readonly status$ = this.store.status$;
  protected readonly error$ = this.store.error$;
  protected readonly total$ = this.store.totalElements$;
  protected readonly query$ = this.store.query$;
  protected readonly sort$ = this.query$.pipe(
    map((query) => {
      const [active, direction] = query.sort.split(',');
      return {
        active: active === 'lastName' ? 'name' : active,
        direction: direction as 'asc' | 'desc',
      };
    }),
  );
  protected readonly searchControl = inject(NonNullableFormBuilder).control('');

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

  protected onSort(sort: Sort): void {
    const property = sort.active === 'name' ? 'lastName' : sort.active;
    this.store.load({
      sort: sort.direction ? `${property},${sort.direction}` : 'lastName,asc',
      page: 0,
    });
  }

  protected onPage(event: PageEvent): void {
    this.store.load({ page: event.pageIndex, size: event.pageSize });
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
      .subscribe(() => this.notifications.success(`${name} deleted`));
  }
}
