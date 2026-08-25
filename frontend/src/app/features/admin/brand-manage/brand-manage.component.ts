import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
  computed,
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { BrandService } from '../../../core/services/brand.service';
import { UploadService } from '../../../core/services/upload.service';
import { BrandResponse } from '../../../core/models/brand.model';
import { BulkActionType } from '../../../core/models/bulk.model';

import {
  ConfirmDialogComponent,
  PaginationComponent,
  ImageUploadComponent,
} from '../../../shared';

type FormMode = 'create' | 'edit';
type ViewMode = 'active' | 'trash';

@Component({
  selector: 'app-brand-manage',
  imports: [
    ReactiveFormsModule,
    ConfirmDialogComponent,
    PaginationComponent,
    ImageUploadComponent,
  ],
  templateUrl: './brand-manage.component.html',
  styleUrl: './brand-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandManageComponent implements OnInit {
  private readonly brandService = inject(BrandService);
  private readonly uploadService = inject(UploadService);
  private readonly fb = inject(FormBuilder);

  // ── View Mode & Filter ────────────────────────────────────────
  readonly viewMode = signal<ViewMode>('active');
  readonly filterKeyword = signal('');
  readonly filterStatus = signal('');

  // ── State ─────────────────────────────────────────────────────
  readonly brands = signal<BrandResponse[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly pageSize = signal(10);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly uploadingLogo = signal(false);
  readonly deleting = signal<number | null>(null);
  readonly restoring = signal<number | null>(null);

  // ── Multi-select / Bulk ───────────────────────────────────────
  readonly selectedIds = signal<number[]>([]);
  readonly bulkLoading = signal(false);

  // ── Modal dialog state ────────────────────────────────────────
  readonly showModal = signal(false);
  readonly formMode = signal<FormMode>('create');
  readonly editingId = signal<number | null>(null);

  // Logo preview URL & Tab selection ('upload' | 'url')
  readonly logoPreview = signal<string | null>(null);
  readonly logoInputMode = signal<'upload' | 'url'>('upload');

  // Slug edit toggle
  readonly slugEditable = signal(false);

  // Confirm delete dialog state
  readonly confirmDeleteId = signal<number | null>(null);
  readonly confirmDeleteName = signal('');

  // ── Computed ──────────────────────────────────────────────────
  readonly isAllSelected = computed(() => {
    const list = this.brands();
    const sel = this.selectedIds();
    return list.length > 0 && list.every((b) => sel.includes(b.brandId));
  });

  readonly isSomeSelected = computed(() => {
    const list = this.brands();
    const sel = this.selectedIds();
    return sel.length > 0 && !list.every((b) => sel.includes(b.brandId));
  });

  readonly pageNumbers = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i)
  );

  readonly paginationText = computed(() => {
    const total = this.totalElements();
    if (total === 0) return '0 - 0 trong 0';
    const start = this.currentPage() * this.pageSize() + 1;
    const end = Math.min((this.currentPage() + 1) * this.pageSize(), total);
    return `${start} - ${end} trong tổng số ${total} thương hiệu`;
  });

  // ── Form ──────────────────────────────────────────────────────
  readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    logoUrl: ['', Validators.maxLength(500)],
    country: ['', Validators.maxLength(100)],
    description: ['', Validators.maxLength(2000)],
    status: ['active', Validators.required],
    slug: [{ value: '', disabled: true }, Validators.maxLength(180)],
  });

  // ── Lifecycle ─────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadBrands();
  }

  // ── Tab & Filter Handlers ─────────────────────────────────────
  switchTab(mode: ViewMode): void {
    if (this.viewMode() === mode) return;
    this.viewMode.set(mode);
    this.currentPage.set(0);
    this.selectedIds.set([]);
    this.loadBrands();
  }

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.filterKeyword.set(value);
    this.currentPage.set(0);
    this.selectedIds.set([]);
    this.loadBrands();
  }

  onStatusChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.filterStatus.set(value);
    this.currentPage.set(0);
    this.selectedIds.set([]);
    this.loadBrands();
  }

  resetFilter(): void {
    this.filterKeyword.set('');
    this.filterStatus.set('');
    this.currentPage.set(0);
    this.selectedIds.set([]);
    this.loadBrands();
  }

  // ── Data Loading ──────────────────────────────────────────────
  loadBrands(): void {
    this.loading.set(true);
    this.selectedIds.set([]);

    if (this.viewMode() === 'trash') {
      this.brandService.getTrash(this.currentPage(), this.pageSize()).subscribe({
        next: (res) => {
          this.brands.set(res.data.content);
          this.totalElements.set(res.data.totalElements);
          this.totalPages.set(res.data.totalPages);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
    } else {
      this.brandService.getPaginated(
        this.currentPage(),
        this.pageSize(),
        this.filterKeyword(),
        this.filterStatus()
      ).subscribe({
        next: (res) => {
          this.brands.set(res.data.content);
          this.totalElements.set(res.data.totalElements);
          this.totalPages.set(res.data.totalPages);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
    }
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadBrands();
  }

  // ── Selection & Bulk Actions ──────────────────────────────────
  toggleSelectAll(): void {
    if (this.isAllSelected()) {
      this.selectedIds.set([]);
    } else {
      this.selectedIds.set(this.brands().map((b) => b.brandId));
    }
  }

  toggleSelectItem(id: number): void {
    this.selectedIds.update((current) =>
      current.includes(id) ? current.filter((x) => x !== id) : [...current, id]
    );
  }

  // ── Bulk Confirm Modal State ─────────────────────────────────
  readonly showBulkConfirmModal = signal(false);
  readonly pendingBulkAction = signal<BulkActionType>('delete');

  openBulkConfirmModal(action: BulkActionType): void {
    if (this.selectedIds().length === 0) return;
    this.pendingBulkAction.set(action);
    this.showBulkConfirmModal.set(true);
  }

  closeBulkConfirmModal(): void {
    this.showBulkConfirmModal.set(false);
  }

  confirmBulkAction(): void {
    const ids = this.selectedIds();
    const action = this.pendingBulkAction();
    if (ids.length === 0) return;

    this.bulkLoading.set(true);
    this.brandService.bulkAction({ ids, action }).subscribe({
      next: () => {
        this.bulkLoading.set(false);
        this.closeBulkConfirmModal();
        this.selectedIds.set([]);
        this.loadBrands();
      },
      error: (err) => {
        this.bulkLoading.set(false);
        const msg = err.error?.message || err.message || 'Thao tác hàng loạt thất bại. Vui lòng thử lại.';
        alert(msg);
      },
    });
  }

  // Alias for backward compatibility
  executeBulkAction(action: BulkActionType): void {
    this.openBulkConfirmModal(action);
  }

  // ── Modal / Form ──────────────────────────────────────────────
  openCreateModal(): void {
    this.formMode.set('create');
    this.editingId.set(null);
    this.slugEditable.set(false);
    this.logoPreview.set(null);
    this.logoInputMode.set('upload');
    this.form.reset({ status: 'active' });
    this.form.get('slug')?.disable();
    this.showModal.set(true);
  }

  openEditModal(brand: BrandResponse): void {
    this.formMode.set('edit');
    this.editingId.set(brand.brandId);
    this.slugEditable.set(false);
    this.logoPreview.set(brand.logoUrl ?? null);
    this.logoInputMode.set(brand.logoUrl ? 'url' : 'upload');
    this.form.reset();
    this.form.get('slug')?.disable();
    this.form.patchValue({
      name: brand.name,
      logoUrl: brand.logoUrl ?? '',
      country: brand.country ?? '',
      description: brand.description ?? '',
      status: brand.status ?? 'active',
      slug: brand.slug,
    });
    this.showModal.set(true);
  }

  private isMouseDownOnBackdrop = false;

  onBackdropMouseDown(event: MouseEvent): void {
    this.isMouseDownOnBackdrop = event.target === event.currentTarget;
  }

  onBackdropMouseUp(event: MouseEvent): void {
    if (this.isMouseDownOnBackdrop && event.target === event.currentTarget) {
      this.closeModal();
    }
    this.isMouseDownOnBackdrop = false;
  }

  closeModal(): void {
    this.showModal.set(false);
    this.form.reset({ status: 'active' });
    this.logoPreview.set(null);
    this.slugEditable.set(false);
    this.isMouseDownOnBackdrop = false;
  }

  toggleSlugEdit(): void {
    const editable = !this.slugEditable();
    this.slugEditable.set(editable);
    if (editable) {
      this.form.get('slug')?.enable();
    } else {
      this.form.get('slug')?.disable();
    }
  }

  setLogoMode(mode: 'upload' | 'url'): void {
    this.logoInputMode.set(mode);
  }

  // ── File Upload Handler ───────────────────────────────────────
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    this.handleFileUpload(file);
  }

  onFileDropped(event: DragEvent): void {
    event.preventDefault();
    if (!event.dataTransfer?.files || event.dataTransfer.files.length === 0) return;
    const file = event.dataTransfer.files[0];
    this.handleFileUpload(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  handleFileUpload(file: File): void {
    if (!file.type.startsWith('image/')) {
      alert('Vui lòng chọn một file ảnh hợp lệ (PNG, JPG, WEBP, SVG, GIF)');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      alert('Dung lượng ảnh tối đa là 5MB');
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      this.logoPreview.set(e.target?.result as string);
    };
    reader.readAsDataURL(file);

    this.uploadingLogo.set(true);
    this.uploadService.uploadImage(file).subscribe({
      next: (url) => {
        this.form.patchValue({ logoUrl: url });
        this.logoPreview.set(url);
        this.uploadingLogo.set(false);
      },
      error: () => {
        alert('Tải ảnh lên máy chủ thất bại. Bạn có thể thử lại hoặc nhập link ảnh trực tiếp.');
        this.uploadingLogo.set(false);
      },
    });
  }

  onUrlLogoUpdate(url: string): void {
    this.form.patchValue({ logoUrl: url });
    this.logoPreview.set(url || null);
  }

  onUrlLogoChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const url = input.value.trim();
    this.onUrlLogoUpdate(url);
  }

  removeLogo(): void {
    this.form.patchValue({ logoUrl: '' });
    this.logoPreview.set(null);
  }

  handleLogoError(event: Event, slug?: string): void {
    const target = event.target as HTMLImageElement;
    const fallback = slug ? `/assets/brands/${slug}.svg` : '';
    if (target && fallback && !target.src.endsWith(fallback)) {
      target.src = fallback;
    }
  }

  // ── Save ──────────────────────────────────────────────────────
  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const raw = this.form.getRawValue();
    const request = {
      name: raw.name.trim(),
      logoUrl: raw.logoUrl?.trim() || null,
      country: raw.country?.trim() || null,
      description: raw.description?.trim() || null,
      status: raw.status || 'active',
      ...(this.formMode() === 'edit' && this.slugEditable() && raw.slug
        ? { slug: raw.slug.trim() }
        : {}),
    };

    const op$ = this.formMode() === 'create'
      ? this.brandService.create(request)
      : this.brandService.update(this.editingId()!, request);

    op$.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeModal();
        this.currentPage.set(0);
        this.loadBrands();
      },
      error: () => this.saving.set(false),
    });
  }

  // ── Delete / Restore Single ───────────────────────────────────
  askDelete(brand: BrandResponse): void {
    this.confirmDeleteId.set(brand.brandId);
    this.confirmDeleteName.set(brand.name);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
    this.confirmDeleteName.set('');
  }

  confirmDelete(): void {
    const id = this.confirmDeleteId();
    if (id == null) return;
    this.deleting.set(id);
    this.brandService.softDelete(id).subscribe({
      next: () => {
        this.deleting.set(null);
        this.confirmDeleteId.set(null);
        this.confirmDeleteName.set('');
        if (this.brands().length === 1 && this.currentPage() > 0) {
          this.currentPage.update((p) => p - 1);
        }
        this.loadBrands();
      },
      error: () => this.deleting.set(null),
    });
  }

  restoreBrand(id: number): void {
    this.restoring.set(id);
    this.brandService.restore(id).subscribe({
      next: () => {
        this.restoring.set(null);
        this.loadBrands();
      },
      error: () => {
        this.restoring.set(null);
        alert('Khôi phục thương hiệu thất bại.');
      },
    });
  }

  // ── Helpers ───────────────────────────────────────────────────
  hasError(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }

  getError(field: string): string {
    const ctrl = this.form.get(field);
    if (ctrl?.errors?.['required']) return 'Vui lòng nhập trường này';
    if (ctrl?.errors?.['maxlength']) return `Tối đa ${ctrl.errors['maxlength'].requiredLength} ký tự`;
    return '';
  }
}
