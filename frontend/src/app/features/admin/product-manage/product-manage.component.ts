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

  // Confirm delete
  readonly confirmDeleteId = signal<number | null>(null);
  readonly confirmDeleteName = signal('');

  // ── Filter Form ───────────────────────────────────────────────
  readonly filterForm: FormGroup = this.fb.group({
    keyword: [''],
    categoryId: [null],
    brandId: [null],
    status: [''],
  });

  readonly pageNumbers = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i)
  );

  readonly paginationText = computed(() => {
    const total = this.totalElements();
    if (total === 0) return '0-0 of 0';
    const start = this.currentPage() * this.pageSize() + 1;
    const end = Math.min((this.currentPage() + 1) * this.pageSize(), total);
    return `${start}-${end} of ${total}`;
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

  loadProducts(): void {
    this.loading.set(true);
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

  onFilterChange(): void {
    this.currentPage.set(0);
    this.loadProducts();
  }

  onPageSizeChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.pageSize.set(Number(select.value));
    this.currentPage.set(0);
    this.loadProducts();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadProducts();
  }

  editProduct(productId: number): void {
    this.router.navigate(['/admin/products', productId, 'edit']);
  }

  // ── Delete ────────────────────────────────────────────────────
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
    this.productService.delete(id).subscribe({
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

  // ── Helpers ───────────────────────────────────────────────────
  getPriceDisplay(prod: ProductResponse): string {
    if (!prod.variants || prod.variants.length === 0) return '—';
    const prices = prod.variants.map((v) => Number(v.salePrice ?? v.price));
    const min = Math.min(...prices);
    const max = Math.max(...prices);
    if (min === max) {
      return `$${min.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
    }
    return `$${min.toLocaleString('en-US', { minimumFractionDigits: 2 })} - $${max.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
  }

  getStatusClass(status: string): string {
    return status?.toLowerCase() || 'inactive';
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      active: 'Active',
      inactive: 'Inactive',
      discontinued: 'Discontinued',
    };
    return map[status?.toLowerCase()] ?? status;
  }
}
