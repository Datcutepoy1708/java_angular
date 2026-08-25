import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  AdminUser,
  AdminUserCreateRequest,
  AdminUserPasswordResetRequest,
  AdminUserStatusRequest,
  AdminUserUpdateRequest,
  Role
} from '../../../core/models/admin-user.model';
import { PaginationComponent } from '../../../shared';

@Component({
  selector: 'app-user-manage',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PaginationComponent],
  templateUrl: './user-manage.component.html',
  styleUrl: './user-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserManageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly adminUserService = inject(AdminUserService);
  private readonly authService = inject(AuthService);

  readonly currentAuthUser = this.authService.currentUser;

  // ── State Signals ─────────────────────────────────────────────
  readonly users = signal<AdminUser[]>([]);
  readonly rolesList = signal<Role[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly isDeleting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Pagination & Filters
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly searchKeyword = signal<string>('');
  readonly filterRole = signal<string>('all');
  readonly filterStatus = signal<string>('all');

  // Modals state
  readonly isCreateModalOpen = signal<boolean>(false);
  readonly isEditModalOpen = signal<boolean>(false);
  readonly isResetPasswordModalOpen = signal<boolean>(false);
  readonly isDetailModalOpen = signal<boolean>(false);
  readonly isConfirmDeleteOpen = signal<boolean>(false);

  readonly selectedUser = signal<AdminUser | null>(null);

  // ── Reactive Forms ────────────────────────────────────────────
  readonly createForm: FormGroup = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    phone: ['', [Validators.pattern('^(84|0[3|5|7|8|9])+([0-9]{8})$')]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(50)]],
    gender: ['other'],
    birthDate: [''],
    roles: [['ROLE_STAFF'], [Validators.required]],
    status: ['active', [Validators.required]]
  });

  readonly editForm: FormGroup = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    phone: ['', [Validators.pattern('^(84|0[3|5|7|8|9])+([0-9]{8})$')]],
    gender: ['other'],
    birthDate: [''],
    roles: [['ROLE_CUSTOMER'], [Validators.required]],
    status: ['active', [Validators.required]]
  });

  readonly resetPasswordForm: FormGroup = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(50)]]
  });

  // ── Computed Metrics ──────────────────────────────────────────
  readonly staffAdminCount = computed(() =>
    this.users().filter(u =>
      u.roles?.some(r => r === 'ROLE_ADMIN' || r === 'ROLE_STAFF')
    ).length
  );

  readonly activeCustomersCount = computed(() =>
    this.users().filter(u =>
      u.status === 'active' &&
      u.roles?.includes('ROLE_CUSTOMER') &&
      !u.roles?.some(r => r === 'ROLE_ADMIN' || r === 'ROLE_STAFF')
    ).length
  );

  readonly lockedCount = computed(() =>
    this.users().filter(u => u.status === 'banned' || u.status === 'inactive').length
  );

  ngOnInit(): void {
    this.loadRoles();
    this.loadUsers();
  }

  loadRoles(): void {
    this.adminUserService.getAllRoles().subscribe({
      next: res => {
        this.rolesList.set(res.data || []);
      },
      error: () => {
        // Fallback standard roles
        this.rolesList.set([
          { roleId: 1, roleName: 'ROLE_ADMIN', description: 'Quản trị hệ thống' },
          { roleId: 2, roleName: 'ROLE_STAFF', description: 'Nhân viên bán hàng' },
          { roleId: 3, roleName: 'ROLE_CUSTOMER', description: 'Khách hàng' }
        ]);
      }
    });
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.adminUserService
      .getUsersPaginated(
        this.currentPage(),
        this.pageSize(),
        this.searchKeyword(),
        this.filterRole(),
        this.filterStatus()
      )
      .subscribe({
        next: res => {
          this.users.set(res.data.content || []);
          this.totalElements.set(res.data.totalElements);
          this.totalPages.set(res.data.totalPages);
          this.isLoading.set(false);
        },
        error: err => {
          this.errorMessage.set(err.error?.message || 'Không thể nạp danh sách tài khoản');
          this.isLoading.set(false);
        }
      });
  }

  onSearch(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.searchKeyword.set(val);
    this.currentPage.set(0);
    this.loadUsers();
  }

  onFilterRole(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    this.filterRole.set(val);
    this.currentPage.set(0);
    this.loadUsers();
  }

  onFilterStatus(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    this.filterStatus.set(val);
    this.currentPage.set(0);
    this.loadUsers();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadUsers();
  }

  // ── Create Modal ──────────────────────────────────────────────
  openCreateModal(): void {
    this.createForm.reset({
      fullName: '',
      email: '',
      phone: '',
      password: this.generateRandomPassword(),
      gender: 'other',
      birthDate: '',
      roles: ['ROLE_STAFF'],
      status: 'active'
    });
    this.errorMessage.set(null);
    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
    this.createForm.reset();
  }

  submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set(null);

    const v = this.createForm.value;
    const req: AdminUserCreateRequest = {
      fullName: v.fullName.trim(),
      email: v.email.trim(),
      phone: v.phone?.trim() || null,
      password: v.password,
      gender: v.gender || null,
      birthDate: v.birthDate || null,
      roles: Array.isArray(v.roles) ? v.roles : [v.roles],
      status: v.status
    };

    this.adminUserService.createUser(req).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.closeCreateModal();
        this.showSuccess('Tạo tài khoản người dùng thành công');
        this.loadUsers();
      },
      error: err => {
        this.isSaving.set(false);
        this.errorMessage.set(err.error?.message || 'Có lỗi xảy ra khi tạo tài khoản');
      }
    });
  }

  // ── Edit Modal ────────────────────────────────────────────────
  openEditModal(user: AdminUser): void {
    this.selectedUser.set(user);
    this.editForm.patchValue({
      fullName: user.fullName,
      phone: user.phone || '',
      gender: user.gender || 'other',
      birthDate: user.birthDate || '',
      roles: user.roles || ['ROLE_CUSTOMER'],
      status: user.status || 'active'
    });
    this.errorMessage.set(null);
    this.isEditModalOpen.set(true);
  }

  closeEditModal(): void {
    this.isEditModalOpen.set(false);
    this.selectedUser.set(null);
    this.editForm.reset();
  }

  submitEdit(): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const u = this.selectedUser();
    if (!u) return;

    this.isSaving.set(true);
    this.errorMessage.set(null);

    const v = this.editForm.value;
    const req: AdminUserUpdateRequest = {
      fullName: v.fullName.trim(),
      phone: v.phone?.trim() || null,
      gender: v.gender || null,
      birthDate: v.birthDate || null,
      roles: Array.isArray(v.roles) ? v.roles : [v.roles],
      status: v.status
    };

    this.adminUserService.updateUser(u.userId, req).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.closeEditModal();
        this.showSuccess('Cập nhật tài khoản người dùng thành công');
        this.loadUsers();
      },
      error: err => {
        this.isSaving.set(false);
        this.errorMessage.set(err.error?.message || 'Có lỗi xảy ra khi cập nhật');
      }
    });
  }

  // ── Toggle Role Selection in Checkbox ─────────────────────────
  toggleRole(form: FormGroup, roleName: string): void {
    const currentRoles: string[] = form.get('roles')?.value || [];
    let updated: string[];
    if (currentRoles.includes(roleName)) {
      if (currentRoles.length === 1) return; // Must have at least 1 role
      updated = currentRoles.filter(r => r !== roleName);
    } else {
      updated = [...currentRoles, roleName];
    }
    form.patchValue({ roles: updated });
    form.get('roles')?.markAsTouched();
  }

  isRoleSelected(form: FormGroup, roleName: string): boolean {
    const roles: string[] = form.get('roles')?.value || [];
    return roles.includes(roleName);
  }

  // ── Status Toggle ─────────────────────────────────────────────
  toggleUserStatus(user: AdminUser): void {
    const newStatus = user.status === 'active' ? 'banned' : 'active';
    const req: AdminUserStatusRequest = { status: newStatus };

    this.adminUserService.updateUserStatus(user.userId, req).subscribe({
      next: () => {
        const actionText = newStatus === 'active' ? 'mở khóa' : 'khóa';
        this.showSuccess(`Đã ${actionText} tài khoản "${user.fullName}" thành công`);
        this.loadUsers();
      },
      error: err => {
        this.errorMessage.set(err.error?.message || 'Không thể thay đổi trạng thái');
      }
    });
  }

  // ── Reset Password Modal ──────────────────────────────────────
  openResetPasswordModal(user: AdminUser): void {
    this.selectedUser.set(user);
    this.resetPasswordForm.patchValue({
      newPassword: this.generateRandomPassword()
    });
    this.errorMessage.set(null);
    this.isResetPasswordModalOpen.set(true);
  }

  closeResetPasswordModal(): void {
    this.isResetPasswordModalOpen.set(false);
    this.selectedUser.set(null);
    this.resetPasswordForm.reset();
  }

  submitResetPassword(): void {
    if (this.resetPasswordForm.invalid) {
      this.resetPasswordForm.markAllAsTouched();
      return;
    }

    const u = this.selectedUser();
    if (!u) return;

    this.isSaving.set(true);
    const req: AdminUserPasswordResetRequest = {
      newPassword: this.resetPasswordForm.value.newPassword
    };

    this.adminUserService.resetUserPassword(u.userId, req).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.closeResetPasswordModal();
        this.showSuccess(`Đã đặt lại mật khẩu cho "${u.fullName}" thành công`);
      },
      error: err => {
        this.isSaving.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể đặt lại mật khẩu');
      }
    });
  }

  // ── Detail Modal ──────────────────────────────────────────────
  openDetailModal(user: AdminUser): void {
    this.selectedUser.set(user);
    this.isDetailModalOpen.set(true);
  }

  closeDetailModal(): void {
    this.isDetailModalOpen.set(false);
    this.selectedUser.set(null);
  }

  // ── Delete Confirm Modal ──────────────────────────────────────
  openConfirmDelete(user: AdminUser): void {
    this.selectedUser.set(user);
    this.isConfirmDeleteOpen.set(true);
  }

  closeConfirmDelete(): void {
    this.isConfirmDeleteOpen.set(false);
    this.selectedUser.set(null);
  }

  confirmDelete(): void {
    const u = this.selectedUser();
    if (!u) return;

    this.isDeleting.set(true);
    this.adminUserService.deleteUser(u.userId).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.closeConfirmDelete();
        this.showSuccess(`Đã xóa tài khoản "${u.fullName}" thành công`);
        this.loadUsers();
      },
      error: err => {
        this.isDeleting.set(false);
        this.closeConfirmDelete();
        this.errorMessage.set(err.error?.message || 'Không thể xóa tài khoản');
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────
  generateRandomPassword(): string {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%';
    let pwd = 'Cp#';
    for (let i = 0; i < 7; i++) {
      pwd += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return pwd;
  }

  copyPassword(pwd: string): void {
    navigator.clipboard.writeText(pwd);
    this.showSuccess('Đã sao chép mật khẩu vào bộ nhớ tạm');
  }

  getUserInitials(name: string): string {
    if (!name) return 'U';
    return name
      .split(' ')
      .map(w => w[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();
  }

  formatPrice(amount: number | null): string {
    if (!amount) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '—';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch {
      return dateStr;
    }
  }

  private showSuccess(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => {
      this.successMessage.set(null);
    }, 4000);
  }
}
