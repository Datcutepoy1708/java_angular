import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { RoleService } from './role.service';
import { RoleCreateRequest, RoleDetail, RolePermissionsUpdateRequest, RoleUpdateRequest } from '../models/role.model';
import { environment } from '../../../environments/environment';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

describe('RoleService', () => {
  let service: RoleService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1/admin/roles`;
  const permUrl = `${environment.apiUrl}/api/v1/admin/permissions`;

  const mockRole: RoleDetail = {
    roleId: 1,
    roleName: 'ROLE_ADMIN',
    description: 'Quản trị viên',
    isSystemRole: true,
    userCount: 2,
    permissionCodes: ['PRODUCT_VIEW', 'ROLE_MANAGE'],
    createdAt: '2026-08-20T10:00:00'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RoleService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(RoleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getAllRoles should return all roles', () => {
    service.getAllRoles().subscribe(res => {
      expect(res.data.length).toBe(1);
      expect(res.data[0].roleName).toBe('ROLE_ADMIN');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: [mockRole] });
  });

  it('getRoleById should return role detail', () => {
    service.getRoleById(1).subscribe(res => {
      expect(res.data.roleId).toBe(1);
    });

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockRole });
  });

  it('createRole should post new role data', () => {
    const request: RoleCreateRequest = {
      roleName: 'ROLE_ACCOUNTANT',
      description: 'Kế toán viên',
      permissionCodes: ['ORDER_VIEW']
    };

    service.createRole(request).subscribe(res => {
      expect(res.data.roleName).toBe('ROLE_ACCOUNTANT');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ success: true, message: 'Created', data: { ...mockRole, roleId: 5, roleName: 'ROLE_ACCOUNTANT' } });
  });

  it('updateRole should put updated role data', () => {
    const request: RoleUpdateRequest = { description: 'Updated Description' };

    service.updateRole(1, request).subscribe(res => {
      expect(res.data.description).toBe('Updated Description');
    });

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush({ success: true, message: 'Updated', data: { ...mockRole, description: 'Updated Description' } });
  });

  it('deleteRole should send DELETE request', () => {
    service.deleteRole(5).subscribe(res => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/5`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, message: 'Deleted', data: null });
  });

  it('updateRolePermissions should put new permission list', () => {
    const request: RolePermissionsUpdateRequest = { permissionCodes: ['PRODUCT_VIEW', 'PRODUCT_CREATE'] };

    service.updateRolePermissions(1, request).subscribe(res => {
      expect(res.data.permissionCodes).toEqual(['PRODUCT_VIEW', 'PRODUCT_CREATE']);
    });

    const req = httpMock.expectOne(`${baseUrl}/1/permissions`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush({ success: true, message: 'Updated', data: { ...mockRole, permissionCodes: ['PRODUCT_VIEW', 'PRODUCT_CREATE'] } });
  });

  it('getGroupedPermissions should return grouped permissions', () => {
    service.getGroupedPermissions().subscribe(res => {
      expect(res.data.length).toBe(1);
      expect(res.data[0].groupCode).toBe('PRODUCT');
    });

    const req = httpMock.expectOne(permUrl);
    expect(req.request.method).toBe('GET');
    req.flush({
      success: true,
      message: 'OK',
      data: [{ groupCode: 'PRODUCT', groupName: 'Sản Phẩm', permissions: [] }]
    });
  });
});
