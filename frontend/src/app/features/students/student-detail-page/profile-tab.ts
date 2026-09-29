import { AsyncPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { NotificationService } from '../../../core/notification/notification-service';
import { HasUnsavedChanges } from '../../../shared/guards/unsaved-changes-guard';
import { Student, StudentRequest } from '../data/student';
import { StudentStore } from '../data/student-store';
import { StudentForm } from '../student-form/student-form';

@Component({
  selector: 'app-profile-tab',
  imports: [AsyncPipe, StudentForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (student$ | async; as student) {
      <app-student-form [student]="student" [saving]="saving()" (save)="save(student, $event)" />
    }
  `,
})
export class ProfileTab implements HasUnsavedChanges {
  private readonly store = inject(StudentStore);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly form = viewChild(StudentForm);

  protected readonly student$ = this.store.selected$;
  protected readonly saving = signal(false);

  hasUnsavedChanges(): boolean {
    return !this.saving() && (this.form()?.hasUnsavedChanges() ?? false);
  }

  protected save(student: Student, request: StudentRequest): void {
    this.saving.set(true);
    this.store
      .update(student.id, request)
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => this.notifications.success('Profile saved'),
        error: (error) => this.form()?.applyServerErrors(error),
      });
  }
}
