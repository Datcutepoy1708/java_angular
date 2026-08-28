import { computed, inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, of } from 'rxjs';
import { ApiResponse, AuthResponse, LoginRequest, RegisterRequest, UserSummary } from '../models/auth.model';
import { environment } from '../../../environments/environment';

const ACCESS_TOKEN_KEY = 'complexus_access_token';
const REFRESH_TOKEN_KEY = 'complexus_refresh_token';
const USER_KEY = 'complexus_user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);

  private readonly baseUrl = `${environment.apiUrl}/api/v1/auth`;

  // Signals for Reactive State Management
  readonly currentUser = signal<UserSummary | null>(this.loadInitialUser());
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
  readonly userRoles = computed(() => this.currentUser()?.roles ?? []);
  readonly userPermissions = computed(() => this.currentUser()?.permissions ?? []);
  readonly isAdmin = computed(() => this.userRoles().includes('ROLE_ADMIN'));
  readonly isStaff = computed(() => this.userRoles().includes('ROLE_STAFF'));

  login(request: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/login`, request).pipe(
      tap((response) => {
        if (response.success && response.data) {
          this.handleAuthSuccess(response.data);
        }
      })
    );
  }

  adminLogin(request: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/admin/login`, request).pipe(
      tap((response) => {
        if (response.success && response.data) {
          this.handleAuthSuccess(response.data);
        }
      })
    );
  }

  register(request: RegisterRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/register`, request).pipe(
      tap((response) => {
        if (response.success && response.data) {
          this.handleAuthSuccess(response.data);
        }
      })
    );
  }

  loginWithGoogle(idToken: string): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/social/google`, { idToken }).pipe(
      tap((response) => {
        if (response.success && response.data) {
          this.handleAuthSuccess(response.data);
        }
      })
    );
  }

  loginWithFacebook(accessToken: string): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/social/facebook`, { accessToken }).pipe(
      tap((response) => {
        if (response.success && response.data) {
          this.handleAuthSuccess(response.data);
        }
      })
    );
  }

  loginWithZalo(code: string, codeVerifier: string): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/social/zalo`, { code, codeVerifier }).pipe(
      tap((response) => {
        if (response.success && response.data) {
          this.handleAuthSuccess(response.data);
        }
      })
    );
  }

  refreshToken(): Observable<ApiResponse<AuthResponse>> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      // Do not call logout() here — that would redirect mid-navigation
      this.clearStorage();
      this.currentUser.set(null);
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/refresh-token`, { refreshToken }).pipe(
      tap((response) => {
        if (response.success && response.data) {
          this.handleAuthSuccess(response.data);
        }
      }),
      catchError((error) => {
        this.logout();
        return throwError(() => error);
      })
    );
  }

  getProfile(): Observable<ApiResponse<UserSummary>> {
    return this.http.get<ApiResponse<UserSummary>>(`${this.baseUrl}/me`).pipe(
      tap((response) => {
        if (response.success && response.data) {
          this.currentUser.set(response.data);
          this.saveStorage(USER_KEY, JSON.stringify(response.data));
        }
      })
    );
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      this.http.post(`${this.baseUrl}/logout`, { refreshToken }).subscribe({
        next: () => {},
        error: () => {}
      });
    }

    this.clearStorage();
    this.currentUser.set(null);

    // Redirect to admin login when on admin pages, otherwise customer login
    const currentUrl = this.router.url ?? '';
    if (currentUrl.startsWith('/admin')) {
      this.router.navigate(['/admin/login']);
    } else {
      this.router.navigate(['/auth/login']);
    }
  }

  getAccessToken(): string | null {
    return this.getStorage(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return this.getStorage(REFRESH_TOKEN_KEY);
  }

  hasRole(role: string): boolean {
    return this.userRoles().includes(role);
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some((role) => this.userRoles().includes(role));
  }

  hasPermission(permission: string): boolean {
    if (this.isAdmin()) {
      return true;
    }
    return this.userPermissions().includes(permission);
  }

  hasAnyPermission(permissions: string[]): boolean {
    if (this.isAdmin()) {
      return true;
    }
    return permissions.some((permission) => this.userPermissions().includes(permission));
  }

  forgotPassword(email: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/forgot-password`, { email });
  }

  verifyOtp(email: string, otp: string): Observable<ApiResponse<{ resetToken: string; email: string; expiresInSeconds: number }>> {
    return this.http.post<ApiResponse<{ resetToken: string; email: string; expiresInSeconds: number }>>(`${this.baseUrl}/verify-otp`, { email, otp });
  }

  resetPassword(data: { email: string; resetToken: string; newPassword: string; confirmPassword: string }): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/reset-password`, data);
  }

  private handleAuthSuccess(authData: AuthResponse): void {
    this.saveStorage(ACCESS_TOKEN_KEY, authData.accessToken);
    this.saveStorage(REFRESH_TOKEN_KEY, authData.refreshToken);
    this.saveStorage(USER_KEY, JSON.stringify(authData.user));
    this.currentUser.set(authData.user);
  }

  private loadInitialUser(): UserSummary | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }
    const userJson = this.getStorage(USER_KEY);
    if (!userJson) {
      return null;
    }
    try {
      return JSON.parse(userJson) as UserSummary;
    } catch {
      this.clearStorage();
      return null;
    }
  }

  private getStorage(key: string): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem(key);
    }
    return null;
  }

  private saveStorage(key: string, value: string): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(key, value);
    }
  }

  private clearStorage(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
  }
}
