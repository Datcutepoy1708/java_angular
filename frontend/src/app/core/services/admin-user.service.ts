import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminUser,
  AdminUserCreateRequest,
  AdminUserPage,
  AdminUserPasswordResetRequest,
  AdminUserStatusRequest,
  AdminUserUpdateRequest,
  Role
} from '../models/admin-user.model';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1/admin/users`;

  getUsersPaginated(
    page = 0,
    size = 10,
    keyword = '',
    role = '',
    status = '',
    sortBy = 'createdAt',
    sortDir = 'desc'
  ): Observable<ApiResponse<AdminUserPage>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }
    if (role.trim() && role !== 'all') {
      params = params.set('role', role.trim());
    }
    if (status.trim() && status !== 'all') {
      params = params.set('status', status.trim());
    }

    return this.http.get<ApiResponse<AdminUserPage>>(this.base, { params });
  }

  getCustomersPaginated(
    page = 0,
    size = 10,
    keyword = '',
    status = '',
    sortBy = 'createdAt',
    sortDir = 'desc'
  ): Observable<ApiResponse<AdminUserPage>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }
    if (status.trim() && status !== 'all') {
      params = params.set('status', status.trim());
    }

    return this.http.get<ApiResponse<AdminUserPage>>(`${this.base}/customers`, { params });
  }

  getStaffPaginated(
    page = 0,
    size = 10,
    keyword = '',
    role = '',
    status = '',
    sortBy = 'createdAt',
    sortDir = 'desc'
  ): Observable<ApiResponse<AdminUserPage>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }
    if (role.trim() && role !== 'all') {
      params = params.set('role', role.trim());
    }
    if (status.trim() && status !== 'all') {
      params = params.set('status', status.trim());
    }

    return this.http.get<ApiResponse<AdminUserPage>>(`${this.base}/staff`, { params });
  }

  getAllRoles(): Observable<ApiResponse<Role[]>> {
    return this.http.get<ApiResponse<Role[]>>(`${this.base}/roles`);
  }

  getUserById(id: number): Observable<ApiResponse<AdminUser>> {
    return this.http.get<ApiResponse<AdminUser>>(`${this.base}/${id}`);
  }

  createUser(request: AdminUserCreateRequest): Observable<ApiResponse<AdminUser>> {
    return this.http.post<ApiResponse<AdminUser>>(this.base, request);
  }

  updateUser(id: number, request: AdminUserUpdateRequest): Observable<ApiResponse<AdminUser>> {
    return this.http.put<ApiResponse<AdminUser>>(`${this.base}/${id}`, request);
  }

  updateUserStatus(id: number, request: AdminUserStatusRequest): Observable<ApiResponse<AdminUser>> {
    return this.http.patch<ApiResponse<AdminUser>>(`${this.base}/${id}/status`, request);
  }

  resetUserPassword(id: number, request: AdminUserPasswordResetRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.base}/${id}/reset-password`, request);
  }

  deleteUser(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`);
  }
}
