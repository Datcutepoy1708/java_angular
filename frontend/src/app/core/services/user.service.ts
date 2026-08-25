import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/auth.model';
import { ChangePasswordPayload, UpdateProfilePayload, UserProfile } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/users`;

  readonly userProfile = signal<UserProfile | null>(null);

  getMyProfile(): Observable<ApiResponse<UserProfile>> {
    return this.http.get<ApiResponse<UserProfile>>(`${this.baseUrl}/me`).pipe(
      tap(res => {
        if (res.data) {
          this.userProfile.set(res.data);
        }
      })
    );
  }

  updateMyProfile(payload: UpdateProfilePayload): Observable<ApiResponse<UserProfile>> {
    return this.http.put<ApiResponse<UserProfile>>(`${this.baseUrl}/me`, payload).pipe(
      tap(res => {
        if (res.data) {
          this.userProfile.set(res.data);
        }
      })
    );
  }

  changePassword(payload: ChangePasswordPayload): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.baseUrl}/me/password`, payload);
  }
}
