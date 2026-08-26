import {
  Component,
  OnInit,
  signal,
  computed,
  inject,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RoleService } from '../../../core/services/role.service';
import {
  PermissionGroup,
  PermissionItem,
  RoleCreateRequest,
  RoleDetail,
  RolePermissionsUpdateRequest,
  RoleUpdateRequest
} from '../../../core/models/role.model';

@Component({
  selector: 'app-role-manage',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './role-manage.component.html',
  styleUrl: './role-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RoleManageComponent implements OnInit {
  private readonly roleService = inject(RoleService);
  private readonly fb = inject(FormBuilder);

  // Active Tab: 'roles' | 'matrix' (permissions assignment)
  readonly activeTab = signal<'roles' | 'matrix'>('roles');

  readonly roles = signal<RoleDetail[]>([]);
  readonly permissionGroups = signal<PermissionGroup[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Selected role for permission assignment in Tab 2
  readonly selectedRoleIdForPermissions = signal<number>(1);

  readonly activeRoleForPermissions = computed(() => {
    const list = this.roles();
    if (list.length === 0) return null;
    return list.find(r => r.roleId === this.selectedRoleIdForPermissions()) || list[0];
  });

  // Working copy state: roleId -> Set<permissionCode>
  readonly matrixDraft = signal<Map<number, Set<string>>>(new Map());
  readonly isMatrixDirty = signal<boolean>(false);

  // Total permissions assigned to the currently selected role
  readonly activeRoleAssignedPermissionsCount = computed(() => {
    const role = this.activeRoleForPermissions();
    if (!role) return 0;
    const perms = this.matrixDraft().get(role.roleId);
    return perms ? perms.size : 0;
  });

  // Total system permissions across all groups
  readonly totalSystemPermissionsCount = computed(() => {
    return this.permissionGroups().reduce((acc, g) => acc + (g.permissions?.length || 0), 0);
  });

  // Modals state
  readonly isCreateModalOpen = signal<boolean>(false);
  readonly isEditModalOpen = signal<boolean>(false);
  readonly isConfirmDeleteOpen = signal<boolean>(false);

  readonly selectedRole = signal<RoleDetail | null>(null);

  // Forms
  createRoleForm!: FormGroup;
  editRoleForm!: FormGroup;

  // Protected permissions for ROLE_ADMIN against lockout
  readonly criticalAdminPermissions = new Set(['ROLE_MANAGE', 'STAFF_MANAGE', 'SETTING_MANAGE']);

  ngOnInit(): void {
    this.initForms();
    this.loadData();
  }

  private initForms(): void {
    this.createRoleForm = this.fb.group({
      displayName: ['', [Validators.required, Validators.maxLength(100)]],
      roleCode: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9_]+$/), Validators.maxLength(50)]],
      description: ['', [Validators.maxLength(255)]],
      initialPermissions: [[]]
    });

    this.editRoleForm = this.fb.group({
      roleName: ['', [Validators.maxLength(50)]],
      description: ['', [Validators.maxLength(255)]]
    });
  }

  loadData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.roleService.getAllRoles().subscribe({
      next: rolesRes => {
        if (rolesRes.success && rolesRes.data) {
          this.roles.set(rolesRes.data);
          this.initMatrixDraft(rolesRes.data);

          // Default selected role
          if (rolesRes.data.length > 0 && !this.roles().some(r => r.roleId === this.selectedRoleIdForPermissions())) {
            this.selectedRoleIdForPermissions.set(rolesRes.data[0].roleId);
          }
        }

        this.roleService.getGroupedPermissions().subscribe({
          next: permRes => {
            this.isLoading.set(false);
            if (permRes.success && permRes.data) {
              this.permissionGroups.set(permRes.data);
            }
          },
          error: err => {
            this.isLoading.set(false);
            this.errorMessage.set(err.error?.message || 'Không thể tải nhóm quyền');
          }
        });
      },
      error: err => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể tải danh sách chức vụ');
      }
    });
  }

  initMatrixDraft(rolesList: RoleDetail[]): void {
    const draft = new Map<number, Set<string>>();
    for (const r of rolesList) {
      draft.set(r.roleId, new Set(r.permissionCodes || []));
    }
    this.matrixDraft.set(draft);
    this.isMatrixDirty.set(false);
  }

  switchTab(tab: 'roles' | 'matrix'): void {
    this.activeTab.set(tab);
  }

  selectRoleForPermissions(role: RoleDetail): void {
    this.selectedRoleIdForPermissions.set(role.roleId);
    this.activeTab.set('matrix');
  }

  // --- Auto generate role code from display name ---
  onDisplayNameChange(name: string): void {
    if (!name) return;
    const ascii = name
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd')
      .replace(/Đ/g, 'D')
      .toUpperCase()
      .trim()
      .replace(/[^A-Z0-9]/g, '_')
      .replace(/_+/g, '_');

    const code = ascii.startsWith('ROLE_') ? ascii : `ROLE_${ascii}`;
    this.createRoleForm.get('roleCode')?.setValue(code);
  }

  openCreateModal(): void {
    this.createRoleForm.reset({
      displayName: '',
      roleCode: '',
      description: '',
      initialPermissions: []
    });
    this.isCreateModalOpen.set(true);
  }

  toggleCreatePermission(code: string): void {
    const current: string[] = this.createRoleForm.get('initialPermissions')?.value || [];
    const index = current.indexOf(code);
    if (index > -1) {
      this.createRoleForm.get('initialPermissions')?.setValue(current.filter(c => c !== code));
    } else {
      this.createRoleForm.get('initialPermissions')?.setValue([...current, code]);
    }
  }

  isCreatePermissionSelected(code: string): boolean {
    const current: string[] = this.createRoleForm.get('initialPermissions')?.value || [];
    return current.includes(code);
  }

  submitCreateRole(): void {
    if (this.createRoleForm.invalid) return;

    this.isLoading.set(true);
    const formVal = this.createRoleForm.value;
    const req: RoleCreateRequest = {
      roleName: formVal.roleCode.trim(),
      description: formVal.displayName.trim() + (formVal.description ? ` - ${formVal.description.trim()}` : ''),
      permissionCodes: formVal.initialPermissions
    };

    this.roleService.createRole(req).subscribe({
      next: res => {
        this.isLoading.set(false);
        this.isCreateModalOpen.set(false);
        this.showToast('Tạo chức vụ mới thành công');
        if (res.data) {
          this.selectedRoleIdForPermissions.set(res.data.roleId);
        }
        this.loadData();
      },
      error: err => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể tạo chức vụ');
      }
    });
  }

  openEditModal(role: RoleDetail): void {
    this.selectedRole.set(role);
    this.editRoleForm.patchValue({
      roleName: role.roleName,
      description: role.description || ''
    });
    this.isEditModalOpen.set(true);
  }

  submitEditRole(): void {
    if (this.editRoleForm.invalid || !this.selectedRole()) return;

    this.isLoading.set(true);
    const formVal = this.editRoleForm.value;
    const req: RoleUpdateRequest = {
      roleName: this.selectedRole()!.isSystemRole ? undefined : formVal.roleName.trim(),
      description: formVal.description ? formVal.description.trim() : ''
    };

    this.roleService.updateRole(this.selectedRole()!.roleId, req).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isEditModalOpen.set(false);
        this.showToast('Cập nhật chức vụ thành công');
        this.loadData();
      },
      error: err => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể cập nhật chức vụ');
      }
    });
  }

  openConfirmDelete(role: RoleDetail): void {
    if (role.isSystemRole || role.userCount > 0) return;
    this.selectedRole.set(role);
    this.isConfirmDeleteOpen.set(true);
  }

  confirmDeleteRole(): void {
    if (!this.selectedRole()) return;

    this.isLoading.set(true);
    this.roleService.deleteRole(this.selectedRole()!.roleId).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isConfirmDeleteOpen.set(false);
        this.selectedRole.set(null);
        this.showToast('Đã xóa chức vụ thành công');
        this.loadData();
      },
      error: err => {
        this.isLoading.set(false);
        this.isConfirmDeleteOpen.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể xóa chức vụ');
      }
    });
  }

  // --- Permission Selection Logic for Active Role ---
  isPermissionChecked(roleId: number, permCode: string): boolean {
    const rolePerms = this.matrixDraft().get(roleId);
    return rolePerms ? rolePerms.has(permCode) : false;
  }

  isPermissionDisabled(role: RoleDetail | null, permCode: string): boolean {
    if (!role) return false;
    if (role.roleName === 'ROLE_ADMIN' && this.criticalAdminPermissions.has(permCode)) {
      return true;
    }
    return false;
  }

  toggleMatrixPermission(roleId: number, permCode: string): void {
    const role = this.roles().find(r => r.roleId === roleId);
    if (role && this.isPermissionDisabled(role, permCode)) return;

    const draft = new Map(this.matrixDraft());
    let perms = draft.get(roleId);
    if (!perms) {
      perms = new Set<string>();
      draft.set(roleId, perms);
    } else {
      perms = new Set<string>(perms);
      draft.set(roleId, perms);
    }

    if (perms.has(permCode)) {
      perms.delete(permCode);
    } else {
      perms.add(permCode);
    }

    this.matrixDraft.set(draft);
    this.isMatrixDirty.set(true);
  }

  isGroupAllChecked(group: PermissionGroup, roleId: number): boolean {
    const rolePerms = this.matrixDraft().get(roleId);
    if (!rolePerms || group.permissions.length === 0) return false;
    return group.permissions.every(p => rolePerms.has(p.permissionCode));
  }

  toggleGroupForRole(group: PermissionGroup, roleId: number, checkAll: boolean): void {
    const role = this.roles().find(r => r.roleId === roleId);
    if (!role) return;

    const draft = new Map(this.matrixDraft());
    let perms = draft.get(roleId);
    perms = perms ? new Set<string>(perms) : new Set<string>();
    draft.set(roleId, perms);

    for (const p of group.permissions) {
      if (role.roleName === 'ROLE_ADMIN' && this.criticalAdminPermissions.has(p.permissionCode)) {
        continue;
      }
      if (checkAll) {
        perms.add(p.permissionCode);
      } else {
        perms.delete(p.permissionCode);
      }
    }

    this.matrixDraft.set(draft);
    this.isMatrixDirty.set(true);
  }

  toggleAllPermissionsForActiveRole(checkAll: boolean): void {
    const role = this.activeRoleForPermissions();
    if (!role) return;

    const draft = new Map(this.matrixDraft());
    let perms = draft.get(role.roleId);
    perms = perms ? new Set<string>(perms) : new Set<string>();
    draft.set(role.roleId, perms);

    for (const group of this.permissionGroups()) {
      for (const p of group.permissions) {
        if (role.roleName === 'ROLE_ADMIN' && this.criticalAdminPermissions.has(p.permissionCode)) {
          continue;
        }
        if (checkAll) {
          perms.add(p.permissionCode);
        } else {
          perms.delete(p.permissionCode);
        }
      }
    }

    this.matrixDraft.set(draft);
    this.isMatrixDirty.set(true);
  }

  resetMatrixChanges(): void {
    this.initMatrixDraft(this.roles());
  }

  saveActiveRolePermissions(): void {
    const role = this.activeRoleForPermissions();
    if (!role) return;

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const perms = this.matrixDraft().get(role.roleId) || new Set<string>();
    const req: RolePermissionsUpdateRequest = {
      permissionCodes: Array.from(perms)
    };

    this.roleService.updateRolePermissions(role.roleId, req).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isMatrixDirty.set(false);
        this.showToast(`Lưu quyền hạn cho chức vụ ${role.description || role.roleName} thành công`);
        this.loadData();
      },
      error: err => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Có lỗi xảy ra khi lưu quyền hạn');
      }
    });
  }

  saveMatrixChanges(): void {
    this.saveActiveRolePermissions();
  }

  formatPermissionCode(code: string): string {
    return code.toLowerCase().replace(/_/g, '.');
  }

  private showToast(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(null), 4000);
  }
}
