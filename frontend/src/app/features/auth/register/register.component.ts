import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { SocialAuthService } from '../../../core/services/social-auth.service';

export const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  if (password && confirmPassword && password !== confirmPassword) {
    return { passwordMismatch: true };
  }
  return null;
};

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RegisterComponent {
  private readonly authService = inject(AuthService);
  private readonly socialAuthService = inject(SocialAuthService);
  private readonly router = inject(Router);

  readonly isLoading = signal(false);
  readonly isSocialLoading = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly showPassword = signal(false);
  readonly showConfirmPassword = signal(false);

  readonly registerForm = new FormGroup(
    {
      fullName: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(2), Validators.maxLength(100)]
      }),
      email: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.email]
      }),
      phone: new FormControl('', {
        nonNullable: true,
        validators: [Validators.pattern(/^$|^(0[35789])[0-9]{8}$/)]
      }),
      password: new FormControl('', {
        nonNullable: true,
        validators: [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern(/^[\x21-\x7E]+$/)
        ]
      }),
      confirmPassword: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required]
      }),
      agreeTerms: new FormControl(false, {
        nonNullable: true,
        validators: [Validators.requiredTrue]
      })
    },
    { validators: passwordMatchValidator }
  );

  readonly passwordValue = signal('');

  constructor() {
    this.registerForm.get('password')?.valueChanges.subscribe((val) => {
      this.passwordValue.set(val || '');
    });
  }

  readonly passwordStrength = computed(() => {
    const val = this.passwordValue();
    if (!val) return 0;
    let score = 0;
    if (val.length >= 8) score++;
    if (/[A-Z]/.test(val) && /[a-z]/.test(val)) score++;
    if (/[0-9]/.test(val)) score++;
    if (/[^A-Za-z0-9]/.test(val)) score++;
    return score;
  });

  readonly strengthLabel = computed(() => {
    switch (this.passwordStrength()) {
      case 1:
        return 'Yếu';
      case 2:
        return 'Trung bình';
      case 3:
        return 'Khá';
      case 4:
        return 'Mạnh';
      default:
        return '';
    }
  });

  togglePassword(): void {
    this.showPassword.update((val) => !val);
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword.update((val) => !val);
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const { fullName, email, phone, password } = this.registerForm.getRawValue();
    const cleanPhone = phone && phone.trim() ? phone.trim() : undefined;

    this.authService.register({ fullName, email, phone: cleanPhone, password }).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        if (response.success) {
          this.successMessage.set('Đăng ký tài khoản thành công! Đang chuyển hướng...');
          setTimeout(() => {
            this.router.navigate(['/']);
          }, 1200);
        }
      },
      error: (error) => {
        this.isLoading.set(false);
        if (error.status === 409 || error.error?.message?.includes('tồn tại') || error.error?.message?.includes('already exists')) {
          this.errorMessage.set('Email này đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.');
        } else if (error.error?.data && typeof error.error.data === 'object') {
          const validationMsgs = Object.values(error.error.data).join('. ');
          this.errorMessage.set(validationMsgs || error.error?.message || 'Thông tin đăng ký không hợp lệ.');
        } else {
          this.errorMessage.set(
            error.error?.message || 'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin và thử lại.'
          );
        }
      }
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  isPasswordMismatch(): boolean {
    const confirmField = this.registerForm.get('confirmPassword');
    return !!(
      this.registerForm.hasError('passwordMismatch') &&
      (confirmField?.dirty || confirmField?.touched)
    );
  }

  async onGoogleSignup(): Promise<void> {
    if (this.isLoading() || this.isSocialLoading()) return;

    this.isSocialLoading.set('google');
    this.errorMessage.set(null);

    try {
      const idToken = await this.socialAuthService.signInWithGoogle();
      this.authService.loginWithGoogle(idToken).subscribe({
        next: (response) => {
          this.isSocialLoading.set(null);
          if (response.success) {
            this.router.navigateByUrl('/');
          }
        },
        error: (error) => {
          this.isSocialLoading.set(null);
          this.errorMessage.set(error.error?.message || 'Đăng ký bằng Google thất bại. Vui lòng thử lại.');
        }
      });
    } catch (err: any) {
      this.isSocialLoading.set(null);
      this.errorMessage.set(err.message || 'Đăng ký Google không thành công.');
    }
  }

  async onFacebookSignup(): Promise<void> {
    if (this.isLoading() || this.isSocialLoading()) return;

    this.isSocialLoading.set('facebook');
    this.errorMessage.set(null);

    try {
      const accessToken = await this.socialAuthService.signInWithFacebook();
      this.authService.loginWithFacebook(accessToken).subscribe({
        next: (response) => {
          this.isSocialLoading.set(null);
          if (response.success) {
            this.router.navigateByUrl('/');
          }
        },
        error: (error) => {
          this.isSocialLoading.set(null);
          this.errorMessage.set(error.error?.message || 'Đăng ký bằng Facebook thất bại. Vui lòng thử lại.');
        }
      });
    } catch (err: any) {
      this.isSocialLoading.set(null);
      this.errorMessage.set(err.message || 'Đăng ký Facebook không thành công.');
    }
  }

  async onZaloSignup(): Promise<void> {
    if (this.isLoading() || this.isSocialLoading()) return;

    this.isSocialLoading.set('zalo');
    this.errorMessage.set(null);

    try {
      const { code, codeVerifier } = await this.socialAuthService.signInWithZalo();
      this.authService.loginWithZalo(code, codeVerifier).subscribe({
        next: (response) => {
          this.isSocialLoading.set(null);
          if (response.success) {
            this.router.navigateByUrl('/');
          }
        },
        error: (error) => {
          this.isSocialLoading.set(null);
          this.errorMessage.set(error.error?.message || 'Đăng ký bằng Zalo thất bại. Vui lòng thử lại.');
        }
      });
    } catch (err: any) {
      this.isSocialLoading.set(null);
      this.errorMessage.set(err.message || 'Đăng ký Zalo không thành công.');
    }
  }
}
