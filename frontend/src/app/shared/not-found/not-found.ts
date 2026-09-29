import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { StateMessage } from '../state-message/state-message';

@Component({
  selector: 'app-not-found',
  imports: [StateMessage, MatButtonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-state-message
      icon="explore_off"
      title="Page not found"
      message="The page you requested does not exist."
    >
      <a mat-flat-button routerLink="/students">Go to students</a>
    </app-state-message>
  `,
})
export default class NotFound {}
