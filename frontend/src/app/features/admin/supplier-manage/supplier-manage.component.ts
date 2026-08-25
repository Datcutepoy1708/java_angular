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
import { SupplierService } from '../../../core/services/supplier.service';
import { SupplierRequest, SupplierResponse } from '../../../core/models/supplier.model';
import { PaginationComponent } from '../../../shared';

@Component({
  selector: 'app-supplier-manage',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PaginationComponent],
  templateUrl: './supplier-manage.component.html',
  styleUrl: './supplier-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SupplierManageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly supplierService = inject(SupplierService);

  // ── State Signals ─────────────────────────────────────────────
  readonly suppliers = signal<SupplierResponse[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly isDeleting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Pagination & Filtering
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly searchKeyword = signal<string>('');
  readonly filterStatus = signal<string>('all');

  // Modal State
  readonly isModalOpen = signal<boolean>(false);
  readonly isEditing = signal<boolean>(false);
  readonly selectedSupplierId = signal<number | null>(null);

  // Confirm Delete Dialog
  readonly isConfirmDeleteOpen = signal<boolean>(false);
  readonly deletingSupplier = signal<SupplierResponse | null>(null);

  // Reactive Form
  readonly supplierForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    contactName: ['', [Validators.maxLength(150)]],
    phone: ['', [Validators.pattern('^(84|0[3|5|7|8|9])+([0-9]{8})$')]],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    address: ['', [Validators.maxLength(300)]],
    status: ['active', [Validators.required]]
  });

  // Computed Metrics
  readonly activeCount = computed(() =>
    this.suppliers().filter(s => s.status === 'active').length
  );
  readonly totalLinkedProducts = computed(() =>
    this.suppliers().reduce((sum, s) => sum + (s.productCount || 0), 0)
  );

  ngOnInit(): void {
    this.loadSuppliers();
  }

  loadSuppliers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.supplierService
      .getSuppliersPaginated(
        this.currentPage(),
        this.pageSize(),
        this.searchKeyword(),
        this.filterStatus()
      )
      .subscribe({
        next: res => {
          this.suppliers.set(res.data.content || []);
          this.totalElements.set(res.data.totalElements);
          this.totalPages.set(res.data.totalPages);
          this.isLoading.set(false);
        },
        error: err => {
          this.errorMessage.set(err.error?.message || 'Không thể tải danh sách nhà cung cấp');
          this.isLoading.set(false);
        }
      });
  }

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchKeyword.set(value);
    this.currentPage.set(0);
    this.loadSuppliers();
  }

  onFilterStatus(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.filterStatus.set(value);
    this.currentPage.set(0);
    this.loadSuppliers();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadSuppliers();
  }

  openCreateModal(): void {
    this.isEditing.set(false);
    this.selectedSupplierId.set(null);
    this.supplierForm.reset({
      name: '',
      contactName: '',
      phone: '',
      email: '',
      address: '',
      status: 'active'
    });
    this.errorMessage.set(null);
    this.isModalOpen.set(true);
  }

  openEditModal(supplier: SupplierResponse): void {
    this.isEditing.set(true);
    this.selectedSupplierId.set(supplier.supplierId);
    this.supplierForm.patchValue({
      name: supplier.name,
      contactName: supplier.contactName || '',
      phone: supplier.phone || '',
      email: supplier.email || '',
      address: supplier.address || '',
      status: supplier.status || 'active'
    });
    this.errorMessage.set(null);
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.supplierForm.reset();
  }

  saveSupplier(): void {
    if (this.supplierForm.invalid) {
      this.supplierForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set(null);

    const formVal = this.supplierForm.value;
    const req: SupplierRequest = {
      name: formVal.name?.trim(),
      contactName: formVal.contactName?.trim() || null,
      phone: formVal.phone?.trim() || null,
      email: formVal.email?.trim() || null,
      address: formVal.address?.trim() || null,
      status: formVal.status
    };

    if (this.isEditing() && this.selectedSupplierId()) {
      this.supplierService.updateSupplier(this.selectedSupplierId()!, req).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeModal();
          this.showSuccess('Cập nhật thông tin nhà cung cấp thành công');
          this.loadSuppliers();
        },
        error: err => {
          this.isSaving.set(false);
          this.errorMessage.set(err.error?.message || 'Có lỗi xảy ra khi cập nhật');
        }
      });
    } else {
      this.supplierService.createSupplier(req).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeModal();
          this.showSuccess('Thêm nhà cung cấp mới thành công');
          this.loadSuppliers();
        },
        error: err => {
          this.isSaving.set(false);
          this.errorMessage.set(err.error?.message || 'Có lỗi xảy ra khi tạo mới');
        }
      });
    }
  }

  openConfirmDelete(supplier: SupplierResponse): void {
    this.deletingSupplier.set(supplier);
    this.isConfirmDeleteOpen.set(true);
  }

  closeConfirmDelete(): void {
    this.deletingSupplier.set(null);
    this.isConfirmDeleteOpen.set(false);
  }

  confirmDelete(): void {
    const s = this.deletingSupplier();
    if (!s) return;

    this.isDeleting.set(true);
    this.supplierService.deleteSupplier(s.supplierId).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.closeConfirmDelete();
        this.showSuccess(`Đã xóa nhà cung cấp "${s.name}" thành công`);
        this.loadSuppliers();
      },
      error: err => {
        this.isDeleting.set(false);
        this.closeConfirmDelete();
        this.errorMessage.set(err.error?.message || 'Không thể xóa nhà cung cấp');
      }
    });
  }

  private showSuccess(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => {
      this.successMessage.set(null);
    }, 4000);
  }
}
