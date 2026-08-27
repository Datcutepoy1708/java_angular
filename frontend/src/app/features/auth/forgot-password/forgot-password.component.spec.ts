import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ForgotPasswordComponent } from './forgot-password.component';
import { AuthService } from '../../../core/services/auth.service';

describe('ForgotPasswordComponent', () => {
  let component: ForgotPasswordComponent;
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let authService: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent],
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
    fixture.detectChanges();
  });

  it('should create component with initial email step', () => {
    expect(component).toBeTruthy();
    expect(component.currentStep()).toBe('email');
    expect(component.emailForm.valid).toBe(false);
  });

  it('should validate email format in step 1', () => {
    const emailControl = component.emailForm.get('email');
    emailControl?.setValue('not-an-email');
    expect(emailControl?.valid).toBe(false);

    emailControl?.setValue('valid@example.com');
    expect(emailControl?.valid).toBe(true);
  });

  it('should advance to otp step on successful email submit', () => {
    vi.spyOn(authService, 'forgotPassword').mockReturnValue(
      of({ success: true, message: 'OTP sent', data: undefined as any, timestamp: '' })
    );

    component.emailForm.setValue({ email: 'user@store.com' });
    component.submitEmail();

    expect(component.currentStep()).toBe('otp');
    expect(component.userEmail()).toBe('user@store.com');
    expect(component.countdown()).toBe(60);
  });

  it('should advance to reset step on valid OTP submission', () => {
    component.userEmail.set('user@store.com');
    component.currentStep.set('otp');

    vi.spyOn(authService, 'verifyOtp').mockReturnValue(
      of({
        success: true,
        message: 'OTP verified',
        data: { resetToken: 'mock-uuid-token', email: 'user@store.com', expiresInSeconds: 600 },
        timestamp: ''
      })
    );

    component.otpForm.setValue({ otp: '123456' });
    component.submitOtp();

    expect(component.currentStep()).toBe('reset');
    expect(component.resetToken()).toBe('mock-uuid-token');
  });

  it('should show error when passwords do not match in step 3', () => {
    component.currentStep.set('reset');
    component.resetForm.setValue({
      newPassword: 'password123',
      confirmPassword: 'differentPassword'
    });

    component.submitReset();

    expect(component.errorMessage()).toBe('Mật khẩu xác nhận không khớp.');
  });

  it('should advance to success step on successful reset', () => {
    component.currentStep.set('reset');
    component.userEmail.set('user@store.com');
    component.resetToken.set('mock-uuid-token');

    vi.spyOn(authService, 'resetPassword').mockReturnValue(
      of({ success: true, message: 'Password reset', data: undefined as any, timestamp: '' })
    );

    component.resetForm.setValue({
      newPassword: 'newPassword123',
      confirmPassword: 'newPassword123'
    });

    component.submitReset();

    expect(component.currentStep()).toBe('success');
  });
});
