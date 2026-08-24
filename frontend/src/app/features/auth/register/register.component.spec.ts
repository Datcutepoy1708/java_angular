import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../../core/services/auth.service';
import { ApiResponse, AuthResponse } from '../../../core/models/auth.model';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
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
        fullName: 'Nguyễn Văn B',
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
      imports: [RegisterComponent],
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
    fixture.detectChanges();
  });

  it('should create register component', () => {
    expect(component).toBeTruthy();
    expect(component.registerForm.valid).toBe(false);
  });

  it('should calculate password strength correctly', () => {
    component.registerForm.get('password')?.setValue('pass');
    expect(component.passwordStrength()).toBe(0);

    component.registerForm.get('password')?.setValue('password123');
    expect(component.passwordStrength()).toBe(2);
    expect(component.strengthLabel()).toBe('Trung bình');

    component.registerForm.get('password')?.setValue('Password123!');
    expect(component.passwordStrength()).toBe(4);
    expect(component.strengthLabel()).toBe('Mạnh');
  });

  it('should validate password mismatch', () => {
    component.registerForm.patchValue({
      password: 'Password123!',
      confirmPassword: 'DifferentPassword123!'
    });

    expect(component.registerForm.hasError('passwordMismatch')).toBe(true);

    component.registerForm.patchValue({
      confirmPassword: 'Password123!'
    });

    expect(component.registerForm.hasError('passwordMismatch')).toBe(false);
  });

  it('should submit register on valid form', () => {
    vi.spyOn(authService, 'register').mockReturnValue(of(mockSuccessResponse));

    component.registerForm.setValue({
      fullName: 'Nguyễn Văn B',
      email: 'user@example.com',
      phone: '0987654321',
      password: 'Password123!',
      confirmPassword: 'Password123!',
      agreeTerms: true
    });

    component.onSubmit();

    expect(authService.register).toHaveBeenCalledWith({
      fullName: 'Nguyễn Văn B',
      email: 'user@example.com',
      phone: '0987654321',
      password: 'Password123!'
    });
    expect(component.successMessage()).toContain('thành công');
  });

  it('should show error when email already exists', () => {
    vi.spyOn(authService, 'register').mockReturnValue(
      throwError(() => ({
        status: 409,
        error: { message: 'Email đã tồn tại' }
      }))
    );

    component.registerForm.setValue({
      fullName: 'Nguyễn Văn B',
      email: 'existing@example.com',
      phone: '',
      password: 'Password123!',
      confirmPassword: 'Password123!',
      agreeTerms: true
    });

    component.onSubmit();

    expect(component.errorMessage()).toContain('đã được đăng ký');
  });

  it('should invalidate passwords with Vietnamese diacritics or accents', () => {
    const pwdControl = component.registerForm.get('password');
    pwdControl?.setValue('Mậtkhẩu123!');
    expect(pwdControl?.hasError('pattern')).toBe(true);

    pwdControl?.setValue('Password123!');
    expect(pwdControl?.hasError('pattern')).toBe(false);
  });
});
