import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Component } from '@angular/core';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { AuthService } from './auth.service';
import { ApiResponse, AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';
import { environment } from '../../../environments/environment';

@Component({ template: '' })
class DummyComponent {}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const mockAuthResponse: ApiResponse<AuthResponse> = {
    success: true,
    message: 'Đăng nhập thành công',
    data: {
      accessToken: 'mock-access-token',
      refreshToken: 'mock-refresh-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: {
        userId: 1,
        fullName: 'Nguyễn Văn A',
        email: 'user@example.com',
        status: 'ACTIVE',
        roles: ['ROLE_CUSTOMER'],
        permissions: ['READ_PRODUCT']
      }
    },
    timestamp: '2026-08-22T10:00:00Z'
  };

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'auth/login', component: DummyComponent },
          { path: '', component: DummyComponent }
        ])
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created and initial user should be null', () => {
    expect(service).toBeTruthy();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
  });

  it('should login successfully and update signal state', () => {
    const loginRequest: LoginRequest = {
      email: 'user@example.com',
      password: 'Password123'
    };

    service.login(loginRequest).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.accessToken).toBe('mock-access-token');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/v1/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush(mockAuthResponse);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.currentUser()?.email).toBe('user@example.com');
    expect(service.getAccessToken()).toBe('mock-access-token');
    expect(service.hasRole('ROLE_CUSTOMER')).toBe(true);
    expect(service.isAdmin()).toBe(false);
  });

  it('should register successfully and update state', () => {
    const regRequest: RegisterRequest = {
      fullName: 'Nguyễn Văn A',
      email: 'user@example.com',
      password: 'Password123'
    };

    service.register(regRequest).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.user.fullName).toBe('Nguyễn Văn A');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/v1/auth/register`);
    expect(req.request.method).toBe('POST');
    req.flush(mockAuthResponse);

    expect(service.isAuthenticated()).toBe(true);
  });

  it('should logout and clear state and storage', () => {
    service.login({ email: 'user@example.com', password: 'Password123' }).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/v1/auth/login`);
    req.flush(mockAuthResponse);

    expect(service.isAuthenticated()).toBe(true);

    service.logout();

    const logoutReq = httpMock.expectOne(`${environment.apiUrl}/api/v1/auth/logout`);
    expect(logoutReq.request.method).toBe('POST');
    logoutReq.flush({ success: true, message: 'Logged out' });

    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
    expect(service.getAccessToken()).toBeNull();
  });

  it('should loginWithGoogle successfully and update state', () => {
    service.loginWithGoogle('mock-id-token').subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.accessToken).toBe('mock-access-token');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/v1/auth/social/google`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ idToken: 'mock-id-token' });
    req.flush(mockAuthResponse);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.getAccessToken()).toBe('mock-access-token');
  });

  it('should loginWithFacebook successfully and update state', () => {
    service.loginWithFacebook('mock-fb-access-token').subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.accessToken).toBe('mock-access-token');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/v1/auth/social/facebook`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ accessToken: 'mock-fb-access-token' });
    req.flush(mockAuthResponse);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.getAccessToken()).toBe('mock-access-token');
  });
});
