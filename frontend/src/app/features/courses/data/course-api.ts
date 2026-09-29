import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_BASE_URL, BACKGROUND_REQUEST } from '../../../core/api/api-config';
import { Page, PageQuery, toHttpParams } from '../../../core/api/page';
import { Course, CourseRequest } from './course';

@Injectable({ providedIn: 'root' })
export class CourseApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${inject(API_BASE_URL)}/courses`;

  list(query: PageQuery): Observable<Page<Course>> {
    return this.http.get<Page<Course>>(this.url, { params: toHttpParams(query) });
  }

  get(id: number): Observable<Course> {
    return this.http.get<Course>(`${this.url}/${id}`);
  }

  create(request: CourseRequest): Observable<Course> {
    return this.http.post<Course>(this.url, request);
  }

  update(id: number, request: CourseRequest): Observable<Course> {
    return this.http.put<Course>(`${this.url}/${id}`, request);
  }

  delete(id: number, force: boolean): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`, { params: { force } });
  }

  isCodeAvailable(code: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('code', code);
    if (excludeId != null) {
      params = params.set('excludeId', excludeId);
    }
    return this.http
      .get<{ available: boolean }>(`${this.url}/code-available`, {
        params,
        context: new HttpContext().set(BACKGROUND_REQUEST, true),
      })
      .pipe(map((response) => response.available));
  }
}
