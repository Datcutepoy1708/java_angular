import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { Component } from '@angular/core';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { AdminLoginComponent } from './admin-login.component';
import { AuthService } from '../../../core/services/auth.service';
import { ApiResponse, AuthResponse } from '../../../core/models/auth.model';

@Component({ template: '' })
class DummyComponent {}

describe('AdminLoginComponent', () => {
  let component: AdminLoginComponent;
  let fixture: ComponentFixture<AdminLoginComponent>;
  let authService: AuthService;
  let router: Router;

  const mockAdminSuccessResponse: ApiResponse<AuthResponse> = {
    success: true,
    message: 'Admin login success',
    data: {
      accessToken: 'admin-token',
      refreshToken: 'admin-refresh',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: {
        userId: 99,
        fullName: 'Administrator',
        email: 'admin@complexus.com',
        status: 'ACTIVE',
        roles: ['ROLE_ADMIN'],
        permissions: ['MANAGE_PRODUCTS', 'MANAGE_ORDERS']
      }
    },
    timestamp: '2026-08-22T00:00:00Z'
  };

  const mockCustomerSuccessResponse: ApiResponse<AuthResponse> = {
    success: true,
    message: 'Customer login success',
    data: {
      accessToken: 'customer-token',
      refreshToken: 'customer-refresh',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: {
        userId: 10,
        fullName: 'Normal Customer',
        email: 'customer@complexus.com',
        status: 'ACTIVE',
        roles: ['ROLE_CUSTOMER'],
        permissions: []
      }
    },
    timestamp: '2026-08-22T00:00:00Z'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminLoginComponent],
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'auth/login', component: DummyComponent },
          { path: 'admin/dashboard', component: DummyComponent }
        ])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminLoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create admin login component', () => {
    expect(component).toBeTruthy();
    expect(component.loginForm.valid).toBe(false);
  });

  it('should submit admin login and navigate to dashboard for ROLE_ADMIN', () => {
    vi.spyOn(authService, 'adminLogin').mockReturnValue(of(mockAdminSuccessResponse));
    const navSpy = vi.spyOn(router, 'navigateByUrl');

    component.loginForm.setValue({
      email: 'admin@complexus.com',
      password: 'AdminPassword123'
    });

    component.onSubmit();

    expect(authService.adminLogin).toHaveBeenCalledWith({
      email: 'admin@complexus.com',
      password: 'AdminPassword123'
    });
    expect(navSpy).toHaveBeenCalledWith('/admin/dashboard');
  });

  it('should reject and logout if logged in user lacks admin roles', () => {
    vi.spyOn(authService, 'adminLogin').mockReturnValue(of(mockCustomerSuccessResponse));
    const logoutSpy = vi.spyOn(authService, 'logout');

    component.loginForm.setValue({
      email: 'customer@complexus.com',
      password: 'CustomerPassword123'
    });

    component.onSubmit();

    expect(logoutSpy).toHaveBeenCalled();
    expect(component.errorMessage()).toContain('không có quyền truy cập');
  });

  it('should handle 403 Forbidden error', () => {
    vi.spyOn(authService, 'adminLogin').mockReturnValue(
      throwError(() => ({
        status: 403,
        error: { message: 'Forbidden' }
      }))
    );

    component.loginForm.setValue({
      email: 'admin@complexus.com',
      password: 'WrongPassword'
    });

    component.onSubmit();

    expect(component.errorMessage()).toContain('không có quyền truy cập');
  });
});
