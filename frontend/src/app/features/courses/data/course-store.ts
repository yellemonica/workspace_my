import { Injectable, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, catchError, map, of, switchMap, tap } from 'rxjs';
import { Page, PageQuery } from '../../../core/api/page';
import { describeHttpError } from '../../../core/api/problem-detail';
import { LoadStatus, Store } from '../../../core/state/store';
import { Course, CourseRequest } from './course';
import { CourseApi } from './course-api';

/** The API caps page size at 100, which comfortably covers the enrollable catalog of this app. */
const OPTIONS_QUERY: PageQuery = { search: '', page: 0, size: 100, sort: 'code' };

interface CourseState {
  query: PageQuery;
  page: Page<Course> | null;
  status: LoadStatus;
  error: string | null;
  options: Course[];
}

type LoadResult<T> = { data: T } | { error: string };

@Injectable({ providedIn: 'root' })
export class CourseStore extends Store<CourseState> {
  private readonly api = inject(CourseApi);
  private readonly queries = new Subject<PageQuery>();

  readonly query$ = this.select((s) => s.query);
  readonly courses$ = this.select((s) => s.page?.content ?? []);
  readonly totalElements$ = this.select((s) => s.page?.totalElements ?? 0);
  readonly status$ = this.select((s) => s.status);
  readonly error$ = this.select((s) => s.error);
  readonly options$ = this.select((s) => s.options);

  constructor() {
    super({
      query: { search: '', page: 0, size: 12, sort: 'code' },
      page: null,
      status: 'idle',
      error: null,
      options: [],
    });

    this.queries
      .pipe(
        tap((query) => this.patch({ query, status: 'loading', error: null })),
        switchMap((query) =>
          this.api.list(query).pipe(
            map((data): LoadResult<Page<Course>> => ({ data })),
            catchError((error) => of({ error: describeHttpError(error) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((result) =>
        'data' in result
          ? this.patch({ page: result.data, status: 'loaded' })
          : this.patch({ status: 'error', error: result.error }),
      );
  }

  load(changes: Partial<PageQuery> = {}): void {
    this.queries.next({ ...this.state.query, ...changes });
  }

  search(search: string): void {
    this.load({ search, page: 0 });
  }

  loadOptions(): Observable<Course[]> {
    return this.api.list(OPTIONS_QUERY).pipe(
      map((page) => page.content),
      tap((options) => this.patch({ options })),
    );
  }

  find(id: number): Observable<Course> {
    return this.api.get(id);
  }

  isCodeAvailable(code: string, excludeId: number | null): Observable<boolean> {
    return this.api.isCodeAvailable(code, excludeId ?? undefined);
  }

  create(request: CourseRequest): Observable<Course> {
    return this.api.create(request);
  }

  update(id: number, request: CourseRequest): Observable<Course> {
    return this.api.update(id, request);
  }

  remove(course: Course): Observable<void> {
    return this.api.delete(course.id, course.enrolledCount > 0).pipe(tap(() => this.load()));
  }
}
