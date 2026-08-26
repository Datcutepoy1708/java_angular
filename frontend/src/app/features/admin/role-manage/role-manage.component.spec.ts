import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RoleManageComponent } from './role-manage.component';
import { RoleService } from '../../../core/services/role.service';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { PermissionGroup, RoleDetail } from '../../../core/models/role.model';

describe('RoleManageComponent', () => {
  let component: RoleManageComponent;
  let fixture: ComponentFixture<RoleManageComponent>;
  let roleServiceMock: any;

  const mockRoles: RoleDetail[] = [
    { roleId: 1, roleName: 'ROLE_ADMIN', description: 'Quản trị viên', isSystemRole: true, userCount: 2, permissionCodes: ['ROLE_MANAGE', 'PRODUCT_VIEW'], createdAt: null },
    { roleId: 2, roleName: 'ROLE_STAFF', description: 'Nhân viên', isSystemRole: true, userCount: 5, permissionCodes: ['PRODUCT_VIEW'], createdAt: null },
    { roleId: 10, roleName: 'ROLE_WAREHOUSE_MANAGER', description: 'Trưởng kho', isSystemRole: false, userCount: 0, permissionCodes: ['INVENTORY_VIEW'], createdAt: null }
  ];

  const mockGroups: PermissionGroup[] = [
    {
      groupCode: 'PRODUCT',
      groupName: 'Quản lý Sản phẩm',
      permissions: [
        { permissionId: 1, permissionCode: 'PRODUCT_VIEW', description: 'Xem sản phẩm', moduleGroup: 'PRODUCT' },
        { permissionId: 2, permissionCode: 'PRODUCT_CREATE', description: 'Tạo sản phẩm', moduleGroup: 'PRODUCT' }
      ]
    },
    {
      groupCode: 'USER',
      groupName: 'Quản trị & Phân quyền',
      permissions: [
        { permissionId: 3, permissionCode: 'ROLE_MANAGE', description: 'Quản lý quyền', moduleGroup: 'USER' }
      ]
    }
  ];

  beforeEach(async () => {
    roleServiceMock = {
      getAllRoles: vi.fn().mockReturnValue(
        of({ success: true, message: 'OK', data: mockRoles })
      ),
      getGroupedPermissions: vi.fn().mockReturnValue(
        of({ success: true, message: 'OK', data: mockGroups })
      ),
      createRole: vi.fn().mockReturnValue(
        of({ success: true, message: 'Created', data: { ...mockRoles[2], roleId: 15, roleName: 'ROLE_ACCOUNTANT' } })
      ),
      updateRole: vi.fn().mockReturnValue(
        of({ success: true, message: 'Updated', data: { ...mockRoles[2], description: 'Trưởng kho mới' } })
      ),
      deleteRole: vi.fn().mockReturnValue(
        of({ success: true, message: 'Deleted', data: null })
      ),
      updateRolePermissions: vi.fn().mockReturnValue(
        of({ success: true, message: 'Updated', data: mockRoles[0] })
      )
    };

    await TestBed.configureTestingModule({
      imports: [RoleManageComponent],
      providers: [{ provide: RoleService, useValue: roleServiceMock }]
    }).compileComponents();

    fixture = TestBed.createComponent(RoleManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load roles and grouped permissions on init', () => {
    expect(component).toBeTruthy();
    expect(roleServiceMock.getAllRoles).toHaveBeenCalled();
    expect(roleServiceMock.getGroupedPermissions).toHaveBeenCalled();
    expect(component.roles().length).toBe(3);
    expect(component.permissionGroups().length).toBe(2);
    expect(component.matrixDraft().get(1)?.has('ROLE_MANAGE')).toBe(true);
    expect(component.activeRoleForPermissions()?.roleId).toBe(1);
  });

  it('should switch tabs and select role for permissions', () => {
    component.selectRoleForPermissions(mockRoles[1]);
    expect(component.activeTab()).toBe('matrix');
    expect(component.selectedRoleIdForPermissions()).toBe(2);
    expect(component.activeRoleForPermissions()?.roleName).toBe('ROLE_STAFF');
  });

  it('should auto-generate roleCode on displayName change', () => {
    component.openCreateModal();
    component.onDisplayNameChange('Trưởng phòng kỹ thuật');
    expect(component.createRoleForm.get('roleCode')?.value).toBe('ROLE_TRUONG_PHONG_KY_THUAT');
  });

  it('should create new role when form is valid', () => {
    component.openCreateModal();
    component.createRoleForm.patchValue({
      displayName: 'Kế toán viên',
      roleCode: 'ROLE_ACCOUNTANT',
      description: 'Phụ trách sổ sách',
      initialPermissions: ['PRODUCT_VIEW']
    });

    component.submitCreateRole();

    expect(roleServiceMock.createRole).toHaveBeenCalledWith(expect.objectContaining({
      roleName: 'ROLE_ACCOUNTANT'
    }));
    expect(component.isCreateModalOpen()).toBe(false);
  });

  it('should open edit modal and submit updates', () => {
    component.openEditModal(mockRoles[2]);
    expect(component.isEditModalOpen()).toBe(true);

    component.editRoleForm.patchValue({ description: 'Trưởng kho mới' });
    component.submitEditRole();

    expect(roleServiceMock.updateRole).toHaveBeenCalledWith(10, expect.objectContaining({
      description: 'Trưởng kho mới'
    }));
    expect(component.isEditModalOpen()).toBe(false);
  });

  it('should delete custom role with 0 users', () => {
    component.openConfirmDelete(mockRoles[2]);
    expect(component.isConfirmDeleteOpen()).toBe(true);

    component.confirmDeleteRole();

    expect(roleServiceMock.deleteRole).toHaveBeenCalledWith(10);
    expect(component.isConfirmDeleteOpen()).toBe(false);
  });

  it('should toggle permission and group checkbox for role', () => {
    component.selectedRoleIdForPermissions.set(2); // ROLE_STAFF

    // Toggle single permission
    component.toggleMatrixPermission(2, 'PRODUCT_CREATE');
    expect(component.matrixDraft().get(2)?.has('PRODUCT_CREATE')).toBe(true);
    expect(component.isMatrixDirty()).toBe(true);

    // Group select all
    component.toggleGroupForRole(mockGroups[0], 2, true);
    expect(component.isGroupAllChecked(mockGroups[0], 2)).toBe(true);

    // Group uncheck all
    component.toggleGroupForRole(mockGroups[0], 2, false);
    expect(component.isGroupAllChecked(mockGroups[0], 2)).toBe(false);
  });

  it('should prevent unchecking critical admin permissions on ROLE_ADMIN', () => {
    const adminRole = mockRoles[0];
    expect(component.isPermissionDisabled(adminRole, 'ROLE_MANAGE')).toBe(true);

    component.toggleMatrixPermission(1, 'ROLE_MANAGE');
    expect(component.matrixDraft().get(1)?.has('ROLE_MANAGE')).toBe(true);
  });

  it('should save active role permissions and reload', () => {
    component.selectedRoleIdForPermissions.set(2);
    component.toggleMatrixPermission(2, 'PRODUCT_CREATE');
    expect(component.isMatrixDirty()).toBe(true);

    component.saveActiveRolePermissions();

    expect(roleServiceMock.updateRolePermissions).toHaveBeenCalledWith(2, expect.objectContaining({
      permissionCodes: expect.arrayContaining(['PRODUCT_VIEW', 'PRODUCT_CREATE'])
    }));
  });

  it('should format permission codes into dot notation', () => {
    expect(component.formatPermissionCode('PRODUCT_CREATE')).toBe('product.create');
    expect(component.formatPermissionCode('ROLES_MANAGE')).toBe('roles.manage');
  });
});
