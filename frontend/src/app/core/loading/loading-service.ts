import { Injectable } from '@angular/core';
import { BehaviorSubject, distinctUntilChanged, map, of, switchMap, timer } from 'rxjs';

const SHOW_DELAY_MS = 150;

@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly pending = new BehaviorSubject(0);

  /** Emits true only when requests stay in flight past a short delay, so fast responses don't flicker. */
  readonly loading$ = this.pending.pipe(
    map((count) => count > 0),
    distinctUntilChanged(),
    switchMap((loading) => (loading ? timer(SHOW_DELAY_MS).pipe(map(() => true)) : of(false))),
  );

  start(): void {
    this.pending.next(this.pending.getValue() + 1);
  }

  stop(): void {
    this.pending.next(Math.max(0, this.pending.getValue() - 1));
  }
}
