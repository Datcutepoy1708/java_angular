import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { DiscountService } from '../../../core/services/discount.service';
import { CategoryService } from '../../../core/services/category.service';
import {
  Discount,
  DiscountFilterParams,
  DiscountMetrics,
  DiscountStatus,
  DiscountType,
  DiscountUsage
} from '../../../core/models/discount.model';
import { CategoryResponse } from '../../../core/models/category.model';

@Component({
  selector: 'app-discount-manage',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './discount-manage.component.html',
  styleUrls: ['./discount-manage.component.scss']
})
export class DiscountManageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly discountService = inject(DiscountService);
  private readonly categoryService = inject(CategoryService);

  readonly discounts = signal<Discount[]>([]);
  readonly categories = signal<CategoryResponse[]>([]);
  readonly metrics = signal<DiscountMetrics>({
    totalDiscounts: 0,
    activeDiscounts: 0,
    totalUsedCount: 0,
    expiredDiscounts: 0
  });

  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Pagination & Filter
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(10);

  readonly searchKeyword = signal<string>('');
  readonly filterStatus = signal<string>('');
  readonly filterType = signal<string>('');

  // Modals state
  readonly isModalOpen = signal<boolean>(false);
  readonly isEditing = signal<boolean>(false);
  readonly selectedDiscountId = signal<number | null>(null);

  readonly isUsagesModalOpen = signal<boolean>(false);
  readonly selectedDiscountCode = signal<string>('');
  readonly usagesList = signal<DiscountUsage[]>([]);
  readonly isLoadingUsages = signal<boolean>(false);

  discountForm!: FormGroup;

  ngOnInit(): void {
    this.initForm();
    this.loadMetrics();
    this.loadCategories();
    this.loadDiscounts();
  }

  private initForm(): void {
    const now = new Date();
    const future = new Date();
    future.setDate(future.getDate() + 30);

    const formatDateForInput = (d: Date) => d.toISOString().slice(0, 16);

    this.discountForm = this.fb.group({
      code: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      description: ['', [Validators.maxLength(255)]],
      discountType: ['percent' as DiscountType, [Validators.required]],
      discountValue: [10, [Validators.required, Validators.min(0.01)]],
      maxDiscountAmount: [null],
      minOrderValue: [0, [Validators.min(0)]],
      usageLimit: [null, [Validators.min(1)]],
      usageLimitPerUser: [1, [Validators.required, Validators.min(1)]],
      applicableCategoryId: [null],
      startDate: [formatDateForInput(now), [Validators.required]],
      endDate: [formatDateForInput(future), [Validators.required]],
      status: ['active' as DiscountStatus, [Validators.required]]
    });
  }

  loadMetrics(): void {
    this.discountService.getMetrics().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.metrics.set(res.data);
        }
      },
      error: () => {}
    });
  }

  loadCategories(): void {
    this.categoryService.getAll().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.categories.set(res.data);
        }
      },
      error: () => {}
    });
  }

  loadDiscounts(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const params: DiscountFilterParams = {
      page: this.currentPage(),
      size: this.pageSize(),
      keyword: this.searchKeyword().trim() || undefined,
      status: (this.filterStatus() as DiscountStatus) || undefined,
      discountType: (this.filterType() as DiscountType) || undefined,
      sortBy: 'createdAt',
      sortDir: 'desc'
    };

    this.discountService.getAdminDiscounts(params).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res.success && res.data) {
          this.discounts.set(res.data.content || []);
          this.totalElements.set(res.data.totalElements || 0);
          this.totalPages.set(res.data.totalPages || 0);
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể tải danh sách mã giảm giá.');
      }
    });
  }

  onSearch(): void {
    this.currentPage.set(0);
    this.loadDiscounts();
  }

  resetFilter(): void {
    this.searchKeyword.set('');
    this.filterStatus.set('');
    this.filterType.set('');
    this.currentPage.set(0);
    this.loadDiscounts();
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadDiscounts();
    }
  }

  openCreateModal(): void {
    this.isEditing.set(false);
    this.selectedDiscountId.set(null);
    this.initForm();
    this.isModalOpen.set(true);
  }

  openEditModal(discount: Discount): void {
    this.isEditing.set(true);
    this.selectedDiscountId.set(discount.discountId);

    const startDateStr = discount.startDate ? discount.startDate.slice(0, 16) : '';
    const endDateStr = discount.endDate ? discount.endDate.slice(0, 16) : '';

    this.discountForm.patchValue({
      code: discount.code,
      description: discount.description || '',
      discountType: discount.discountType,
      discountValue: discount.discountValue,
      maxDiscountAmount: discount.maxDiscountAmount || null,
      minOrderValue: discount.minOrderValue || 0,
      usageLimit: discount.usageLimit || null,
      usageLimitPerUser: discount.usageLimitPerUser || 1,
      applicableCategoryId: discount.applicableCategoryId || null,
      startDate: startDateStr,
      endDate: endDateStr,
      status: discount.status
    });

    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
  }

  saveDiscount(): void {
    if (this.discountForm.invalid) {
      this.discountForm.markAllAsTouched();
      return;
    }

    const formVal = this.discountForm.value;

    // Date validation
    if (new Date(formVal.endDate) <= new Date(formVal.startDate)) {
      alert('Ngày kết thúc phải sau ngày bắt đầu.');
      return;
    }

    // % validation
    if (formVal.discountType === 'percent' && formVal.discountValue > 100) {
      alert('Giá trị giảm phần trăm không được vượt quá 100%.');
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const payload = {
      ...formVal,
      code: formVal.code ? formVal.code.trim().toUpperCase() : '',
      startDate: new Date(formVal.startDate).toISOString(),
      endDate: new Date(formVal.endDate).toISOString()
    };

    if (this.isEditing() && this.selectedDiscountId()) {
      this.discountService.updateDiscount(this.selectedDiscountId()!, payload).subscribe({
        next: (res) => {
          this.isSaving.set(false);
          this.closeModal();
          this.successMessage.set('Cập nhật mã giảm giá thành công.');
          this.loadDiscounts();
          this.loadMetrics();
        },
        error: (err) => {
          this.isSaving.set(false);
          alert(err.error?.message || 'Có lỗi xảy ra khi cập nhật mã.');
        }
      });
    } else {
      this.discountService.createDiscount(payload).subscribe({
        next: (res) => {
          this.isSaving.set(false);
          this.closeModal();
          this.successMessage.set('Tạo mới mã giảm giá thành công.');
          this.loadDiscounts();
          this.loadMetrics();
        },
        error: (err) => {
          this.isSaving.set(false);
          alert(err.error?.message || 'Có lỗi xảy ra khi tạo mã giảm giá.');
        }
      });
    }
  }

  deleteDiscount(discount: Discount): void {
    if (!confirm(`Bạn có chắc chắn muốn vô hiệu hóa mã '${discount.code}'?`)) {
      return;
    }

    this.discountService.deleteDiscount(discount.discountId).subscribe({
      next: () => {
        this.successMessage.set(`Đã vô hiệu hóa mã '${discount.code}' thành công.`);
        this.loadDiscounts();
        this.loadMetrics();
      },
      error: (err) => {
        alert(err.error?.message || 'Không thể vô hiệu hóa mã giảm giá.');
      }
    });
  }

  openUsagesModal(discount: Discount): void {
    this.selectedDiscountCode.set(discount.code);
    this.isUsagesModalOpen.set(true);
    this.isLoadingUsages.set(true);
    this.usagesList.set([]);

    this.discountService.getDiscountUsages(discount.discountId).subscribe({
      next: (res) => {
        this.isLoadingUsages.set(false);
        if (res.success && res.data) {
          this.usagesList.set(res.data);
        }
      },
      error: () => {
        this.isLoadingUsages.set(false);
      }
    });
  }

  closeUsagesModal(): void {
    this.isUsagesModalOpen.set(false);
    this.usagesList.set([]);
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0);
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '---';
    const d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
