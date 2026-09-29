import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { BACKGROUND_REQUEST } from '../api/api-config';
import { LoadingService } from '../loading/loading-service';

export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.context.get(BACKGROUND_REQUEST)) {
    return next(req);
  }
  const loading = inject(LoadingService);
  loading.start();
  return next(req).pipe(finalize(() => loading.stop()));
};
