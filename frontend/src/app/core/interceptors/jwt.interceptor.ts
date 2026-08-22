import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();

  // Do not attach token for public auth endpoints (except me and logout)
  const isAuthEndpoint = req.url.includes('/api/v1/auth/login') ||
                         req.url.includes('/api/v1/auth/register') ||
                         req.url.includes('/api/v1/auth/refresh-token');

  if (token && !isAuthEndpoint) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req);
};
