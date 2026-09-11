import { HttpInterceptorFn } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

export const loadingCount$ = new BehaviorSubject<number>(0);
export const isLoading$ = new BehaviorSubject<boolean>(false);

export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  updateLoading(1);
  return next(req).pipe(finalize(() => updateLoading(-1)));
};

function updateLoading(delta: number): void {
  const current = loadingCount$.value + delta;
  loadingCount$.next(current);
  isLoading$.next(current > 0);
}