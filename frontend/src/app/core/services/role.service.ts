import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  PermissionGroup,
  RoleCreateRequest,
  RoleDetail,
  RolePermissionsUpdateRequest,
  RoleUpdateRequest
} from '../models/role.model';
import { ApiResponse } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class RoleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/admin/roles`;
  private readonly permUrl = `${environment.apiUrl}/api/v1/admin/permissions`;

  getAllRoles(): Observable<ApiResponse<RoleDetail[]>> {
    return this.http.get<ApiResponse<RoleDetail[]>>(this.baseUrl);
  }

  getRoleById(roleId: number): Observable<ApiResponse<RoleDetail>> {
    return this.http.get<ApiResponse<RoleDetail>>(`${this.baseUrl}/${roleId}`);
  }

  createRole(request: RoleCreateRequest): Observable<ApiResponse<RoleDetail>> {
    return this.http.post<ApiResponse<RoleDetail>>(this.baseUrl, request);
  }

  updateRole(roleId: number, request: RoleUpdateRequest): Observable<ApiResponse<RoleDetail>> {
    return this.http.put<ApiResponse<RoleDetail>>(`${this.baseUrl}/${roleId}`, request);
  }

  deleteRole(roleId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${roleId}`);
  }

  updateRolePermissions(
    roleId: number,
    request: RolePermissionsUpdateRequest
  ): Observable<ApiResponse<RoleDetail>> {
    return this.http.put<ApiResponse<RoleDetail>>(`${this.baseUrl}/${roleId}/permissions`, request);
  }

  getGroupedPermissions(): Observable<ApiResponse<PermissionGroup[]>> {
    return this.http.get<ApiResponse<PermissionGroup[]>>(this.permUrl);
  }
}
