import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Course } from '../data/course';

@Component({
  selector: 'app-course-card',
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatChipsModule, MatTooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let c = course();
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-subtitle>{{ c.code }}</mat-card-subtitle>
        <mat-card-title>{{ c.title }}</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <p class="description">{{ c.description || 'No description' }}</p>
        <mat-chip-set aria-label="Course facts">
          <mat-chip disableRipple>{{ c.credits }} credit{{ c.credits === 1 ? '' : 's' }}</mat-chip>
          <mat-chip disableRipple>{{ c.enrolledCount }} enrolled</mat-chip>
        </mat-chip-set>
      </mat-card-content>
      <mat-card-actions align="end">
        <button
          mat-icon-button
          (click)="edit.emit(c)"
          matTooltip="Edit"
          [attr.aria-label]="'Edit ' + c.code"
        >
          <mat-icon>edit</mat-icon>
        </button>
        <button
          mat-icon-button
          (click)="delete.emit(c)"
          matTooltip="Delete"
          [attr.aria-label]="'Delete ' + c.code"
        >
          <mat-icon>delete</mat-icon>
        </button>
      </mat-card-actions>
    </mat-card>
  `,
  styles: `
    mat-card {
      height: 100%;
    }
    mat-card-content {
      flex: 1;
    }
    .description {
      color: var(--mat-sys-on-surface-variant);
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
      min-height: 3.6em;
    }
  `,
})
export class CourseCard {
  readonly course = input.required<Course>();
  readonly edit = output<Course>();
  readonly delete = output<Course>();
}
