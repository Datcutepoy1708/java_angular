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
import { CategoryResponse } from '../../../core/models/category.model';

type FormMode = 'create' | 'edit';

@Component({
  selector: 'app-category-manage',
  imports: [ReactiveFormsModule],
  templateUrl: './category-manage.component.html',
  styleUrl: './category-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoryManageComponent implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly fb = inject(FormBuilder);

  // ── State ─────────────────────────────────────────────────────
  readonly categories = signal<CategoryResponse[]>([]);
  readonly allCategories = signal<CategoryResponse[]>([]); // for parent dropdown
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly pageSize = signal(10);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deleting = signal<number | null>(null);

  readonly showPanel = signal(false);
  readonly formMode = signal<FormMode>('create');
  readonly editingId = signal<number | null>(null);
  readonly slugEditable = signal(false);

  readonly confirmDeleteId = signal<number | null>(null);
  readonly confirmDeleteName = signal('');
  readonly confirmDeleteChildCount = signal(0);

  readonly pageNumbers = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i)
  );

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

  loadAll(): void {
    this.loading.set(true);
    this.categoryService.getPaginated(this.currentPage(), this.pageSize()).subscribe({
      next: (res) => {
        this.categories.set(res.data.content);
        this.totalElements.set(res.data.totalElements);
        this.totalPages.set(res.data.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
    // Load flat list for parent dropdown
    this.categoryService.getAll().subscribe({
      next: (res) => this.allCategories.set(res.data)
    });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadAll();
  }

  // ── Panel ─────────────────────────────────────────────────────
  openCreatePanel(): void {
    this.formMode.set('create');
    this.editingId.set(null);
    this.slugEditable.set(false);
    this.form.reset({ status: 'active', sortOrder: 0 });
    this.form.get('slug')?.disable();
    this.showPanel.set(true);
  }

  openEditPanel(cat: CategoryResponse): void {
    this.formMode.set('edit');
    this.editingId.set(cat.categoryId);
    this.slugEditable.set(false);
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
    this.showPanel.set(true);
  }

  closePanel(): void {
    this.showPanel.set(false);
    this.slugEditable.set(false);
  }

  toggleSlugEdit(): void {
    const next = !this.slugEditable();
    this.slugEditable.set(next);
    next ? this.form.get('slug')?.enable() : this.form.get('slug')?.disable();
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const raw = this.form.getRawValue();
    const request = {
      name: raw.name.trim(),
      parentId: raw.parentId ? Number(raw.parentId) : null,
      iconUrl: raw.iconUrl?.trim() || null,
      description: raw.description?.trim() || null,
      sortOrder: raw.sortOrder ?? 0,
      status: raw.status,
      ...(this.formMode() === 'edit' && this.slugEditable() && raw.slug
        ? { slug: raw.slug.trim() } : {})
    };

    const op$ = this.formMode() === 'create'
      ? this.categoryService.create(request)
      : this.categoryService.update(this.editingId()!, request);

    op$.subscribe({
      next: () => { this.saving.set(false); this.closePanel(); this.currentPage.set(0); this.loadAll(); },
      error: () => this.saving.set(false)
    });
  }

  // ── Delete ────────────────────────────────────────────────────
  askDelete(cat: CategoryResponse): void {
    this.confirmDeleteId.set(cat.categoryId);
    this.confirmDeleteName.set(cat.name);
    this.confirmDeleteChildCount.set(0);
    // Check children count first
    this.categoryService.countChildren(cat.categoryId).subscribe({
      next: (res) => this.confirmDeleteChildCount.set(res.data.childrenCount)
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
    this.categoryService.delete(id).subscribe({
      next: () => {
        this.deleting.set(null);
        this.cancelDelete();
        if (this.categories().length === 1 && this.currentPage() > 0) {
          this.currentPage.update(p => p - 1);
        }
        this.loadAll();
      },
      error: () => this.deleting.set(null)
    });
  }

  // ── Helpers ───────────────────────────────────────────────────
  /** Filter out the editing category from parent dropdown to prevent circular ref */
  availableParents(): CategoryResponse[] {
    const editId = this.editingId();
    return this.allCategories().filter(c => c.categoryId !== editId);
  }

  hasError(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }

  getError(field: string): string {
    const ctrl = this.form.get(field);
    if (ctrl?.errors?.['required']) return 'Trường này là bắt buộc';
    if (ctrl?.errors?.['maxlength']) return `Tối đa ${ctrl.errors['maxlength'].requiredLength} ký tự`;
    return '';
  }
}
