import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StaffManageComponent } from './staff-manage.component';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { RoleService } from '../../../core/services/role.service';
import { AuthService } from '../../../core/services/auth.service';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { AdminUser, AdminUserPage } from '../../../core/models/admin-user.model';
import { UserSummary } from '../../../core/models/auth.model';
import { RoleDetail } from '../../../core/models/role.model';

describe('StaffManageComponent', () => {
  let component: StaffManageComponent;
  let fixture: ComponentFixture<StaffManageComponent>;
  let adminUserServiceMock: any;
  let roleServiceMock: any;
  let authServiceMock: any;

  const mockAdmin: AdminUser = {
    userId: 1,
    fullName: 'Super Admin',
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

  const mockStaff: AdminUser = {
    userId: 2,
    fullName: 'Nhân Viên Kho',
    email: 'staff@store.com',
    phone: '0909998877',
    avatarUrl: null,
    gender: 'male',
    birthDate: '1995-05-15',
    status: 'active',
    emailVerified: true,
    provider: 'local',
    roles: ['ROLE_STAFF', 'ROLE_WAREHOUSE_MANAGER'],
    createdAt: '2026-08-21T10:00:00',
    updatedAt: '2026-08-21T10:00:00',
    totalOrders: 0,
    totalSpend: 0
  };

  const mockPage: AdminUserPage = {
    content: [mockAdmin, mockStaff],
    pageNumber: 0,
    pageSize: 10,
    totalElements: 2,
    totalPages: 1,
    last: true
  };

  const mockRoles: RoleDetail[] = [
    { roleId: 1, roleName: 'ROLE_ADMIN', description: 'Quản trị viên', isSystemRole: true, userCount: 1, permissionCodes: [], createdAt: null },
    { roleId: 2, roleName: 'ROLE_STAFF', description: 'Nhân viên bán hàng', isSystemRole: true, userCount: 1, permissionCodes: [], createdAt: null },
    { roleId: 3, roleName: 'ROLE_WAREHOUSE_MANAGER', description: 'Trưởng kho', isSystemRole: false, userCount: 1, permissionCodes: [], createdAt: null },
    { roleId: 4, roleName: 'ROLE_CUSTOMER', description: 'Khách hàng', isSystemRole: true, userCount: 100, permissionCodes: [], createdAt: null }
  ];

  beforeEach(async () => {
    adminUserServiceMock = {
      getStaffPaginated: vi.fn().mockReturnValue(
        of({ success: true, message: 'OK', data: mockPage })
      ),
      createUser: vi.fn().mockReturnValue(
        of({ success: true, message: 'Created', data: { ...mockStaff, userId: 3, fullName: 'New Staff' } })
      ),
      updateUser: vi.fn().mockReturnValue(
        of({ success: true, message: 'Updated', data: { ...mockStaff, fullName: 'Staff Updated' } })
      ),
      updateUserStatus: vi.fn().mockReturnValue(
        of({ success: true, message: 'Updated', data: { ...mockStaff, status: 'banned' } })
      ),
      resetUserPassword: vi.fn().mockReturnValue(
        of({ success: true, message: 'Reset', data: null })
      ),
      deleteUser: vi.fn().mockReturnValue(
        of({ success: true, message: 'Deleted', data: null })
      )
    };

    roleServiceMock = {
      getAllRoles: vi.fn().mockReturnValue(
        of({ success: true, message: 'OK', data: mockRoles })
      )
    };

    const mockAuthUser: UserSummary = {
      userId: 1,
      fullName: 'Super Admin',
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
      imports: [StaffManageComponent],
      providers: [
        { provide: AdminUserService, useValue: adminUserServiceMock },
        { provide: RoleService, useValue: roleServiceMock },
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StaffManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load staff and roles on init (excluding customer role)', () => {
    expect(component).toBeTruthy();
    expect(adminUserServiceMock.getStaffPaginated).toHaveBeenCalled();
    expect(roleServiceMock.getAllRoles).toHaveBeenCalled();
    expect(component.staffList().length).toBe(2);
    expect(component.availableRoles().length).toBe(3); // excludes ROLE_CUSTOMER
    expect(component.totalAdminsCount()).toBe(1);
    expect(component.activeStaffCount()).toBe(2);
  });

  it('should open create modal with auto-generated password and default role', () => {
    component.openCreateModal();
    expect(component.isCreateModalOpen()).toBe(true);
    expect(component.createForm.get('password')?.value).toBeTruthy();
    expect(component.createForm.get('roles')?.value).toEqual(['ROLE_STAFF']);
  });

  it('should submit create staff when valid', () => {
    component.openCreateModal();
    component.createForm.patchValue({
      fullName: 'Nhân Viên Mới',
      email: 'newstaff@store.com',
      phone: '0988776655',
      password: 'Password#123',
      roles: ['ROLE_STAFF', 'ROLE_WAREHOUSE_MANAGER'],
      status: 'active'
    });

    component.submitCreate();

    expect(adminUserServiceMock.createUser).toHaveBeenCalled();
    expect(component.isCreateModalOpen()).toBe(false);
  });

  it('should open edit modal and patch values', () => {
    component.openEditModal(mockStaff);
    expect(component.isEditModalOpen()).toBe(true);
    expect(component.editForm.get('fullName')?.value).toBe('Nhân Viên Kho');
    expect(component.editForm.get('roles')?.value).toEqual(['ROLE_STAFF', 'ROLE_WAREHOUSE_MANAGER']);

    component.editForm.patchValue({ fullName: 'Nhân Viên Kho VIP' });
    component.submitEdit();

    expect(adminUserServiceMock.updateUser).toHaveBeenCalledWith(
      2,
      expect.objectContaining({ fullName: 'Nhân Viên Kho VIP' })
    );
    expect(component.isEditModalOpen()).toBe(false);
  });

  it('should detect self or last admin correctly', () => {
    // mockAdmin has userId = 1 which matches currentUserId (1)
    expect(component.isSelfOrLastAdmin(mockAdmin)).toBe(true);
    // mockStaff has userId = 2 which is different and not admin
    expect(component.isSelfOrLastAdmin(mockStaff)).toBe(false);
  });

  it('should delete staff when confirmed for non-admin/non-self user', () => {
    component.openConfirmDelete(mockStaff);
    expect(component.isConfirmDeleteOpen()).toBe(true);

    component.confirmDelete();

    expect(adminUserServiceMock.deleteUser).toHaveBeenCalledWith(2);
    expect(component.isConfirmDeleteOpen()).toBe(false);
  });
});
