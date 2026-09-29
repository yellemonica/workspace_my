import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { NotificationService } from '../../../core/notification/notification-service';
import { HasUnsavedChanges } from '../../../shared/guards/unsaved-changes-guard';
import { StateMessage } from '../../../shared/state-message/state-message';
import { Student, StudentRequest } from '../data/student';
import { StudentStore } from '../data/student-store';
import { StudentForm } from '../student-form/student-form';

@Component({
  selector: 'app-student-editor-page',
  imports: [RouterLink, MatButtonModule, StudentForm, StateMessage],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="page-header">
      <h1>{{ studentId() == null ? 'New student' : 'Edit student' }}</h1>
    </header>
    @if (loadFailed()) {
      <app-state-message kind="error" title="Student could not be loaded">
        <a mat-stroked-button routerLink="/students">Back to students</a>
      </app-state-message>
    } @else {
      <app-student-form
        [student]="student()"
        [saving]="saving()"
        [submitLabel]="studentId() == null ? 'Create' : 'Save'"
        (save)="save($event)"
      >
        <a
          mat-button
          formActions
          [routerLink]="studentId() == null ? '/students' : ['/students', studentId()]"
        >
          Cancel
        </a>
      </app-student-form>
    }
  `,
})
export class StudentEditorPage implements OnInit, HasUnsavedChanges {
  private readonly store = inject(StudentStore);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly form = viewChild(StudentForm);

  /** Bound from the `:id` route parameter; absent on /students/new. */
  readonly id = input<string>();

  protected readonly studentId = computed(() => (this.id() ? Number(this.id()) : null));
  protected readonly student = signal<Student | null>(null);
  protected readonly loadFailed = signal(false);
  protected readonly saving = signal(false);

  ngOnInit(): void {
    const id = this.studentId();
    if (id == null) {
      return;
    }
    this.store
      .find(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (student) => this.student.set(student),
        error: () => this.loadFailed.set(true),
      });
  }

  hasUnsavedChanges(): boolean {
    return !this.saving() && (this.form()?.hasUnsavedChanges() ?? false);
  }

  protected save(request: StudentRequest): void {
    const id = this.studentId();
    this.saving.set(true);
    (id == null ? this.store.create(request) : this.store.update(id, request))
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (student) => {
          this.form()?.markSaved();
          this.notifications.success(`${student.firstName} ${student.lastName} saved`);
          this.router.navigate(['/students', student.id]);
        },
        error: (error) => this.form()?.applyServerErrors(error),
      });
  }
}
