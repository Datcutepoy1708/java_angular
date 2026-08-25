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
import { AdminUser, AdminUserStatusRequest, AdminUserPasswordResetRequest, AdminUserUpdateRequest } from '../../../core/models/admin-user.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-customer-manage',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent],
  templateUrl: './customer-manage.component.html',
  styleUrl: './customer-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CustomerManageComponent implements OnInit {
  private readonly adminUserService = inject(AdminUserService);
  private readonly fb = inject(FormBuilder);

  readonly customers = signal<AdminUser[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Pagination & Filtering
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly searchKeyword = signal<string>('');
  readonly selectedStatus = signal<string>('all');

  // Stats
  readonly activeCustomersCount = computed(() =>
    this.customers().filter(c => c.status === 'active').length
  );
  readonly bannedCustomersCount = computed(() =>
    this.customers().filter(c => c.status === 'banned').length
  );

  // Modals state
  readonly isDetailModalOpen = signal<boolean>(false);
  readonly isEditModalOpen = signal<boolean>(false);
  readonly isResetPasswordModalOpen = signal<boolean>(false);
  readonly isConfirmStatusOpen = signal<boolean>(false);

  readonly selectedCustomer = signal<AdminUser | null>(null);
  readonly statusActionTarget = signal<{ customer: AdminUser; nextStatus: 'active' | 'banned' } | null>(null);

  // Forms
  editForm!: FormGroup;
  resetPasswordForm!: FormGroup;

  ngOnInit(): void {
    this.initForms();
    this.loadCustomers();
  }

  private initForms(): void {
    this.editForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.maxLength(100)]],
      phone: ['', [Validators.pattern(/^[0-9]{10,11}$/)]],
      gender: ['other'],
      status: ['active', Validators.required]
    });

    this.resetPasswordForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(50)]]
    });
  }

  loadCustomers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.adminUserService
      .getCustomersPaginated(
        this.currentPage(),
        this.pageSize(),
        this.searchKeyword(),
        this.selectedStatus(),
        'createdAt',
        'desc'
      )
      .subscribe({
        next: res => {
          this.isLoading.set(false);
          if (res.success && res.data) {
            this.customers.set(res.data.content);
            this.totalElements.set(res.data.totalElements);
            this.totalPages.set(res.data.totalPages);
          }
        },
        error: err => {
          this.isLoading.set(false);
          this.errorMessage.set(err.error?.message || 'Không thể tải danh sách khách hàng');
        }
      });
  }

  onSearch(keyword: string): void {
    this.searchKeyword.set(keyword);
    this.currentPage.set(0);
    this.loadCustomers();
  }

  onStatusFilter(status: string): void {
    this.selectedStatus.set(status);
    this.currentPage.set(0);
    this.loadCustomers();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadCustomers();
  }

  openDetailModal(customer: AdminUser): void {
    this.selectedCustomer.set(customer);
    this.isDetailModalOpen.set(true);
  }

  openEditModal(customer: AdminUser): void {
    this.selectedCustomer.set(customer);
    this.editForm.patchValue({
      fullName: customer.fullName,
      phone: customer.phone || '',
      gender: customer.gender || 'other',
      status: customer.status
    });
    this.isEditModalOpen.set(true);
  }

  submitEdit(): void {
    if (this.editForm.invalid || !this.selectedCustomer()) return;

    this.isLoading.set(true);
    const formVal = this.editForm.value;
    const req: AdminUserUpdateRequest = {
      fullName: formVal.fullName.trim(),
      phone: formVal.phone ? formVal.phone.trim() : null,
      gender: formVal.gender,
      roles: ['ROLE_CUSTOMER'],
      status: formVal.status
    };

    this.adminUserService.updateUser(this.selectedCustomer()!.userId, req).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isEditModalOpen.set(false);
        this.showToast('Cập nhật hồ sơ khách hàng thành công');
        this.loadCustomers();
      },
      error: err => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể cập nhật hồ sơ khách hàng');
      }
    });
  }

  openResetPasswordModal(customer: AdminUser): void {
    this.selectedCustomer.set(customer);
    this.resetPasswordForm.reset();
    this.isResetPasswordModalOpen.set(true);
  }

  submitResetPassword(): void {
    if (this.resetPasswordForm.invalid || !this.selectedCustomer()) return;

    this.isLoading.set(true);
    const req: AdminUserPasswordResetRequest = {
      newPassword: this.resetPasswordForm.value.newPassword
    };

    this.adminUserService.resetUserPassword(this.selectedCustomer()!.userId, req).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isResetPasswordModalOpen.set(false);
        this.showToast('Đặt lại mật khẩu cho khách hàng thành công');
      },
      error: err => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể đặt lại mật khẩu');
      }
    });
  }

  promptToggleStatus(customer: AdminUser): void {
    const nextStatus = customer.status === 'active' ? 'banned' : 'active';
    this.statusActionTarget.set({ customer, nextStatus });
    this.isConfirmStatusOpen.set(true);
  }

  confirmToggleStatus(): void {
    const target = this.statusActionTarget();
    if (!target) return;

    this.isLoading.set(true);
    const req: AdminUserStatusRequest = { status: target.nextStatus };

    this.adminUserService.updateUserStatus(target.customer.userId, req).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isConfirmStatusOpen.set(false);
        this.statusActionTarget.set(null);
        this.showToast(`Đã ${target.nextStatus === 'active' ? 'mở khóa' : 'khóa'} tài khoản khách hàng`);
        this.loadCustomers();
      },
      error: err => {
        this.isLoading.set(false);
        this.isConfirmStatusOpen.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể cập nhật trạng thái');
      }
    });
  }

  getUserInitials(name: string): string {
    if (!name) return 'KH';
    const parts = name.trim().split(' ');
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  private showToast(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(null), 4000);
  }
}
