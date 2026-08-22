import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-login',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-login.component.html',
  styleUrl: './admin-login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminLoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly isLoading = signal(false);
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

    this.authService.adminLogin({ email, password }).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        if (response.success && response.data) {
          const roles = response.data.user.roles || [];
          if (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_STAFF')) {
            const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/admin/dashboard';
            this.router.navigateByUrl(returnUrl);
          } else {
            this.authService.logout();
            this.errorMessage.set('Tài khoản này không có quyền truy cập trang quản trị.');
          }
        }
      },
      error: (error) => {
        this.isLoading.set(false);
        if (error.status === 403) {
          this.errorMessage.set('Tài khoản này không có quyền truy cập trang quản trị.');
        } else if (error.status === 429) {
          this.errorMessage.set(
            error.error?.message || 'Bạn đã thử đăng nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.'
          );
        } else if (error.status === 400 || error.status === 401) {
          this.errorMessage.set(
            error.error?.message || 'Email hoặc mật khẩu quản trị viên không chính xác.'
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
}
