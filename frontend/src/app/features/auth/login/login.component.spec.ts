import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { SocialAuthService } from '../../../core/services/social-auth.service';
import { ApiResponse, AuthResponse } from '../../../core/models/auth.model';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: AuthService;

  const mockSuccessResponse: ApiResponse<AuthResponse> = {
    success: true,
    message: 'Success',
    data: {
      accessToken: 'token',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: {
        userId: 1,
        fullName: 'Test User',
        email: 'test@example.com',
        status: 'ACTIVE',
        roles: ['ROLE_CUSTOMER'],
        permissions: []
      }
    },
    timestamp: '2026-08-22T00:00:00Z'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
    fixture.detectChanges();
  });

  it('should create login component', () => {
    expect(component).toBeTruthy();
    expect(component.loginForm).toBeDefined();
    expect(component.loginForm.valid).toBe(false);
  });

  it('should validate email format', () => {
    const emailControl = component.loginForm.get('email');
    emailControl?.setValue('invalid-email');
    expect(emailControl?.valid).toBe(false);
    expect(emailControl?.hasError('email')).toBe(true);

    emailControl?.setValue('valid@example.com');
    expect(emailControl?.valid).toBe(true);
  });

  it('should toggle password visibility', () => {
    expect(component.showPassword()).toBe(false);
    component.togglePassword();
    expect(component.showPassword()).toBe(true);
    component.togglePassword();
    expect(component.showPassword()).toBe(false);
  });

  it('should submit login on valid form', () => {
    vi.spyOn(authService, 'login').mockReturnValue(of(mockSuccessResponse));

    component.loginForm.setValue({
      email: 'test@example.com',
      password: 'Password123'
    });

    component.onSubmit();

    expect(authService.login).toHaveBeenCalledWith({
      email: 'test@example.com',
      password: 'Password123'
    });
    expect(component.errorMessage()).toBeNull();
  });

  it('should set error message when login fails with 401', () => {
    vi.spyOn(authService, 'login').mockReturnValue(
      throwError(() => ({
        status: 401,
        error: { message: 'Email hoặc mật khẩu không chính xác.' }
      }))
    );

    component.loginForm.setValue({
      email: 'wrong@example.com',
      password: 'WrongPassword'
    });

    component.onSubmit();

    expect(component.errorMessage()).toBe('Email hoặc mật khẩu không chính xác.');
    expect(component.isLoading()).toBe(false);
  });

  it('should handle rate limit 429 error correctly', () => {
    vi.spyOn(authService, 'login').mockReturnValue(
      throwError(() => ({
        status: 429,
        error: { message: 'Too many requests' }
      }))
    );

    component.loginForm.setValue({
      email: 'ratelimited@example.com',
      password: 'Password123'
    });

    component.onSubmit();

    expect(component.errorMessage()).toBe('Too many requests');
  });

  it('should handle onGoogleLogin error gracefully', async () => {
    const socialAuth = TestBed.inject(SocialAuthService);
    vi.spyOn(socialAuth, 'signInWithGoogle').mockRejectedValue(new Error('Google login failed'));
    await component.onGoogleLogin();
    expect(component.errorMessage()).toBe('Google login failed');
    expect(component.isSocialLoading()).toBeNull();
  });

  it('should handle onFacebookLogin error gracefully', async () => {
    const socialAuth = TestBed.inject(SocialAuthService);
    vi.spyOn(socialAuth, 'signInWithFacebook').mockRejectedValue(new Error('Facebook login failed'));
    await component.onFacebookLogin();
    expect(component.errorMessage()).toBe('Facebook login failed');
    expect(component.isSocialLoading()).toBeNull();
  });
});
