import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { signal } from '@angular/core';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';

describe('roleGuard', () => {
  let authServiceMock: Partial<AuthService>;
  let routerMock: { createUrlTree: ReturnType<typeof vi.fn> };

  const runGuard = (url: string, roles: string[] = ['ROLE_ADMIN', 'ROLE_STAFF']) => {
    const route = { data: { roles } } as unknown as ActivatedRouteSnapshot;
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
        hasAnyRole: (roles: string[]) => roles.includes('ROLE_ADMIN'),
      };
      TestBed.overrideProvider(AuthService, { useValue: authServiceMock });
    });

    it('should pass through to /admin/dashboard', () => {
      const result = runGuard('/admin/dashboard');
      expect(result).toBe(true);
    });

    it('should pass through to /admin/brands', () => {
      const result = runGuard('/admin/brands');
      expect(result).toBe(true);
    });
  });

  describe('user with ROLE_STAFF', () => {
    beforeEach(() => {
      authServiceMock = {
        isAuthenticated: signal(true) as any,
        hasAnyRole: (roles: string[]) => roles.includes('ROLE_STAFF'),
      };
      TestBed.overrideProvider(AuthService, { useValue: authServiceMock });
    });

    it('should pass through to /admin/dashboard (staff allowed)', () => {
      const result = runGuard('/admin/dashboard');
      expect(result).toBe(true);
    });
  });

  describe('user with ROLE_CUSTOMER', () => {
    beforeEach(() => {
      authServiceMock = {
        isAuthenticated: signal(true) as any,
        hasAnyRole: (_roles: string[]) => false,
      };
      TestBed.overrideProvider(AuthService, { useValue: authServiceMock });
    });

    it('should block and redirect to / when trying to access /admin/**', () => {
      runGuard('/admin/dashboard');
      expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/']);
    });

    it('should block access to /admin/brands', () => {
      runGuard('/admin/brands');
      expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/']);
    });
  });

  describe('unauthenticated user', () => {
    beforeEach(() => {
      authServiceMock = {
        isAuthenticated: signal(false) as any,
        hasAnyRole: (_roles: string[]) => false,
      };
      TestBed.overrideProvider(AuthService, { useValue: authServiceMock });
    });

    it('should redirect to /admin/login when accessing /admin/** while not logged in', () => {
      runGuard('/admin/dashboard');
      expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/admin/login']);
    });
  });
});
