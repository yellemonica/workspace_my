import { Injectable, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  Observable,
  Subject,
  catchError,
  forkJoin,
  map,
  of,
  switchMap,
  tap,
  throwError,
} from 'rxjs';
import { Page, PageQuery } from '../../../core/api/page';
import { describeHttpError } from '../../../core/api/problem-detail';
import { LoadStatus, Store } from '../../../core/state/store';
import { Enrollment, Student, StudentRequest } from './student';
import { StudentApi } from './student-api';

interface StudentState {
  query: PageQuery;
  page: Page<Student> | null;
  status: LoadStatus;
  error: string | null;
  selected: Student | null;
  selectedStatus: LoadStatus;
  selectedError: string | null;
  enrollments: Enrollment[];
}

type LoadResult<T> = { data: T } | { error: string };

function toResult<T>(source: Observable<T>): Observable<LoadResult<T>> {
  return source.pipe(
    map((data) => ({ data })),
    catchError((error) => of({ error: describeHttpError(error) })),
  );
}

@Injectable({ providedIn: 'root' })
export class StudentStore extends Store<StudentState> {
  private readonly api = inject(StudentApi);
  private readonly queries = new Subject<PageQuery>();
  private readonly selections = new Subject<number>();

  readonly query$ = this.select((s) => s.query);
  readonly students$ = this.select((s) => s.page?.content ?? []);
  readonly totalElements$ = this.select((s) => s.page?.totalElements ?? 0);
  readonly status$ = this.select((s) => s.status);
  readonly error$ = this.select((s) => s.error);

  readonly selected$ = this.select((s) => s.selected);
  readonly selectedStatus$ = this.select((s) => s.selectedStatus);
  readonly selectedError$ = this.select((s) => s.selectedError);
  readonly enrollments$ = this.select((s) => s.enrollments);
  readonly totalCredits$ = this.select((s) => s.enrollments.reduce((sum, e) => sum + e.credits, 0));

  constructor() {
    super({
      query: { search: '', page: 0, size: 10, sort: 'lastName,asc' },
      page: null,
      status: 'idle',
      error: null,
      selected: null,
      selectedStatus: 'idle',
      selectedError: null,
      enrollments: [],
    });

    this.queries
      .pipe(
        tap((query) => this.patch({ query, status: 'loading', error: null })),
        switchMap((query) => toResult(this.api.list(query))),
        takeUntilDestroyed(),
      )
      .subscribe((result) =>
        'data' in result
          ? this.patch({ page: result.data, status: 'loaded' })
          : this.patch({ status: 'error', error: result.error }),
      );

    this.selections
      .pipe(
        tap(() =>
          this.patch({
            selected: null,
            enrollments: [],
            selectedStatus: 'loading',
            selectedError: null,
          }),
        ),
        switchMap((id) =>
          toResult(forkJoin({ student: this.api.get(id), enrollments: this.api.enrollments(id) })),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((result) =>
        'data' in result
          ? this.patch({
              selected: result.data.student,
              enrollments: result.data.enrollments,
              selectedStatus: 'loaded',
            })
          : this.patch({ selectedStatus: 'error', selectedError: result.error }),
      );
  }

  load(changes: Partial<PageQuery> = {}): void {
    this.queries.next({ ...this.state.query, ...changes });
  }

  search(search: string): void {
    this.load({ search, page: 0 });
  }

  open(id: number): void {
    this.selections.next(id);
  }

  find(id: number): Observable<Student> {
    return this.api.get(id);
  }

  isEmailAvailable(email: string, excludeId: number | null): Observable<boolean> {
    return this.api.isEmailAvailable(email, excludeId ?? undefined);
  }

  create(request: StudentRequest): Observable<Student> {
    return this.api.create(request);
  }

  update(id: number, request: StudentRequest): Observable<Student> {
    return this.api.update(id, request).pipe(
      tap((student) => {
        if (this.state.selected?.id === id) {
          this.patch({ selected: student });
        }
      }),
    );
  }

  remove(id: number): Observable<void> {
    return this.api.delete(id).pipe(
      tap(() => {
        if (this.state.selected?.id === id) {
          this.patch({ selected: null, enrollments: [], selectedStatus: 'idle' });
        }
        this.load();
      }),
    );
  }

  enroll(courseId: number): Observable<Enrollment> {
    return this.withSelected((student) =>
      this.api
        .enroll(student.id, courseId)
        .pipe(
          tap((enrollment) => this.patch({ enrollments: [...this.state.enrollments, enrollment] })),
        ),
    );
  }

  unenroll(courseId: number): Observable<void> {
    return this.withSelected((student) =>
      this.api.unenroll(student.id, courseId).pipe(
        tap(() =>
          this.patch({
            enrollments: this.state.enrollments.filter((e) => e.courseId !== courseId),
          }),
        ),
      ),
    );
  }

  private withSelected<T>(action: (student: Student) => Observable<T>): Observable<T> {
    const student = this.state.selected;
    return student ? action(student) : throwError(() => new Error('No student selected'));
  }
}
