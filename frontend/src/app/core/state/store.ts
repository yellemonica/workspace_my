import { BehaviorSubject, Observable, distinctUntilChanged, map } from 'rxjs';

export type LoadStatus = 'idle' | 'loading' | 'loaded' | 'error';

/**
 * Minimal observable store: state lives in a BehaviorSubject, is only replaced immutably via
 * `patch`, and is exposed to components exclusively through `select`ed observables.
 */
export abstract class Store<S extends object> {
  private readonly state$: BehaviorSubject<S>;

  protected constructor(initialState: S) {
    this.state$ = new BehaviorSubject(initialState);
  }

  protected get state(): S {
    return this.state$.getValue();
  }

  protected select<R>(project: (state: S) => R): Observable<R> {
    return this.state$.pipe(map(project), distinctUntilChanged());
  }

  protected patch(changes: Partial<S>): void {
    this.state$.next({ ...this.state, ...changes });
  }
}
