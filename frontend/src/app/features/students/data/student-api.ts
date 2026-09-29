import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_BASE_URL, BACKGROUND_REQUEST } from '../../../core/api/api-config';
import { Page, PageQuery, toHttpParams } from '../../../core/api/page';
import { Enrollment, Student, StudentRequest } from './student';

@Injectable({ providedIn: 'root' })
export class StudentApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${inject(API_BASE_URL)}/students`;

  list(query: PageQuery): Observable<Page<Student>> {
    return this.http.get<Page<Student>>(this.url, { params: toHttpParams(query) });
  }

  get(id: number): Observable<Student> {
    return this.http.get<Student>(`${this.url}/${id}`);
  }

  create(request: StudentRequest): Observable<Student> {
    return this.http.post<Student>(this.url, request);
  }

  update(id: number, request: StudentRequest): Observable<Student> {
    return this.http.put<Student>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  isEmailAvailable(email: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('email', email);
    if (excludeId != null) {
      params = params.set('excludeId', excludeId);
    }
    return this.http
      .get<{ available: boolean }>(`${this.url}/email-available`, {
        params,
        context: new HttpContext().set(BACKGROUND_REQUEST, true),
      })
      .pipe(map((response) => response.available));
  }

  enrollments(studentId: number): Observable<Enrollment[]> {
    return this.http.get<Enrollment[]>(`${this.url}/${studentId}/enrollments`);
  }

  enroll(studentId: number, courseId: number): Observable<Enrollment> {
    return this.http.post<Enrollment>(`${this.url}/${studentId}/enrollments`, { courseId });
  }

  unenroll(studentId: number, courseId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${studentId}/enrollments/${courseId}`);
  }
}
