import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';
import { ConfirmDialog, ConfirmDialogData } from './confirm-dialog';

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  private readonly dialog = inject(MatDialog);

  /** Emits once: true when confirmed, false when cancelled or dismissed. */
  confirm(data: ConfirmDialogData): Observable<boolean> {
    return this.dialog
      .open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, { data, width: '28rem' })
      .afterClosed()
      .pipe(map((confirmed) => confirmed === true));
  }

  confirmDiscardChanges(): Observable<boolean> {
    return this.confirm({
      title: 'Discard unsaved changes?',
      message: 'You have changes that have not been saved. Leaving now will discard them.',
      confirmLabel: 'Discard',
      cancelLabel: 'Keep editing',
      destructive: true,
    });
  }
}
