import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401 Unauthorized handling (token expired)
      if (error.status === 401 && !req.url.includes('/api/v1/auth/')) {
        // If the user has no refresh token, they are an unauthenticated guest.
        // Never call logout(), which forces guests to be redirected to the login page!
        const refreshToken = authService.getRefreshToken();
        if (!refreshToken) {
          return throwError(() => error);
        }

        return authService.refreshToken().pipe(
          switchMap((refreshResponse) => {
            if (refreshResponse.success && refreshResponse.data) {
              const retryReq = req.clone({
                setHeaders: {
                  Authorization: `Bearer ${refreshResponse.data.accessToken}`
                }
              });
              return next(retryReq);
            }
            authService.logout();
            return throwError(() => error);
          }),
          catchError((refreshErr) => {
            authService.logout();
            return throwError(() => refreshErr);
          })
        );
      }

      return throwError(() => error);
    })
  );
};
