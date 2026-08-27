import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { signal } from '@angular/core';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';

describe('roleGuard', () => {
  let authServiceMock: Partial<AuthService>;
  let routerMock: { createUrlTree: ReturnType<typeof vi.fn> };

  const runGuard = (url: string, roles: string[] = ['ROLE_ADMIN', 'ROLE_STAFF'], permissions: string[] = []) => {
    const route = { data: { roles, permissions } } as unknown as ActivatedRouteSnapshot;
    const state = { url } as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => roleGuard(route, state));
  };

  beforeEach(() => {
    routerMock = {
      createUrlTree: vi.fn((commands: unknown[]) => commands)
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: routerMock },
      ]
    });
  });

  describe('user with ROLE_ADMIN', () => {
    beforeEach(() => {
      authServiceMock = {
        isAuthenticated: signal(true) as any,
        isAdmin: signal(true) as any,
        hasAnyRole: (_roles: string[]) => true,
        hasAnyPermission: (_perms: string[]) => true,
      };
      TestBed.overrideProvider(AuthService, { useValue: authServiceMock });
    });

    it('should pass through to /admin/dashboard', () => {
      const result = runGuard('/admin/dashboard');
      expect(result).toBe(true);
    });

    it('should pass through to any admin page regardless of permissions', () => {
      const result = runGuard('/admin/staff', ['ROLE_ADMIN'], ['STAFF_VIEW']);
      expect(result).toBe(true);
    });
  });

  describe('user with ROLE_STAFF', () => {
    beforeEach(() => {
      authServiceMock = {
        isAuthenticated: signal(true) as any,
        isAdmin: signal(false) as any,
        hasAnyRole: (roles: string[]) => roles.includes('ROLE_STAFF'),
        hasAnyPermission: (perms: string[]) => perms.includes('PRODUCT_VIEW'),
      };
      TestBed.overrideProvider(AuthService, { useValue: authServiceMock });
    });

    it('should pass through to /admin/dashboard (staff role allowed)', () => {
      const result = runGuard('/admin/dashboard', ['ROLE_ADMIN', 'ROLE_STAFF']);
      expect(result).toBe(true);
    });

    it('should pass through when staff has required permission', () => {
      const result = runGuard('/admin/products', ['ROLE_ADMIN', 'ROLE_STAFF'], ['PRODUCT_VIEW']);
      expect(result).toBe(true);
    });

    it('should redirect to /admin/forbidden when staff lacks required permission', () => {
      runGuard('/admin/staff', ['ROLE_ADMIN'], ['STAFF_VIEW']);
      expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/admin/forbidden']);
    });
  });

  describe('user with ROLE_CUSTOMER', () => {
    beforeEach(() => {
      authServiceMock = {
        isAuthenticated: signal(true) as any,
        isAdmin: signal(false) as any,
        hasAnyRole: (_roles: string[]) => false,
        hasAnyPermission: (_perms: string[]) => false,
      };
      TestBed.overrideProvider(AuthService, { useValue: authServiceMock });
    });

    it('should block and redirect to /admin/forbidden when trying to access /admin/**', () => {
      runGuard('/admin/dashboard');
      expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/admin/forbidden']);
    });

    it('should redirect to / when trying to access non-admin restricted routes', () => {
      runGuard('/special-area', ['ROLE_STAFF']);
      expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/']);
    });
  });

  describe('unauthenticated user', () => {
    beforeEach(() => {
      authServiceMock = {
        isAuthenticated: signal(false) as any,
        isAdmin: signal(false) as any,
        hasAnyRole: (_roles: string[]) => false,
        hasAnyPermission: (_perms: string[]) => false,
      };
      TestBed.overrideProvider(AuthService, { useValue: authServiceMock });
    });

    it('should redirect to /admin/login when accessing /admin/** while not logged in', () => {
      runGuard('/admin/dashboard');
      expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/admin/login']);
    });
  });
});
