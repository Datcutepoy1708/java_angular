import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { SocialAuthService } from '../../../core/services/social-auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly socialAuthService = inject(SocialAuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly isLoading = signal(false);
  readonly isSocialLoading = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly showPassword = signal(false);

  readonly loginForm = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email]
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(6)]
    })
  });

  togglePassword(): void {
    this.showPassword.update((val) => !val);
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const { email, password } = this.loginForm.getRawValue();

    this.authService.login({ email, password }).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        if (response.success) {
          const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
          this.router.navigateByUrl(returnUrl);
        }
      },
      error: (error) => {
        this.isLoading.set(false);
        if (error.status === 429) {
          this.errorMessage.set(
            error.error?.message || 'Bạn đã đăng nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.'
          );
        } else if (error.status === 400 || error.status === 401) {
          this.errorMessage.set(
            error.error?.message || 'Email hoặc mật khẩu không chính xác.'
          );
        } else {
          this.errorMessage.set(
            error.error?.message || 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.'
          );
        }
      }
    });
  }

  isFieldInvalid(fieldName: 'email' | 'password'): boolean {
    const field = this.loginForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  async onGoogleLogin(): Promise<void> {
    if (this.isLoading() || this.isSocialLoading()) return;

    this.isSocialLoading.set('google');
    this.errorMessage.set(null);

    try {
      const idToken = await this.socialAuthService.signInWithGoogle();
      this.authService.loginWithGoogle(idToken).subscribe({
        next: (response) => {
          this.isSocialLoading.set(null);
          if (response.success) {
            const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
            this.router.navigateByUrl(returnUrl);
          }
        },
        error: (error) => {
          this.isSocialLoading.set(null);
          this.errorMessage.set(error.error?.message || 'Đăng nhập bằng Google thất bại. Vui lòng thử lại.');
        }
      });
    } catch (err: any) {
      this.isSocialLoading.set(null);
      this.errorMessage.set(err.message || 'Đăng nhập Google không thành công.');
    }
  }

  async onFacebookLogin(): Promise<void> {
    if (this.isLoading() || this.isSocialLoading()) return;

    this.isSocialLoading.set('facebook');
    this.errorMessage.set(null);

    try {
      const accessToken = await this.socialAuthService.signInWithFacebook();
      this.authService.loginWithFacebook(accessToken).subscribe({
        next: (response) => {
          this.isSocialLoading.set(null);
          if (response.success) {
            const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
            this.router.navigateByUrl(returnUrl);
          }
        },
        error: (error) => {
          this.isSocialLoading.set(null);
          this.errorMessage.set(error.error?.message || 'Đăng nhập bằng Facebook thất bại. Vui lòng thử lại.');
        }
      });
    } catch (err: any) {
      this.isSocialLoading.set(null);
      this.errorMessage.set(err.message || 'Đăng nhập Facebook không thành công.');
    }
  }
}
