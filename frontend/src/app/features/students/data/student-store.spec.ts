import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Enrollment, Student } from './student';
import { StudentStore } from './student-store';

const API = 'http://localhost:8080/api/students';
const ADA: Student = {
  id: 1,
  firstName: 'Ada',
  lastName: 'Lovelace',
  email: 'ada@x.edu',
  dateOfBirth: '2000-01-01',
  enrollmentDate: '2024-09-01',
};
const enrollment = (courseId: number, credits = 3): Enrollment => ({
  courseId,
  code: `CS${courseId}`,
  title: `Course ${courseId}`,
  credits,
  enrolledAt: '2026-09-01T00:00:00Z',
});

describe('StudentStore', () => {
  let store: StudentStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(StudentStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function openAda(enrollments: Enrollment[]) {
    store.open(1);
    http.expectOne(`${API}/1`).flush(ADA);
    http.expectOne(`${API}/1/enrollments`).flush(enrollments);
  }

  it('loads the selected student together with their enrollments', async () => {
    openAda([enrollment(101, 4)]);

    expect(await firstValueFrom(store.selected$)).toEqual(ADA);
    expect(await firstValueFrom(store.selectedStatus$)).toBe('loaded');
    expect(await firstValueFrom(store.totalCredits$)).toBe(4);
  });

  it('publishes a new enrollment to every subscriber as soon as the server confirms it', async () => {
    openAda([enrollment(101)]);
    const seen: number[][] = [];
    store.enrollments$.subscribe((list) => seen.push(list.map((e) => e.courseId)));

    store.enroll(102).subscribe();
    const request = http.expectOne(`${API}/1/enrollments`);
    expect(request.request.body).toEqual({ courseId: 102 });
    request.flush(enrollment(102));

    expect(seen).toEqual([[101], [101, 102]]);
  });

  it('leaves enrollments untouched when the server rejects the enrollment', async () => {
    openAda([enrollment(101)]);

    store.enroll(102).subscribe({ error: () => undefined });
    http
      .expectOne(`${API}/1/enrollments`)
      .flush(
        { status: 422, title: 'Business rule violated' },
        { status: 422, statusText: 'Unprocessable' },
      );

    expect((await firstValueFrom(store.enrollments$)).map((e) => e.courseId)).toEqual([101]);
  });

  it('removes an enrollment after a successful unenroll', async () => {
    openAda([enrollment(101), enrollment(102)]);

    store.unenroll(101).subscribe();
    http.expectOne({ method: 'DELETE', url: `${API}/1/enrollments/101` }).flush(null);

    expect((await firstValueFrom(store.enrollments$)).map((e) => e.courseId)).toEqual([102]);
  });

  it('drops a stale selection response when another student is opened', async () => {
    store.open(1);
    const staleStudent = http.expectOne(`${API}/1`);
    const staleEnrollments = http.expectOne(`${API}/1/enrollments`);

    store.open(2);

    expect(staleStudent.cancelled).toBe(true);
    expect(staleEnrollments.cancelled).toBe(true);
    http.expectOne(`${API}/2`).flush({ ...ADA, id: 2 });
    http.expectOne(`${API}/2/enrollments`).flush([]);
    expect((await firstValueFrom(store.selected$))?.id).toBe(2);
  });
});
