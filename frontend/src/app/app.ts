import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LoadingService } from './core/loading/loading-service';

@Component({
  selector: 'app-root',
  imports: [
    AsyncPipe,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-toolbar>
      <mat-icon aria-hidden="true">school</mat-icon>
      <span class="brand">Student Management</span>
      <nav>
        <a mat-button routerLink="/students" routerLinkActive="active">Students</a>
        <a mat-button routerLink="/courses" routerLinkActive="active">Courses</a>
      </nav>
    </mat-toolbar>
    <div class="progress">
      @if (loading$ | async) {
        <mat-progress-bar mode="indeterminate" aria-label="Loading" />
      }
    </div>
    <main>
      <router-outlet />
    </main>
  `,
  styles: `
    mat-toolbar {
      gap: 0.75rem;
      background: var(--mat-sys-primary-container);
      color: var(--mat-sys-on-primary-container);
    }
    .brand {
      margin-right: 1.5rem;
    }
    nav a.active {
      background: color-mix(in srgb, var(--mat-sys-primary) 14%, transparent);
    }
    .progress {
      height: 4px;
    }
    main {
      max-width: 1100px;
      margin: 0 auto;
      padding: 1.5rem 1rem 3rem;
    }
  `,
})
export class App {
  protected readonly loading$ = inject(LoadingService).loading$;
}
