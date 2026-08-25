import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AdminUserService } from './admin-user.service';
import { AdminUserCreateRequest } from '../models/admin-user.model';
import { environment } from '../../../environments/environment';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

describe('AdminUserService', () => {
  let service: AdminUserService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1/admin/users`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AdminUserService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AdminUserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getUsersPaginated should send query params correctly', () => {
    service.getUsersPaginated(0, 10, 'admin', 'ROLE_ADMIN', 'active').subscribe(res => {
      expect(res.data.content.length).toBe(0);
    });

    const req = httpMock.expectOne(r =>
      r.url === baseUrl &&
      r.params.get('keyword') === 'admin' &&
      r.params.get('role') === 'ROLE_ADMIN' &&
      r.params.get('status') === 'active'
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      success: true,
      message: 'OK',
      data: { content: [], pageNumber: 0, pageSize: 10, totalElements: 0, totalPages: 0, last: true }
    });
  });

  it('getAllRoles should return roles list', () => {
    service.getAllRoles().subscribe(res => {
      expect(res.data.length).toBe(1);
    });

    const req = httpMock.expectOne(`${baseUrl}/roles`);
    expect(req.request.method).toBe('GET');
    req.flush({
      success: true,
      message: 'OK',
      data: [{ roleId: 1, roleName: 'ROLE_ADMIN', description: 'Admin' }]
    });
  });

  it('createUser should post user data', () => {
    const reqData: AdminUserCreateRequest = {
      fullName: 'New Staff',
      email: 'staff@store.com',
      password: 'password123',
      roles: ['ROLE_STAFF']
    };

    service.createUser(reqData).subscribe(res => {
      expect(res.data.fullName).toBe('New Staff');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(reqData);
    req.flush({
      success: true,
      message: 'Created',
      data: {
        userId: 10,
        fullName: 'New Staff',
        email: 'staff@store.com',
        phone: null,
        avatarUrl: null,
        gender: null,
        birthDate: null,
        status: 'active',
        emailVerified: true,
        provider: 'local',
        roles: ['ROLE_STAFF'],
        createdAt: null,
        updatedAt: null,
        totalOrders: 0,
        totalSpend: 0
      }
    });
  });

  it('updateUserStatus should patch status', () => {
    service.updateUserStatus(5, { status: 'banned' }).subscribe(res => {
      expect(res.data.status).toBe('banned');
    });

    const req = httpMock.expectOne(`${baseUrl}/5/status`);
    expect(req.request.method).toBe('PATCH');
    req.flush({
      success: true,
      message: 'Updated',
      data: { userId: 5, status: 'banned' } as any
    });
  });

  it('resetUserPassword should post new password', () => {
    service.resetUserPassword(5, { newPassword: 'NewPassword123' }).subscribe(res => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/5/reset-password`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, message: 'Reset OK', data: null });
  });

  it('deleteUser should send DELETE request', () => {
    service.deleteUser(5).subscribe(res => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/5`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, message: 'Deleted', data: null });
  });
});
