import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { CategoryService } from '../../../core/services/category.service';
import { BrandService } from '../../../core/services/brand.service';
import { ProductResponse } from '../../../core/models/product.model';
import { CategoryResponse } from '../../../core/models/category.model';
import { BrandResponse } from '../../../core/models/brand.model';
import { BulkActionType } from '../../../core/models/bulk.model';

type ViewMode = 'active' | 'trash';

@Component({
  selector: 'app-product-manage',
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './product-manage.component.html',
  styleUrl: './product-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductManageComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly brandService = inject(BrandService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  // ── View Mode & Filter ────────────────────────────────────────
  readonly viewMode = signal<ViewMode>('active');

  // ── State ─────────────────────────────────────────────────────
  readonly products = signal<ProductResponse[]>([]);
  readonly categories = signal<CategoryResponse[]>([]);
  readonly brands = signal<BrandResponse[]>([]);

  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly pageSize = signal(10);
  readonly loading = signal(false);
  readonly deleting = signal<number | null>(null);
  readonly restoring = signal<number | null>(null);

  // ── Multi-select / Bulk ───────────────────────────────────────
  readonly selectedIds = signal<number[]>([]);
  readonly bulkLoading = signal(false);

  // ── Confirm delete ────────────────────────────────────────────
  readonly confirmDeleteId = signal<number | null>(null);
  readonly confirmDeleteName = signal('');

  // ── Filter Form ───────────────────────────────────────────────
  readonly filterForm: FormGroup = this.fb.group({
    keyword: [''],
    categoryId: [null],
    brandId: [null],
    status: [''],
  });

  // ── Computed ──────────────────────────────────────────────────
  readonly isAllSelected = computed(() => {
    const list = this.products();
    const sel = this.selectedIds();
    return list.length > 0 && list.every((p) => sel.includes(p.productId));
  });

  readonly isSomeSelected = computed(() => {
    const list = this.products();
    const sel = this.selectedIds();
    return sel.length > 0 && !list.every((p) => sel.includes(p.productId));
  });

  readonly pageNumbers = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i)
  );

  readonly paginationText = computed(() => {
    const total = this.totalElements();
    if (total === 0) return '0 - 0 trong 0';
    const start = this.currentPage() * this.pageSize() + 1;
    const end = Math.min((this.currentPage() + 1) * this.pageSize(), total);
    return `${start} - ${end} trong tổng số ${total} sản phẩm`;
  });

  // ── Lifecycle ─────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadFilterOptions();
    this.loadProducts();
  }

  loadFilterOptions(): void {
    this.categoryService.getAll().subscribe({
      next: (res) => this.categories.set(res.data),
    });
    this.brandService.getAll().subscribe({
      next: (res) => this.brands.set(res.data),
    });
  }

  switchTab(mode: ViewMode): void {
    if (this.viewMode() === mode) return;
    this.viewMode.set(mode);
    this.currentPage.set(0);
    this.selectedIds.set([]);
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading.set(true);
    this.selectedIds.set([]);

    if (this.viewMode() === 'trash') {
      this.productService.getTrash(this.currentPage(), this.pageSize()).subscribe({
        next: (res) => {
          this.products.set(res.data.content);
          this.totalElements.set(res.data.totalElements);
          this.totalPages.set(res.data.totalPages);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
    } else {
      const filter = this.filterForm.value;
      this.productService
        .getProducts({
          keyword: filter.keyword?.trim() || null,
          categoryId: filter.categoryId ? Number(filter.categoryId) : null,
          brandId: filter.brandId ? Number(filter.brandId) : null,
          status: filter.status || null,
          page: this.currentPage(),
          size: this.pageSize(),
        })
        .subscribe({
          next: (res) => {
            this.products.set(res.data.content);
            this.totalElements.set(res.data.totalElements);
            this.totalPages.set(res.data.totalPages);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        });
    }
  }

  onFilterChange(): void {
    this.currentPage.set(0);
    this.selectedIds.set([]);
    this.loadProducts();
  }

  resetFilter(): void {
    this.filterForm.reset({ keyword: '', categoryId: null, brandId: null, status: '' });
    this.currentPage.set(0);
    this.selectedIds.set([]);
    this.loadProducts();
  }

  onPageSizeChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.pageSize.set(Number(select.value));
    this.currentPage.set(0);
    this.selectedIds.set([]);
    this.loadProducts();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadProducts();
  }

  // ── Selection & Bulk Actions ──────────────────────────────────
  toggleSelectAll(): void {
    if (this.isAllSelected()) {
      this.selectedIds.set([]);
    } else {
      this.selectedIds.set(this.products().map((p) => p.productId));
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
      delete: 'chuyển vào thùng rác',
      restore: 'khôi phục',
    };

    if (!confirm(`Bạn có chắc muốn ${actionNames[action] || action} ${ids.length} sản phẩm đã chọn?`)) {
      return;
    }

    this.bulkLoading.set(true);
    this.productService.bulkAction({ ids, action }).subscribe({
      next: () => {
        this.bulkLoading.set(false);
        this.selectedIds.set([]);
        this.loadProducts();
      },
      error: () => {
        this.bulkLoading.set(false);
        alert('Thao tác hàng loạt thất bại.');
      },
    });
  }

  editProduct(productId: number): void {
    this.router.navigate(['/admin/products', productId, 'edit']);
  }

  // ── Delete / Restore ──────────────────────────────────────────
  askDelete(prod: ProductResponse): void {
    this.confirmDeleteId.set(prod.productId);
    this.confirmDeleteName.set(prod.name);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
    this.confirmDeleteName.set('');
  }

  confirmDelete(): void {
    const id = this.confirmDeleteId();
    if (id == null) return;
    this.deleting.set(id);
    this.productService.softDelete(id).subscribe({
      next: () => {
        this.deleting.set(null);
        this.cancelDelete();
        if (this.products().length === 1 && this.currentPage() > 0) {
          this.currentPage.update((p) => p - 1);
        }
        this.loadProducts();
      },
      error: () => this.deleting.set(null),
    });
  }

  restoreProduct(id: number): void {
    this.restoring.set(id);
    this.productService.restore(id).subscribe({
      next: () => {
        this.restoring.set(null);
        this.loadProducts();
      },
      error: () => {
        this.restoring.set(null);
        alert('Khôi phục sản phẩm thất bại.');
      },
    });
  }

  // ── Helpers ───────────────────────────────────────────────────
  getPriceDisplay(prod: ProductResponse): string {
    if (!prod.variants || prod.variants.length === 0) return '—';
    const prices = prod.variants.map((v) => Number(v.salePrice ?? v.price));
    const min = Math.min(...prices);
    const max = Math.max(...prices);
    if (min === max) {
      return `${min.toLocaleString('vi-VN')} ₫`;
    }
    return `${min.toLocaleString('vi-VN')} ₫ - ${max.toLocaleString('vi-VN')} ₫`;
  }

  getStatusClass(status: string): string {
    return status?.toLowerCase() || 'inactive';
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      active: 'Đang bán',
      inactive: 'Tạm ẩn',
      discontinued: 'Ngừng kinh doanh',
    };
    return map[status?.toLowerCase()] ?? status;
  }
}
