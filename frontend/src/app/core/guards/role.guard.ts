import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const expectedRoles = (route.data?.['roles'] as string[]) ?? [];

  if (!authService.isAuthenticated()) {
    if (state.url.startsWith('/admin')) {
      return router.createUrlTree(['/admin/login']);
    }
    return router.createUrlTree(['/auth/login']);
  }

  if (expectedRoles.length === 0 || authService.hasAnyRole(expectedRoles)) {
    return true;
  }

  // User does not have sufficient role -> redirect to customer home
  return router.createUrlTree(['/']);
};
