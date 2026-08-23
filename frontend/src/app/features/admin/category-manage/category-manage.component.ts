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
import { CategoryRequest, CategoryResponse } from '../../../core/models/category.model';
import { BulkActionType } from '../../../core/models/bulk.model';
import {
  ConfirmDialogComponent,
  PaginationComponent,
  ImageUploadComponent,
} from '../../../shared';

type FormMode = 'create' | 'edit';
type ViewMode = 'active' | 'trash';

export interface FlatCategoryNode {
  category: CategoryResponse;
  level: number;
  hasChildren: boolean;
  childCount: number;
  isExpanded: boolean;
}

@Component({
  selector: 'app-category-manage',
  imports: [
    ReactiveFormsModule,
    ConfirmDialogComponent,
    PaginationComponent,
    ImageUploadComponent,
  ],
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
  readonly allCategories = signal<CategoryResponse[]>([]); // for parent dropdown & tree
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly pageSize = signal(50); // Larger page size for tree view
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly uploadingIcon = signal(false);
  readonly deleting = signal<number | null>(null);
  readonly restoring = signal<number | null>(null);

  // ── Collapsible Tree State ────────────────────────────────────
  readonly expandedIds = signal<number[]>([]);

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

  // ── Collapsed Tree Computed Nodes ─────────────────────────────
  readonly displayedNodes = computed<FlatCategoryNode[]>(() => {
    const items = this.categories();
    if (items.length === 0) return [];

    // Map children by parentId
    const childrenMap = new Map<number, CategoryResponse[]>();
    const allIds = new Set(items.map((c) => c.categoryId));

    for (const item of items) {
      if (item.parentId != null && allIds.has(item.parentId)) {
        const list = childrenMap.get(item.parentId) || [];
        list.push(item);
        childrenMap.set(item.parentId, list);
      }
    }

    // Sort children by sortOrder then name
    for (const list of childrenMap.values()) {
      list.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.name.localeCompare(b.name));
    }

    // Root items: parentId is null OR parent not in the current items list
    const roots = items.filter(
      (c) => c.parentId == null || !allIds.has(c.parentId)
    );
    roots.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.name.localeCompare(b.name));

    const expanded = new Set(this.expandedIds());
    const hasSearch = this.filterKeyword().trim().length > 0;

    const result: FlatCategoryNode[] = [];

    const traverse = (node: CategoryResponse, level: number) => {
      const children = childrenMap.get(node.categoryId) || [];
      const hasChildren = children.length > 0;
      // Auto-expand all matching nodes when searching
      const isExpanded = hasSearch ? true : expanded.has(node.categoryId);

      result.push({
        category: node,
        level,
        hasChildren,
        childCount: children.length,
        isExpanded,
      });

      if (hasChildren && isExpanded) {
        for (const child of children) {
          traverse(child, level + 1);
        }
      }
    };

    for (const root of roots) {
      traverse(root, 0);
    }

    return result;
  });

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
    description: ['', Validators.maxLength(2000)],
    sortOrder: [0],
    status: ['active', Validators.required],
    slug: [{ value: '', disabled: true }, Validators.maxLength(180)],
  });

  // ── Lifecycle ─────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadAll();
  }

  // ── Tree Expand / Collapse Handlers ───────────────────────────
  toggleExpand(id: number, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.expandedIds.update((current) =>
      current.includes(id) ? current.filter((x) => x !== id) : [...current, id]
    );
  }

  expandAll(): void {
    const parentIdsWithChildren = this.categories()
      .filter((c) => this.categories().some((child) => child.parentId === c.categoryId))
      .map((c) => c.categoryId);
    this.expandedIds.set(parentIdsWithChildren);
  }

  collapseAll(): void {
    this.expandedIds.set([]);
  }

  // ── Tab & Filter Handlers ─────────────────────────────────────
  switchTab(mode: ViewMode): void {
    if (this.viewMode() === mode) return;
    this.viewMode.set(mode);
    this.currentPage.set(0);
    this.selectedIds.set([]);
    this.expandedIds.set([]);
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
      error: (err) => console.warn('Could not load allCategories for dropdown:', err),
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
    this.categoryService.bulkAction({ ids, action }).subscribe({
      next: () => {
        this.bulkLoading.set(false);
        this.closeBulkConfirmModal();
        this.selectedIds.set([]);
        this.loadAll();
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
    this.iconPreview.set(null);
    this.iconInputMode.set('upload');
    this.uploadingIcon.set(false);
    this.saving.set(false);
    this.form.reset({
      name: '',
      parentId: null,
      iconUrl: '',
      description: '',
      sortOrder: 0,
      status: 'active',
      slug: '',
    });
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
    this.uploadingIcon.set(false);
    this.saving.set(false);
    this.form.reset({
      name: cat.name || '',
      parentId: cat.parentId ?? null,
      iconUrl: cat.iconUrl ?? '',
      description: cat.description ?? '',
      sortOrder: cat.sortOrder ?? 0,
      status: cat.status || 'active',
      slug: cat.slug || '',
    });
    this.form.get('slug')?.disable();
    this.showModal.set(true);
  }

  // Alias for backward compatibility with specs
  openEditPanel(cat: CategoryResponse): void {
    this.openEditModal(cat);
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
    this.form.reset({ status: 'active', sortOrder: 0 });
    this.iconPreview.set(null);
    this.slugEditable.set(false);
    this.isMouseDownOnBackdrop = false;
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

  handleFileUpload(file: File): void {
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

  onUrlIconUpdate(url: string): void {
    this.form.patchValue({ iconUrl: url });
    this.iconPreview.set(url || null);
  }

  removeIcon(): void {
    this.iconPreview.set(null);
    this.form.patchValue({ iconUrl: '' });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      const invalidControls: string[] = [];
      for (const name of Object.keys(this.form.controls)) {
        if (this.form.controls[name].invalid) {
          invalidControls.push(name);
        }
      }
      alert('Vui lòng điền đầy đủ và chính xác các thông tin: ' + invalidControls.join(', '));
      return;
    }

    this.saving.set(true);
    const raw = this.form.getRawValue();
    const request: CategoryRequest = {
      name: raw.name.trim(),
      parentId: raw.parentId != null ? Number(raw.parentId) : null,
      iconUrl: raw.iconUrl?.trim() || null,
      description: raw.description?.trim() || null,
      sortOrder: raw.sortOrder != null ? Number(raw.sortOrder) : 0,
      status: (raw.status as 'active' | 'inactive') || 'active',
      slug: raw.slug?.trim() || null,
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
      error: (err) => {
        this.saving.set(false);
        let msg = err.error?.message || err.message || 'Lưu danh mục thất bại. Vui lòng thử lại.';
        if (err.error?.data && typeof err.error.data === 'object') {
          const details = Object.entries(err.error.data)
            .map(([field, error]) => `• ${field}: ${error}`)
            .join('\n');
          msg += `\n\nChi tiết:\n${details}`;
        }
        alert(msg);
      },
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
    if (ctrl?.errors?.['maxlength']) {
      const max = ctrl.errors['maxlength'].requiredLength;
      const actual = ctrl.errors['maxlength'].actualLength;
      return `Tối đa ${max} ký tự (hiện tại có ${actual} ký tự)`;
    }
    return '';
  }
}
