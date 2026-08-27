import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const expectedRoles = (route.data?.['roles'] as string[]) ?? [];
  const expectedPermissions = (route.data?.['permissions'] as string[]) ?? [];

  if (!authService.isAuthenticated()) {
    if (state.url.startsWith('/admin')) {
      return router.createUrlTree(['/admin/login']);
    }
    return router.createUrlTree(['/auth/login']);
  }

  // Admin has full access to all roles and permissions
  if (authService.isAdmin()) {
    return true;
  }

  // Check roles requirement
  if (expectedRoles.length > 0 && !authService.hasAnyRole(expectedRoles)) {
    if (state.url.startsWith('/admin')) {
      return router.createUrlTree(['/admin/forbidden']);
    }
    return router.createUrlTree(['/']);
  }

  // Check permissions requirement
  if (expectedPermissions.length > 0 && !authService.hasAnyPermission(expectedPermissions)) {
    if (state.url.startsWith('/admin')) {
      return router.createUrlTree(['/admin/forbidden']);
    }
    return router.createUrlTree(['/']);
  }

  return true;
};
