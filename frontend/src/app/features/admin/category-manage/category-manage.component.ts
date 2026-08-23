import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CategoryService } from '../../../core/services/category.service';
import { UploadService } from '../../../core/services/upload.service';
import { CategoryResponse } from '../../../core/models/category.model';
import { BulkActionType } from '../../../core/models/bulk.model';

type FormMode = 'create' | 'edit';
type ViewMode = 'active' | 'trash';

@Component({
  selector: 'app-category-manage',
  imports: [ReactiveFormsModule],
  templateUrl: './category-manage.component.html',
  styleUrl: './category-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoryManageComponent implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly uploadService = inject(UploadService);
  private readonly fb = inject(FormBuilder);

  // ── View Mode & Filter ────────────────────────────────────────
  readonly viewMode = signal<ViewMode>('active');
  readonly filterKeyword = signal('');

  // ── State ─────────────────────────────────────────────────────
  readonly categories = signal<CategoryResponse[]>([]);
  readonly allCategories = signal<CategoryResponse[]>([]); // for parent dropdown
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly pageSize = signal(10);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly uploadingIcon = signal(false);
  readonly deleting = signal<number | null>(null);
  readonly restoring = signal<number | null>(null);

  // ── Multi-select / Bulk ───────────────────────────────────────
  readonly selectedIds = signal<number[]>([]);
  readonly bulkLoading = signal(false);

  // ── Modal dialog state (Centered Dialog) ──────────────────────
  readonly showModal = signal(false);
  readonly formMode = signal<FormMode>('create');
  readonly editingId = signal<number | null>(null);
  readonly slugEditable = signal(false);

  // Icon preview URL & Tab selection ('upload' | 'url')
  readonly iconPreview = signal<string | null>(null);
  readonly iconInputMode = signal<'upload' | 'url'>('upload');

  // ── Confirm delete dialog state ───────────────────────────────
  readonly confirmDeleteId = signal<number | null>(null);
  readonly confirmDeleteName = signal('');
  readonly confirmDeleteChildCount = signal(0);

  // ── Computed ──────────────────────────────────────────────────
  readonly isAllSelected = computed(() => {
    const list = this.categories();
    const sel = this.selectedIds();
    return list.length > 0 && list.every((c) => sel.includes(c.categoryId));
  });

  readonly isSomeSelected = computed(() => {
    const list = this.categories();
    const sel = this.selectedIds();
    return sel.length > 0 && !list.every((c) => sel.includes(c.categoryId));
  });

  readonly pageNumbers = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i)
  );

  readonly paginationText = computed(() => {
    const total = this.totalElements();
    if (total === 0) return '0 - 0 trong 0';
    const start = this.currentPage() * this.pageSize() + 1;
    const end = Math.min((this.currentPage() + 1) * this.pageSize(), total);
    return `${start} - ${end} trong tổng số ${total} danh mục`;
  });

  // ── Form ──────────────────────────────────────────────────────
  readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    parentId: [null],
    iconUrl: ['', Validators.maxLength(500)],
    description: ['', Validators.maxLength(500)],
    sortOrder: [0],
    status: ['active', Validators.required],
    slug: [{ value: '', disabled: true }, Validators.maxLength(180)],
  });

  // ── Lifecycle ─────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadAll();
  }

  // ── Tab & Filter Handlers ─────────────────────────────────────
  switchTab(mode: ViewMode): void {
    if (this.viewMode() === mode) return;
    this.viewMode.set(mode);
    this.currentPage.set(0);
    this.selectedIds.set([]);
    this.loadAll();
  }

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.filterKeyword.set(value);
    this.currentPage.set(0);
    this.selectedIds.set([]);
    this.loadAll();
  }

  resetFilter(): void {
    this.filterKeyword.set('');
    this.currentPage.set(0);
    this.selectedIds.set([]);
    this.loadAll();
  }

  // ── Data Loading ──────────────────────────────────────────────
  loadAll(): void {
    this.loading.set(true);
    this.selectedIds.set([]);

    if (this.viewMode() === 'trash') {
      this.categoryService.getTrash(this.currentPage(), this.pageSize()).subscribe({
        next: (res) => {
          this.categories.set(res.data.content);
          this.totalElements.set(res.data.totalElements);
          this.totalPages.set(res.data.totalPages);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
    } else {
      this.categoryService
        .getPaginated(this.currentPage(), this.pageSize(), this.filterKeyword())
        .subscribe({
          next: (res) => {
            this.categories.set(res.data.content);
            this.totalElements.set(res.data.totalElements);
            this.totalPages.set(res.data.totalPages);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        });
    }

    // Load active flat list for parent dropdown
    this.categoryService.getAll().subscribe({
      next: (res) => this.allCategories.set(res.data),
    });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadAll();
  }

  // ── Selection & Bulk Actions ──────────────────────────────────
  toggleSelectAll(): void {
    if (this.isAllSelected()) {
      this.selectedIds.set([]);
    } else {
      this.selectedIds.set(this.categories().map((c) => c.categoryId));
    }
  }

  toggleSelectItem(id: number): void {
    this.selectedIds.update((current) =>
      current.includes(id) ? current.filter((x) => x !== id) : [...current, id]
    );
  }

  executeBulkAction(action: BulkActionType): void {
    const ids = this.selectedIds();
    if (ids.length === 0) return;

    const actionNames: Record<string, string> = {
      delete: 'chuyển vào thùng rác (bao gồm toàn bộ danh mục con)',
      restore: 'khôi phục (bao gồm toàn bộ danh mục con)',
    };

    if (
      !confirm(
        `Bạn có chắc muốn ${actionNames[action] || action} ${ids.length} danh mục đã chọn?`
      )
    ) {
      return;
    }

    this.bulkLoading.set(true);
    this.categoryService.bulkAction({ ids, action }).subscribe({
      next: () => {
        this.bulkLoading.set(false);
        this.selectedIds.set([]);
        this.loadAll();
      },
      error: () => {
        this.bulkLoading.set(false);
        alert('Thao tác hàng loạt thất bại. Vui lòng thử lại.');
      },
    });
  }

  // ── Modal / Form ──────────────────────────────────────────────
  openCreateModal(): void {
    this.formMode.set('create');
    this.editingId.set(null);
    this.slugEditable.set(false);
    this.iconPreview.set(null);
    this.iconInputMode.set('upload');
    this.form.reset({ status: 'active', sortOrder: 0 });
    this.form.get('slug')?.disable();
    this.showModal.set(true);
  }

  // Alias for backward compatibility with specs
  openCreatePanel(): void {
    this.openCreateModal();
  }

  openEditModal(cat: CategoryResponse): void {
    this.formMode.set('edit');
    this.editingId.set(cat.categoryId);
    this.slugEditable.set(false);
    this.iconPreview.set(cat.iconUrl ?? null);
    this.iconInputMode.set(cat.iconUrl ? 'url' : 'upload');
    this.form.reset();
    this.form.get('slug')?.disable();
    this.form.patchValue({
      name: cat.name,
      parentId: cat.parentId ?? null,
      iconUrl: cat.iconUrl ?? '',
      description: cat.description ?? '',
      sortOrder: cat.sortOrder ?? 0,
      status: cat.status,
      slug: cat.slug,
    });
    this.showModal.set(true);
  }

  // Alias for backward compatibility with specs
  openEditPanel(cat: CategoryResponse): void {
    this.openEditModal(cat);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.form.reset({ status: 'active', sortOrder: 0 });
    this.iconPreview.set(null);
    this.slugEditable.set(false);
  }

  // Alias for backward compatibility with specs
  closePanel(): void {
    this.closeModal();
  }

  // Alias property for spec backward compatibility
  get showPanel() {
    return this.showModal;
  }

  toggleSlugEdit(): void {
    const next = !this.slugEditable();
    this.slugEditable.set(next);
    next ? this.form.get('slug')?.enable() : this.form.get('slug')?.disable();
  }

  setIconMode(mode: 'upload' | 'url'): void {
    this.iconInputMode.set(mode);
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

  private handleFileUpload(file: File): void {
    if (!file.type.startsWith('image/')) {
      alert('Vui lòng chọn một file ảnh hoặc icon hợp lệ (PNG, JPG, WEBP, SVG, GIF)');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      alert('Dung lượng ảnh tối đa là 5MB');
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      this.iconPreview.set(e.target?.result as string);
    };
    reader.readAsDataURL(file);

    this.uploadingIcon.set(true);
    this.uploadService.uploadImage(file).subscribe({
      next: (url) => {
        this.form.patchValue({ iconUrl: url });
        this.iconPreview.set(url);
        this.uploadingIcon.set(false);
      },
      error: () => {
        alert('Tải ảnh icon lên máy chủ thất bại. Bạn có thể thử lại hoặc nhập link URL trực tiếp.');
        this.uploadingIcon.set(false);
      },
    });
  }

  onUrlIconChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const url = input.value.trim();
    this.form.patchValue({ iconUrl: url });
    this.iconPreview.set(url || null);
  }

  removeIcon(): void {
    this.form.patchValue({ iconUrl: '' });
    this.iconPreview.set(null);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const raw = this.form.getRawValue();
    const request = {
      name: raw.name.trim(),
      parentId: raw.parentId ? Number(raw.parentId) : null,
      iconUrl: raw.iconUrl?.trim() || null,
      description: raw.description?.trim() || null,
      sortOrder: raw.sortOrder != null ? Number(raw.sortOrder) : 0,
      status: raw.status || 'active',
      ...(this.formMode() === 'edit' && this.slugEditable() && raw.slug
        ? { slug: raw.slug.trim() }
        : {}),
    };

    const op$ =
      this.formMode() === 'create'
        ? this.categoryService.create(request)
        : this.categoryService.update(this.editingId()!, request);

    op$.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeModal();
        this.currentPage.set(0);
        this.loadAll();
      },
      error: () => this.saving.set(false),
    });
  }

  // ── Delete / Restore ──────────────────────────────────────────
  askDelete(cat: CategoryResponse): void {
    this.confirmDeleteId.set(cat.categoryId);
    this.confirmDeleteName.set(cat.name);
    this.confirmDeleteChildCount.set(0);
    // Check children count for cascade warning
    this.categoryService.countChildren(cat.categoryId).subscribe({
      next: (res) => this.confirmDeleteChildCount.set(res.data.childrenCount),
    });
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
    this.confirmDeleteName.set('');
    this.confirmDeleteChildCount.set(0);
  }

  confirmDelete(): void {
    const id = this.confirmDeleteId();
    if (id == null) return;
    this.deleting.set(id);
    this.categoryService.softDelete(id).subscribe({
      next: () => {
        this.deleting.set(null);
        this.cancelDelete();
        if (this.categories().length === 1 && this.currentPage() > 0) {
          this.currentPage.update((p) => p - 1);
        }
        this.loadAll();
      },
      error: () => this.deleting.set(null),
    });
  }

  restoreCategory(id: number): void {
    this.restoring.set(id);
    this.categoryService.restore(id).subscribe({
      next: () => {
        this.restoring.set(null);
        this.loadAll();
      },
      error: () => {
        this.restoring.set(null);
        alert('Khôi phục danh mục thất bại.');
      },
    });
  }

  // ── Helpers ───────────────────────────────────────────────────
  availableParents(): CategoryResponse[] {
    const editId = this.editingId();
    return this.allCategories().filter((c) => c.categoryId !== editId);
  }

  hasError(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }

  getError(field: string): string {
    const ctrl = this.form.get(field);
    if (ctrl?.errors?.['required']) return 'Trường này là bắt buộc';
    if (ctrl?.errors?.['maxlength'])
      return `Tối đa ${ctrl.errors['maxlength'].requiredLength} ký tự`;
    return '';
  }
}
