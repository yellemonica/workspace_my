import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** Empty / error placeholder for lists and pages; actions are projected as content. */
@Component({
  selector: 'app-state-message',
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-icon [class.error]="kind() === 'error'">{{
      kind() === 'error' ? 'error_outline' : icon()
    }}</mat-icon>
    <h3>{{ title() }}</h3>
    @if (message()) {
      <p>{{ message() }}</p>
    }
    <ng-content />
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
      padding: 3rem 1rem;
      text-align: center;
      color: var(--mat-sys-on-surface-variant);
    }
    mat-icon {
      width: 48px;
      height: 48px;
      font-size: 48px;
    }
    mat-icon.error {
      color: var(--mat-sys-error);
    }
    h3,
    p {
      margin: 0;
    }
  `,
})
export class StateMessage {
  readonly kind = input<'empty' | 'error'>('empty');
  readonly icon = input('inbox');
  readonly title = input.required<string>();
  readonly message = input<string | null>(null);
}
