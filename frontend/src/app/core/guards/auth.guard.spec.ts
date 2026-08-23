import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { signal } from '@angular/core';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let authServiceMock: Partial<AuthService>;
  let routerMock: { createUrlTree: ReturnType<typeof vi.fn>; url: string };
  let mockRoute: ActivatedRouteSnapshot;

  const runGuard = (url: string) => {
    const state = { url } as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => authGuard(mockRoute, state));
  };

  beforeEach(() => {
    authServiceMock = {
      isAuthenticated: signal(false) as any,
    };

    routerMock = {
      createUrlTree: vi.fn((commands: unknown[]) => commands),
      url: '/'
    };

    mockRoute = {} as ActivatedRouteSnapshot;

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
      ]
    });
  });

  describe('when not authenticated', () => {
    beforeEach(() => {
      (authServiceMock as any).isAuthenticated = signal(false);
    });

    it('should redirect to /admin/login when navigating to /admin/dashboard', () => {
      runGuard('/admin/dashboard');
      expect(routerMock.createUrlTree).toHaveBeenCalledWith(
        ['/admin/login'],
        expect.any(Object)
      );
    });

    it('should redirect to /auth/login when navigating to a non-admin route', () => {
      runGuard('/products');
      expect(routerMock.createUrlTree).toHaveBeenCalledWith(
        ['/auth/login'],
        expect.any(Object)
      );
    });

    it('should redirect to /admin/login when navigating to /admin/brands', () => {
      runGuard('/admin/brands');
      expect(routerMock.createUrlTree).toHaveBeenCalledWith(
        ['/admin/login'],
        expect.any(Object)
      );
    });
  });

  describe('when authenticated', () => {
    beforeEach(() => {
      (authServiceMock as any).isAuthenticated = signal(true);
    });

    it('should pass through and return true', () => {
      const result = runGuard('/admin/dashboard');
      expect(result).toBe(true);
    });

    it('should not redirect when authenticated', () => {
      runGuard('/admin/products');
      expect(routerMock.createUrlTree).not.toHaveBeenCalled();
    });
  });
});
