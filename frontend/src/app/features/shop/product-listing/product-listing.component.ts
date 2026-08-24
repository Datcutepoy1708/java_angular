import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { ProductService } from '../../../core/services/product.service';
import { CategoryService } from '../../../core/services/category.service';
import { BrandService } from '../../../core/services/brand.service';
import { AttributeService } from '../../../core/services/attribute.service';
import { ProductFilterRequest, ProductResponse } from '../../../core/models/product.model';
import { CategoryResponse } from '../../../core/models/category.model';
import { BrandResponse } from '../../../core/models/brand.model';
import { AttributeResponse } from '../../../core/models/attribute.model';
import { ProductCardComponent } from '../../../shared/components/product-card/product-card.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

interface PricePreset {
  label: string;
  min: number | null;
  max: number | null;
}

@Component({
  selector: 'app-product-listing',
  standalone: true,
  imports: [RouterLink, FormsModule, DecimalPipe, ProductCardComponent, PaginationComponent],
  templateUrl: './product-listing.component.html',
  styleUrl: './product-listing.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductListingComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly brandService = inject(BrandService);
  private readonly attributeService = inject(AttributeService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  // ── Data Signals ───────────────────────────────────────────────
  readonly products = signal<ProductResponse[]>([]);
  readonly categoriesTree = signal<CategoryResponse[]>([]);
  readonly allBrands = signal<BrandResponse[]>([]);

  // ── Dynamic Attribute Filters (EAV) ───────────────────────────
  readonly categoryAttributes = signal<AttributeResponse[]>([]);
  readonly selectedAttributeFilters = signal<{ [attributeId: number]: string[] }>({});
  readonly availableOptionsByAttribute = signal<{ [attributeId: number]: string[] }>({});

  // ── Filter State ───────────────────────────────────────────────
  readonly currentCategorySlug = signal<string | null>(null);
  readonly currentCategoryName = signal<string>('Tất cả sản phẩm');
  readonly selectedBrandIds = signal<number[]>([]);
  readonly brandSearchText = signal<string>('');
  readonly keyword = signal<string>('');
  readonly selectedMinPrice = signal<number | null>(null);
  readonly selectedMaxPrice = signal<number | null>(null);
  readonly customMinPrice = signal<number | null>(null);
  readonly customMaxPrice = signal<number | null>(null);
  readonly sortBy = signal<string>('createdAt,desc');

  // ── Pagination State ───────────────────────────────────────────
  readonly page = signal(0);
  readonly size = signal(12);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(true);

  // ── Price Presets ──────────────────────────────────────────────
  readonly pricePresets: PricePreset[] = [
    { label: 'Dưới 10 triệu', min: null, max: 10000000 },
    { label: '10 - 20 triệu', min: 10000000, max: 20000000 },
    { label: '20 - 30 triệu', min: 20000000, max: 30000000 },
    { label: 'Trên 30 triệu', min: 30000000, max: null },
  ];

  ngOnInit(): void {
    this.loadCategoriesTree();
    this.loadBrands();

    // Subscribe to query parameter changes
    this.route.queryParams.subscribe((params) => {
      this.keyword.set(params['keyword'] || '');
      this.currentCategorySlug.set(params['category'] || null);

      if (params['brand']) {
        const brandSlug = params['brand'];
        const found = this.allBrands().find((b) => b.slug === brandSlug);
        if (found) {
          this.selectedBrandIds.set([found.brandId]);
        }
      }

      if (params['attributes']) {
        this.parseAttributeParams(params['attributes']);
      } else {
        this.selectedAttributeFilters.set({});
      }

      if (params['minPrice']) this.selectedMinPrice.set(Number(params['minPrice']));
      if (params['maxPrice']) this.selectedMaxPrice.set(Number(params['maxPrice']));
      if (params['sort']) this.sortBy.set(params['sort']);
      if (params['page']) this.page.set(Number(params['page']));

      this.loadProducts();
    });
  }

  loadCategoriesTree(): void {
    this.categoryService.getTree().subscribe({
      next: (res: { data: CategoryResponse[] }) => {
        this.categoriesTree.set(res.data);
        if (this.currentCategorySlug()) {
          const cat = this.findCategoryBySlug(res.data, this.currentCategorySlug()!);
          if (cat) {
            this.loadCategoryAttributes(cat.categoryId);
          }
        }
      },
    });
  }

  loadCategoryAttributes(categoryId: number): void {
    this.attributeService.getByCategory(categoryId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.categoryAttributes.set(res.data);
        } else {
          this.categoryAttributes.set([]);
        }
      },
      error: () => this.categoryAttributes.set([])
    });
  }

  loadBrands(): void {
    this.brandService.getAll().subscribe({
      next: (res: { data: BrandResponse[] }) => {
        this.allBrands.set(res.data);
        // If brand was passed in route params initially
        const brandSlug = this.route.snapshot.queryParams['brand'];
        if (brandSlug) {
          const found = res.data.find((b: BrandResponse) => b.slug === brandSlug);
          if (found && !this.selectedBrandIds().includes(found.brandId)) {
            this.selectedBrandIds.set([found.brandId]);
            this.loadProducts();
          }
        }
      },
    });
  }

  loadProducts(): void {
    this.loading.set(true);

    const sortParts = this.sortBy().split(',');
    const filter: ProductFilterRequest = {
      page: this.page(),
      size: this.size(),
      sortBy: sortParts[0] || 'createdAt',
      sortDir: sortParts[1] || 'desc',
    };

    if (this.keyword().trim()) {
      filter.keyword = this.keyword().trim();
    }

    if (this.selectedBrandIds().length === 1) {
      filter.brandId = this.selectedBrandIds()[0];
    }

    // Resolve Category ID from slug if present
    if (this.currentCategorySlug()) {
      const slug = this.currentCategorySlug()!;
      const cat = this.findCategoryBySlug(this.categoriesTree(), slug);
      if (cat) {
        filter.categoryId = cat.categoryId;
        this.currentCategoryName.set(cat.name);
        this.loadCategoryAttributes(cat.categoryId);
      } else {
        this.currentCategoryName.set(slug);
        this.categoryAttributes.set([]);
      }
    } else {
      this.currentCategoryName.set('Tất cả sản phẩm');
      this.categoryAttributes.set([]);
    }

    // EAV Attribute Filters
    const attrQuery = this.buildAttributeQueryString();
    if (attrQuery) {
      filter.attributes = attrQuery;
    }

    if (this.selectedMinPrice() != null) {
      filter.minPrice = this.selectedMinPrice()!;
    }
    if (this.selectedMaxPrice() != null) {
      filter.maxPrice = this.selectedMaxPrice()!;
    }

    this.productService.getProducts(filter).subscribe({
      next: (res) => {
        this.products.set(res.data.content);
        this.totalElements.set(res.data.totalElements);
        this.totalPages.set(res.data.totalPages);
        this.extractAvailableOptions(res.data.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private extractAvailableOptions(products: ProductResponse[]): void {
    const optionsMap: { [attrId: number]: Set<string> } = {};

    for (const p of products) {
      if (p.specifications) {
        for (const spec of p.specifications) {
          if (!optionsMap[spec.attributeId]) {
            optionsMap[spec.attributeId] = new Set();
          }
          optionsMap[spec.attributeId].add(spec.value);
        }
      }
    }

    const finalMap: { [attrId: number]: string[] } = {};
    for (const [attrId, set] of Object.entries(optionsMap)) {
      finalMap[Number(attrId)] = Array.from(set).sort();
    }
    this.availableOptionsByAttribute.set(finalMap);
  }

  getOptionsForAttribute(attr: AttributeResponse): string[] {
    const fromMap = this.availableOptionsByAttribute()[attr.attributeId];
    if (fromMap && fromMap.length > 0) return fromMap;

    // Default common options if products list is currently filtered
    const nameLower = attr.name.toLowerCase();
    if (nameLower.includes('socket')) return ['LGA1700', 'AM5', 'AM4'];
    if (nameLower.includes('số nhân')) return ['24', '20', '16', '14', '8', '6'];
    if (nameLower.includes('vram')) return ['24GB', '16GB', '12GB', '8GB'];
    if (nameLower.includes('chuẩn ram')) return ['DDR5', 'DDR4'];
    if (nameLower.includes('kích thước')) return ['16 inch', '15.6 inch', '14 inch', '27 inch', '24 inch', '32 inch'];
    if (nameLower.includes('tấm nền')) return ['QD-OLED', 'Fast IPS', 'IPS', 'OLED'];
    if (nameLower.includes('tần số')) return ['240Hz', '180Hz', '165Hz', '144Hz'];
    return [];
  }

  toggleAttributeFilter(attributeId: number, value: string): void {
    this.selectedAttributeFilters.update((current) => {
      const existing = current[attributeId] ? [...current[attributeId]] : [];
      const index = existing.indexOf(value);
      if (index > -1) {
        existing.splice(index, 1);
      } else {
        existing.push(value);
      }

      const updated = { ...current };
      if (existing.length > 0) {
        updated[attributeId] = existing;
      } else {
        delete updated[attributeId];
      }
      return updated;
    });

    this.page.set(0);
    this.updateQueryParams();
  }

  isAttributeSelected(attributeId: number, value: string): boolean {
    const selected = this.selectedAttributeFilters()[attributeId];
    return selected ? selected.includes(value) : false;
  }

  buildAttributeQueryString(): string | null {
    const filters = this.selectedAttributeFilters();
    const parts: string[] = [];
    for (const [attrId, values] of Object.entries(filters)) {
      if (values && values.length > 0) {
        parts.push(`${attrId}:${values.join(',')}`);
      }
    }
    return parts.length > 0 ? parts.join(';') : null;
  }

  parseAttributeParams(paramStr: string): void {
    const result: { [attrId: number]: string[] } = {};
    if (!paramStr) return;

    const sections = paramStr.split(';');
    for (const sec of sections) {
      const parts = sec.split(':');
      if (parts.length === 2) {
        const attrId = Number(parts[0]);
        const vals = parts[1].split(',').filter((v) => v.trim().length > 0);
        if (!isNaN(attrId) && vals.length > 0) {
          result[attrId] = vals;
        }
      }
    }
    this.selectedAttributeFilters.set(result);
  }

  private findCategoryBySlug(tree: CategoryResponse[], slug: string): CategoryResponse | null {
    for (const item of tree) {
      if (item.slug === slug) return item;
      if (item.children && item.children.length > 0) {
        const found = this.findCategoryBySlug(item.children, slug);
        if (found) return found;
      }
    }
    return null;
  }

  get filteredBrands(): BrandResponse[] {
    const q = this.brandSearchText().toLowerCase().trim();
    if (!q) return this.allBrands();
    return this.allBrands().filter((b) => b.name.toLowerCase().includes(q));
  }

  // ── Filter Actions ─────────────────────────────────────────────
  selectCategory(slug: string | null): void {
    this.currentCategorySlug.set(slug);
    this.selectedAttributeFilters.set({});
    this.page.set(0);
    this.updateQueryParams();
  }

  toggleBrand(brandId: number): void {
    this.selectedBrandIds.update((ids) =>
      ids.includes(brandId) ? ids.filter((id) => id !== brandId) : [...ids, brandId]
    );
    this.page.set(0);
    this.loadProducts();
  }

  selectPricePreset(preset: PricePreset): void {
    if (this.selectedMinPrice() === preset.min && this.selectedMaxPrice() === preset.max) {
      this.selectedMinPrice.set(null);
      this.selectedMaxPrice.set(null);
    } else {
      this.selectedMinPrice.set(preset.min);
      this.selectedMaxPrice.set(preset.max);
    }
    this.page.set(0);
    this.updateQueryParams();
  }

  applyCustomPrice(): void {
    const min = this.customMinPrice();
    const max = this.customMaxPrice();
    this.selectedMinPrice.set(min);
    this.selectedMaxPrice.set(max);
    this.page.set(0);
    this.updateQueryParams();
  }

  onSortChange(sort: string): void {
    this.sortBy.set(sort);
    this.page.set(0);
    this.updateQueryParams();
  }

  onPageChange(newPage: number): void {
    this.page.set(newPage);
    this.updateQueryParams();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  resetAllFilters(): void {
    this.currentCategorySlug.set(null);
    this.selectedBrandIds.set([]);
    this.selectedAttributeFilters.set({});
    this.selectedMinPrice.set(null);
    this.selectedMaxPrice.set(null);
    this.customMinPrice.set(null);
    this.customMaxPrice.set(null);
    this.keyword.set('');
    this.page.set(0);
    this.router.navigate(['/products']);
  }

  private updateQueryParams(): void {
    const queryParams: any = {};
    if (this.currentCategorySlug()) queryParams.category = this.currentCategorySlug();
    if (this.keyword().trim()) queryParams.keyword = this.keyword().trim();
    if (this.selectedMinPrice() != null) queryParams.minPrice = this.selectedMinPrice();
    if (this.selectedMaxPrice() != null) queryParams.maxPrice = this.selectedMaxPrice();
    if (this.sortBy() !== 'createdAt,desc') queryParams.sort = this.sortBy();
    if (this.page() > 0) queryParams.page = this.page();

    const attrQuery = this.buildAttributeQueryString();
    if (attrQuery) {
      queryParams.attributes = attrQuery;
    }

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
    });
  }

  getBrandName(id: number): string {
    const b = this.allBrands().find((item) => item.brandId === id);
    return b ? b.name : '';
  }

  onAddToCart(product: ProductResponse): void {
    console.log('Thêm vào giỏ hàng từ listing:', product.name);
  }
}
