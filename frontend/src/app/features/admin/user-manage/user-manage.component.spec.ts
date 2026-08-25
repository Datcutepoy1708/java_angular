import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UserManageComponent } from './user-manage.component';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { AuthService } from '../../../core/services/auth.service';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { AdminUser, AdminUserPage } from '../../../core/models/admin-user.model';
import { UserSummary } from '../../../core/models/auth.model';

describe('UserManageComponent', () => {
  let component: UserManageComponent;
  let fixture: ComponentFixture<UserManageComponent>;
  let adminUserServiceMock: any;
  let authServiceMock: any;

  const mockAdminUser: AdminUser = {
    userId: 1,
    fullName: 'Admin Test',
    email: 'admin@store.com',
    phone: '0901112233',
    avatarUrl: null,
    gender: 'male',
    birthDate: '1990-01-01',
    status: 'active',
    emailVerified: true,
    provider: 'local',
    roles: ['ROLE_ADMIN'],
    createdAt: '2026-08-20T10:00:00',
    updatedAt: '2026-08-20T10:00:00',
    totalOrders: 0,
    totalSpend: 0
  };

  const mockCustomerUser: AdminUser = {
    userId: 2,
    fullName: 'Khách Hàng',
    email: 'customer@gmail.com',
    phone: '0909998877',
    avatarUrl: null,
    gender: 'female',
    birthDate: '1995-05-15',
    status: 'active',
    emailVerified: true,
    provider: 'local',
    roles: ['ROLE_CUSTOMER'],
    createdAt: '2026-08-21T10:00:00',
    updatedAt: '2026-08-21T10:00:00',
    totalOrders: 3,
    totalSpend: 15000000
  };

  const mockPage: AdminUserPage = {
    content: [mockAdminUser, mockCustomerUser],
    pageNumber: 0,
    pageSize: 10,
    totalElements: 2,
    totalPages: 1,
    last: true
  };

  beforeEach(async () => {
    adminUserServiceMock = {
      getUsersPaginated: vi.fn().mockReturnValue(
        of({ success: true, message: 'OK', data: mockPage })
      ),
      getAllRoles: vi.fn().mockReturnValue(
        of({
          success: true,
          message: 'OK',
          data: [
            { roleId: 1, roleName: 'ROLE_ADMIN', description: 'Admin' },
            { roleId: 2, roleName: 'ROLE_STAFF', description: 'Staff' },
            { roleId: 3, roleName: 'ROLE_CUSTOMER', description: 'Customer' }
          ]
        })
      ),
      createUser: vi.fn().mockReturnValue(
        of({
          success: true,
          message: 'Created',
          data: { ...mockAdminUser, userId: 3, fullName: 'New Staff', email: 'staff@store.com', roles: ['ROLE_STAFF'] }
        })
      ),
      updateUser: vi.fn().mockReturnValue(
        of({ success: true, message: 'Updated', data: { ...mockCustomerUser, fullName: 'Khách Hàng VIP' } })
      ),
      updateUserStatus: vi.fn().mockReturnValue(
        of({ success: true, message: 'Updated', data: { ...mockCustomerUser, status: 'banned' } })
      ),
      resetUserPassword: vi.fn().mockReturnValue(
        of({ success: true, message: 'Reset', data: null })
      ),
      deleteUser: vi.fn().mockReturnValue(
        of({ success: true, message: 'Deleted', data: null })
      )
    };

    const mockAuthUser: UserSummary = {
      userId: 1,
      fullName: 'Admin Test',
      email: 'admin@store.com',
      phone: '0901112233',
      avatarUrl: undefined,
      status: 'active',
      roles: ['ROLE_ADMIN'],
      permissions: []
    };

    authServiceMock = {
      currentUser: signal<UserSummary | null>(mockAuthUser)
    };

    await TestBed.configureTestingModule({
      imports: [UserManageComponent],
      providers: [
        { provide: AdminUserService, useValue: adminUserServiceMock },
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load users & roles on init', () => {
    expect(component).toBeTruthy();
    expect(adminUserServiceMock.getUsersPaginated).toHaveBeenCalled();
    expect(adminUserServiceMock.getAllRoles).toHaveBeenCalled();
    expect(component.users().length).toBe(2);
    expect(component.staffAdminCount()).toBe(1);
    expect(component.activeCustomersCount()).toBe(1);
  });

  it('should open create modal with auto-generated password', () => {
    component.openCreateModal();
    expect(component.isCreateModalOpen()).toBe(true);
    expect(component.createForm.get('password')?.value).toBeTruthy();
    expect(component.createForm.get('roles')?.value).toEqual(['ROLE_STAFF']);
  });

  it('should open edit modal and patch values', () => {
    component.openEditModal(mockCustomerUser);
    expect(component.isEditModalOpen()).toBe(true);
    expect(component.selectedUser()?.userId).toBe(2);
    expect(component.editForm.get('fullName')?.value).toBe('Khách Hàng');
  });

  it('should create new staff user when form is valid', () => {
    component.openCreateModal();
    component.createForm.patchValue({
      fullName: 'New Staff',
      email: 'staff@store.com',
      password: 'Password#123',
      roles: ['ROLE_STAFF'],
      status: 'active'
    });

    component.submitCreate();

    expect(adminUserServiceMock.createUser).toHaveBeenCalled();
    expect(component.isCreateModalOpen()).toBe(false);
  });

  it('should update user and roles when edit is submitted', () => {
    component.openEditModal(mockCustomerUser);
    component.editForm.patchValue({
      fullName: 'Khách Hàng VIP',
      roles: ['ROLE_CUSTOMER', 'ROLE_STAFF']
    });

    component.submitEdit();

    expect(adminUserServiceMock.updateUser).toHaveBeenCalledWith(
      2,
      expect.objectContaining({
        fullName: 'Khách Hàng VIP',
        roles: ['ROLE_CUSTOMER', 'ROLE_STAFF']
      })
    );
    expect(component.isEditModalOpen()).toBe(false);
  });

  it('should toggle user status', () => {
    component.toggleUserStatus(mockCustomerUser);
    expect(adminUserServiceMock.updateUserStatus).toHaveBeenCalledWith(2, { status: 'banned' });
  });

  it('should reset user password', () => {
    component.openResetPasswordModal(mockCustomerUser);
    expect(component.isResetPasswordModalOpen()).toBe(true);
    component.resetPasswordForm.patchValue({ newPassword: 'NewPassword@999' });

    component.submitResetPassword();

    expect(adminUserServiceMock.resetUserPassword).toHaveBeenCalledWith(2, { newPassword: 'NewPassword@999' });
    expect(component.isResetPasswordModalOpen()).toBe(false);
  });

  it('should delete user when confirmed', () => {
    component.openConfirmDelete(mockCustomerUser);
    expect(component.isConfirmDeleteOpen()).toBe(true);

    component.confirmDelete();

    expect(adminUserServiceMock.deleteUser).toHaveBeenCalledWith(2);
    expect(component.isConfirmDeleteOpen()).toBe(false);
  });
});
