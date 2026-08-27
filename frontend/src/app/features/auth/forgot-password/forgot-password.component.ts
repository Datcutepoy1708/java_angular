import { Component, inject, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

export type ForgotStep = 'email' | 'otp' | 'reset' | 'success';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  readonly currentStep = signal<ForgotStep>('email');
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly userEmail = signal<string>('');
  readonly resetToken = signal<string>('');
  readonly countdown = signal<number>(0);

  readonly showNewPassword = signal<boolean>(false);
  readonly showConfirmPassword = signal<boolean>(false);

  private countdownTimer: any = null;

  // Step 1 Form: Email
  readonly emailForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  // Step 2 Form: OTP
  readonly otpForm = this.fb.nonNullable.group({
    otp: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]]
  });

  // Step 3 Form: Reset Password
  readonly resetForm = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]]
  });

  ngOnDestroy(): void {
    this.stopCountdown();
  }

  // ── Step 1: Submit Email ──────────────────────────────────────────
  submitEmail(): void {
    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      return;
    }

    const email = this.emailForm.getRawValue().email.trim().toLowerCase();
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.forgotPassword(email).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.userEmail.set(email);
        this.currentStep.set('otp');
        this.startCountdown(60);
        this.successMessage.set(`Mã xác thực OTP đã được gửi đến email ${email}`);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể gửi mã OTP. Vui lòng kiểm tra lại địa chỉ email.');
      }
    });
  }

  // ── Step 2: Submit OTP ────────────────────────────────────────────
  submitOtp(): void {
    if (this.otpForm.invalid) {
      this.otpForm.markAllAsTouched();
      return;
    }

    const otp = this.otpForm.getRawValue().otp.trim();
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.verifyOtp(this.userEmail(), otp).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res.success && res.data?.resetToken) {
          this.resetToken.set(res.data.resetToken);
          this.currentStep.set('reset');
          this.stopCountdown();
        } else {
          this.errorMessage.set('Mã xác thực không hợp lệ.');
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Mã OTP không chính xác hoặc đã hết hạn.');
      }
    });
  }

  // Resend OTP
  resendOtp(): void {
    if (this.countdown() > 0 || this.isLoading()) return;

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.forgotPassword(this.userEmail()).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.startCountdown(60);
        this.successMessage.set('Mã xác thực mới đã được gửi lại vào email của bạn.');
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể gửi lại mã OTP. Vui lòng thử lại sau.');
      }
    });
  }

  // Back to Email Step
  backToEmail(): void {
    this.currentStep.set('email');
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.stopCountdown();
  }

  // ── Step 3: Submit Reset Password ─────────────────────────────────
  submitReset(): void {
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    const { newPassword, confirmPassword } = this.resetForm.getRawValue();
    if (newPassword !== confirmPassword) {
      this.errorMessage.set('Mật khẩu xác nhận không khớp.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.resetPassword({
      email: this.userEmail(),
      resetToken: this.resetToken(),
      newPassword,
      confirmPassword
    }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.currentStep.set('success');
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Đặt lại mật khẩu thất bại. Vui lòng thử lại.');
      }
    });
  }

  // ── Countdown Helpers ─────────────────────────────────────────────
  private startCountdown(seconds: number): void {
    this.stopCountdown();
    this.countdown.set(seconds);
    this.countdownTimer = setInterval(() => {
      const current = this.countdown();
      if (current <= 1) {
        this.stopCountdown();
      } else {
        this.countdown.set(current - 1);
      }
    }, 1000);
  }

  private stopCountdown(): void {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = null;
    }
    this.countdown.set(0);
  }
}
