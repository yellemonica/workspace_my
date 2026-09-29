import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable, firstValueFrom, of } from 'rxjs';
import { ConfirmDialogService } from '../confirm-dialog/confirm-dialog-service';
import { HasUnsavedChanges, unsavedChangesGuard } from './unsaved-changes-guard';

describe('unsavedChangesGuard', () => {
  const confirmDiscardChanges = vi.fn<() => Observable<boolean>>();

  beforeEach(() => {
    confirmDiscardChanges.mockReset();
    TestBed.configureTestingModule({
      providers: [{ provide: ConfirmDialogService, useValue: { confirmDiscardChanges } }],
    });
  });

  function run(component: HasUnsavedChanges) {
    return TestBed.runInInjectionContext(() =>
      unsavedChangesGuard(
        component,
        {} as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot,
        {} as RouterStateSnapshot,
      ),
    );
  }

  it('allows navigation without asking when nothing changed', () => {
    expect(run({ hasUnsavedChanges: () => false })).toBe(true);
    expect(confirmDiscardChanges).not.toHaveBeenCalled();
  });

  it.each([true, false])(
    'defers to the confirm dialog when there are changes (confirmed: %s)',
    async (confirmed) => {
      confirmDiscardChanges.mockReturnValue(of(confirmed));

      const result = run({ hasUnsavedChanges: () => true });

      expect(await firstValueFrom(result as Observable<boolean>)).toBe(confirmed);
    },
  );
});
