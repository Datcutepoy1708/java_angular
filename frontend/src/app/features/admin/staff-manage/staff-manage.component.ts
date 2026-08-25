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
import { AdminUserService } from '../../../core/services/admin-user.service';
import { RoleService } from '../../../core/services/role.service';
import { AuthService } from '../../../core/services/auth.service';
import { AdminUser, AdminUserCreateRequest, AdminUserPasswordResetRequest, AdminUserStatusRequest, AdminUserUpdateRequest } from '../../../core/models/admin-user.model';
import { RoleDetail } from '../../../core/models/role.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-staff-manage',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent],
  templateUrl: './staff-manage.component.html',
  styleUrl: './staff-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StaffManageComponent implements OnInit {
  private readonly adminUserService = inject(AdminUserService);
  private readonly roleService = inject(RoleService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly staffList = signal<AdminUser[]>([]);
  readonly availableRoles = signal<RoleDetail[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Pagination & Filtering
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly searchKeyword = signal<string>('');
  readonly selectedRole = signal<string>('all');
  readonly selectedStatus = signal<string>('all');

  // Stats
  readonly totalAdminsCount = computed(() =>
    this.staffList().filter(s => s.roles.includes('ROLE_ADMIN')).length
  );
  readonly activeStaffCount = computed(() =>
    this.staffList().filter(s => s.status === 'active').length
  );

  // Current logged in user ID
  readonly currentUserId = computed(() => this.authService.currentUser()?.userId || 0);

  // Active Admin Guard check
  readonly activeAdminsInCurrentPage = computed(() =>
    this.staffList().filter(s => s.roles.includes('ROLE_ADMIN') && s.status === 'active').length
  );

  // Modals state
  readonly isCreateModalOpen = signal<boolean>(false);
  readonly isEditModalOpen = signal<boolean>(false);
  readonly isDetailModalOpen = signal<boolean>(false);
  readonly isResetPasswordModalOpen = signal<boolean>(false);
  readonly isConfirmDeleteOpen = signal<boolean>(false);
  readonly isConfirmStatusOpen = signal<boolean>(false);

  readonly selectedStaff = signal<AdminUser | null>(null);
  readonly statusActionTarget = signal<{ staff: AdminUser; nextStatus: 'active' | 'banned' | 'inactive' } | null>(null);

  // Forms
  createForm!: FormGroup;
  editForm!: FormGroup;
  resetPasswordForm!: FormGroup;

  ngOnInit(): void {
    this.initForms();
    this.loadRoles();
    this.loadStaff();
  }

  private initForms(): void {
    this.createForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      phone: ['', [Validators.pattern(/^[0-9]{10,11}$/)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(50)]],
      gender: ['male'],
      roles: [['ROLE_STAFF'], [Validators.required]],
      status: ['active', Validators.required]
    });

    this.editForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.maxLength(100)]],
      phone: ['', [Validators.pattern(/^[0-9]{10,11}$/)]],
      gender: ['male'],
      roles: [[], [Validators.required]],
      status: ['active', Validators.required]
    });

    this.resetPasswordForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(50)]]
    });
  }

  loadRoles(): void {
    this.roleService.getAllRoles().subscribe({
      next: res => {
        if (res.success && res.data) {
          // Exclude ROLE_CUSTOMER from staff assignable roles
          const staffRoles = res.data.filter(r => r.roleName !== 'ROLE_CUSTOMER');
          this.availableRoles.set(staffRoles);
        }
      },
      error: () => {}
    });
  }

  loadStaff(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.adminUserService
      .getStaffPaginated(
        this.currentPage(),
        this.pageSize(),
        this.searchKeyword(),
        this.selectedRole(),
        this.selectedStatus(),
        'createdAt',
        'desc'
      )
      .subscribe({
        next: res => {
          this.isLoading.set(false);
          if (res.success && res.data) {
            this.staffList.set(res.data.content);
            this.totalElements.set(res.data.totalElements);
            this.totalPages.set(res.data.totalPages);
          }
        },
        error: err => {
          this.isLoading.set(false);
          this.errorMessage.set(err.error?.message || 'Không thể tải danh sách nhân sự');
        }
      });
  }

  onSearch(keyword: string): void {
    this.searchKeyword.set(keyword);
    this.currentPage.set(0);
    this.loadStaff();
  }

  onRoleFilter(role: string): void {
    this.selectedRole.set(role);
    this.currentPage.set(0);
    this.loadStaff();
  }

  onStatusFilter(status: string): void {
    this.selectedStatus.set(status);
    this.currentPage.set(0);
    this.loadStaff();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadStaff();
  }

  openCreateModal(): void {
    this.createForm.reset({
      fullName: '',
      email: '',
      phone: '',
      password: this.generateRandomPassword(),
      gender: 'male',
      roles: ['ROLE_STAFF'],
      status: 'active'
    });
    this.isCreateModalOpen.set(true);
  }

  generateRandomPassword(): string {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$';
    let pwd = '';
    for (let i = 0; i < 12; i++) {
      pwd += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return pwd;
  }

  copyPassword(inputEl: HTMLInputElement): void {
    navigator.clipboard.writeText(inputEl.value).then(() => {
      this.showToast('Đã sao chép mật khẩu vào clipboard');
    });
  }

  toggleCreateRole(roleCode: string): void {
    const current: string[] = this.createForm.get('roles')?.value || [];
    const index = current.indexOf(roleCode);
    let updated: string[];
    if (index > -1) {
      if (current.length === 1) return; // Must have at least 1 role
      updated = current.filter(r => r !== roleCode);
    } else {
      updated = [...current, roleCode];
    }
    this.createForm.get('roles')?.setValue(updated);
  }

  isCreateRoleSelected(roleCode: string): boolean {
    const current: string[] = this.createForm.get('roles')?.value || [];
    return current.includes(roleCode);
  }

  submitCreate(): void {
    if (this.createForm.invalid) return;

    this.isLoading.set(true);
    const formVal = this.createForm.value;
    const req: AdminUserCreateRequest = {
      fullName: formVal.fullName.trim(),
      email: formVal.email.trim(),
      phone: formVal.phone ? formVal.phone.trim() : null,
      password: formVal.password,
      gender: formVal.gender,
      roles: formVal.roles,
      status: formVal.status
    };

    this.adminUserService.createUser(req).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isCreateModalOpen.set(false);
        this.showToast('Thêm nhân sự mới thành công');
        this.loadStaff();
      },
      error: err => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể tạo nhân sự mới');
      }
    });
  }

  openEditModal(staff: AdminUser): void {
    this.selectedStaff.set(staff);
    this.editForm.patchValue({
      fullName: staff.fullName,
      phone: staff.phone || '',
      gender: staff.gender || 'male',
      roles: staff.roles || ['ROLE_STAFF'],
      status: staff.status
    });
    this.isEditModalOpen.set(true);
  }

  toggleEditRole(roleCode: string): void {
    const current: string[] = this.editForm.get('roles')?.value || [];
    const index = current.indexOf(roleCode);
    let updated: string[];
    if (index > -1) {
      if (current.length === 1) return;
      updated = current.filter(r => r !== roleCode);
    } else {
      updated = [...current, roleCode];
    }
    this.editForm.get('roles')?.setValue(updated);
  }

  isEditRoleSelected(roleCode: string): boolean {
    const current: string[] = this.editForm.get('roles')?.value || [];
    return current.includes(roleCode);
  }

  submitEdit(): void {
    if (this.editForm.invalid || !this.selectedStaff()) return;

    this.isLoading.set(true);
    const formVal = this.editForm.value;
    const req: AdminUserUpdateRequest = {
      fullName: formVal.fullName.trim(),
      phone: formVal.phone ? formVal.phone.trim() : null,
      gender: formVal.gender,
      roles: formVal.roles,
      status: formVal.status
    };

    this.adminUserService.updateUser(this.selectedStaff()!.userId, req).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isEditModalOpen.set(false);
        this.showToast('Cập nhật nhân sự thành công');
        this.loadStaff();
      },
      error: err => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể cập nhật nhân sự');
      }
    });
  }

  openDetailModal(staff: AdminUser): void {
    this.selectedStaff.set(staff);
    this.isDetailModalOpen.set(true);
  }

  openResetPasswordModal(staff: AdminUser): void {
    this.selectedStaff.set(staff);
    this.resetPasswordForm.reset();
    this.isResetPasswordModalOpen.set(true);
  }

  submitResetPassword(): void {
    if (this.resetPasswordForm.invalid || !this.selectedStaff()) return;

    this.isLoading.set(true);
    const req: AdminUserPasswordResetRequest = {
      newPassword: this.resetPasswordForm.value.newPassword
    };

    this.adminUserService.resetUserPassword(this.selectedStaff()!.userId, req).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isResetPasswordModalOpen.set(false);
        this.showToast('Đặt lại mật khẩu nhân viên thành công');
      },
      error: err => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể đặt lại mật khẩu');
      }
    });
  }

  promptToggleStatus(staff: AdminUser): void {
    if (this.isSelfOrLastAdmin(staff)) return;
    const nextStatus = staff.status === 'active' ? 'banned' : 'active';
    this.statusActionTarget.set({ staff, nextStatus });
    this.isConfirmStatusOpen.set(true);
  }

  confirmToggleStatus(): void {
    const target = this.statusActionTarget();
    if (!target) return;

    this.isLoading.set(true);
    const req: AdminUserStatusRequest = { status: target.nextStatus };

    this.adminUserService.updateUserStatus(target.staff.userId, req).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isConfirmStatusOpen.set(false);
        this.statusActionTarget.set(null);
        this.showToast(`Đã ${target.nextStatus === 'active' ? 'mở khóa' : 'khóa'} tài khoản nhân viên`);
        this.loadStaff();
      },
      error: err => {
        this.isLoading.set(false);
        this.isConfirmStatusOpen.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể cập nhật trạng thái');
      }
    });
  }

  openConfirmDelete(staff: AdminUser): void {
    if (this.isSelfOrLastAdmin(staff)) return;
    this.selectedStaff.set(staff);
    this.isConfirmDeleteOpen.set(true);
  }

  confirmDelete(): void {
    if (!this.selectedStaff()) return;

    this.isLoading.set(true);
    this.adminUserService.deleteUser(this.selectedStaff()!.userId).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isConfirmDeleteOpen.set(false);
        this.selectedStaff.set(null);
        this.showToast('Đã xóa tài khoản nhân viên');
        this.loadStaff();
      },
      error: err => {
        this.isLoading.set(false);
        this.isConfirmDeleteOpen.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể xóa tài khoản');
      }
    });
  }

  isSelfOrLastAdmin(staff: AdminUser): boolean {
    if (staff.userId === this.currentUserId()) return true;
    if (staff.roles.includes('ROLE_ADMIN') && staff.status === 'active' && this.activeAdminsInCurrentPage() <= 1) {
      return true;
    }
    return false;
  }

  getRoleLabel(roleCode: string): string {
    const found = this.availableRoles().find(r => r.roleName === roleCode);
    if (found && found.description) return found.description;
    switch (roleCode) {
      case 'ROLE_ADMIN': return 'Quản Trị Viên';
      case 'ROLE_STAFF': return 'Nhân Viên';
      default: return roleCode.replace('ROLE_', '');
    }
  }

  getUserInitials(name: string): string {
    if (!name) return 'NV';
    const parts = name.trim().split(' ');
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  private showToast(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(null), 4000);
  }
}
