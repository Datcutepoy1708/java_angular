import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ProductService } from '../../../core/services/product.service';
import { CompareItem, CompareService } from '../../../core/services/compare.service';
import { CartService } from '../../../core/services/cart.service';
import { ProductResponse } from '../../../core/models/product.model';
import { ProductAttributeValueResponse } from '../../../core/models/attribute.model';
import { normalizeImageUrl } from '../../../core/util/image-url.util';

export interface DisplayPriceInfo {
  price: number;
  salePrice: number | null;
  discountPercent: number | null;
}

export interface ComparedProductData {
  product: ProductResponse;
  attributes: ProductAttributeValueResponse[];
  priceInfo: DisplayPriceInfo;
  imageUrl: string;
}

export interface SpecRow {
  attributeName: string;
  unit?: string;
  values: (string | null)[]; // Giá trị tương ứng với từng sản phẩm
  isDifferent: boolean; // Có điểm khác nhau giữa các sản phẩm không
}

@Component({
  selector: 'app-compare',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './compare.component.html',
  styleUrl: './compare.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CompareComponent implements OnInit {
  private readonly productService = inject(ProductService);
  readonly compareService = inject(CompareService);
  private readonly cartService = inject(CartService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // Trạng thái dữ liệu
  readonly isLoading = signal<boolean>(false);
  readonly productsData = signal<ComparedProductData[]>([]);
  readonly highlightDifferencesOnly = signal<boolean>(false);

  // Tìm kiếm thêm sản phẩm vào slot trống
  readonly showAddModal = signal<boolean>(false);
  readonly searchKeyword = signal<string>('');
  readonly searchResults = signal<ProductResponse[]>([]);
  readonly isSearching = signal<boolean>(false);

  // Giỏ hàng
  readonly addingToCartId = signal<number | null>(null);

  // Danh sách dòng thông số kỹ thuật đã tổng hợp
  readonly specRows = computed<SpecRow[]>(() => {
    const list = this.productsData();
    if (list.length === 0) return [];

    // Tập hợp tất cả các attributeName duy nhất
    const attrMap = new Map<string, { name: string; unit?: string }>();
    for (const item of list) {
      for (const attr of item.attributes) {
        if (!attrMap.has(attr.attributeName)) {
          attrMap.set(attr.attributeName, {
            name: attr.attributeName,
            unit: attr.unit,
          });
        }
      }
    }

    const rows: SpecRow[] = [];
    attrMap.forEach(({ name, unit }) => {
      const values: (string | null)[] = list.map((item) => {
        const found = item.attributes.find((a) => a.attributeName === name);
        if (!found || !found.value || found.value.trim() === '') {
          return null;
        }
        return found.value.trim();
      });

      // Kiểm tra có sự khác biệt giữa các sản phẩm không
      const nonNullValues = values.map((v) => (v ? v.toLowerCase() : ''));
      const firstVal = nonNullValues[0];
      const isDifferent = nonNullValues.some((v) => v !== firstVal);

      rows.push({
        attributeName: name,
        unit,
        values,
        isDifferent,
      });
    });

    return rows;
  });

  // Lọc theo chế độ chỉ xem điểm khác biệt
  readonly visibleSpecRows = computed<SpecRow[]>(() => {
    const rows = this.specRows();
    if (this.highlightDifferencesOnly()) {
      return rows.filter((r) => r.isDifferent);
    }
    return rows;
  });

  ngOnInit(): void {
    // Đọc query param ids
    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const idsParam = params['ids'];
        let productIds: number[] = [];

        if (idsParam) {
          productIds = idsParam
            .split(',')
            .map((idStr: string) => parseInt(idStr.trim(), 10))
            .filter((n: number) => !isNaN(n) && n > 0)
            .slice(0, 4); // Tối đa 4 sản phẩm
        } else {
          // Lấy từ CompareService
          productIds = this.compareService.items().map((i) => i.productId).slice(0, 4);
        }

        if (productIds.length > 0) {
          this.loadProductsForComparison(productIds);
        } else {
          this.productsData.set([]);
        }
      });
  }

  loadProductsForComparison(ids: number[]): void {
    if (ids.length === 0) {
      this.productsData.set([]);
      return;
    }

    this.isLoading.set(true);

    const calls = ids.map((id) =>
      forkJoin({
        productRes: this.productService.getById(id).pipe(catchError(() => of(null))),
        attrsRes: this.productService.getProductAttributes(id).pipe(catchError(() => of(null))),
      })
    );

    forkJoin(calls).subscribe({
      next: (results) => {
        const loaded: ComparedProductData[] = [];

        results.forEach((res) => {
          if (res.productRes && res.productRes.success && res.productRes.data) {
            const product = res.productRes.data;
            const attributes = res.attrsRes && res.attrsRes.success && res.attrsRes.data
              ? res.attrsRes.data
              : [];
            const priceInfo = this.getDisplayPrice(product);
            const imageUrl = this.getMainImageUrl(product);

            loaded.push({
              product,
              attributes,
              priceInfo,
              imageUrl,
            });
          }
        });

        this.productsData.set(loaded);
        this.isLoading.set(false);

        // Đồng bộ ngược lại CompareService nếu khác biệt
        this.syncWithCompareService(loaded);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  private syncWithCompareService(loaded: ComparedProductData[]): void {
    const newItems: CompareItem[] = loaded.map((item) => ({
      productId: item.product.productId,
      name: item.product.name,
      slug: item.product.slug,
      imageUrl: item.imageUrl,
      price: item.priceInfo.price,
      salePrice: item.priceInfo.salePrice,
      categoryId: item.product.categoryId,
      categoryName: item.product.categoryName,
      brandName: item.product.brandName,
    }));

    // Cập nhật URL params cho đồng bộ
    const idsString = loaded.map((i) => i.product.productId).join(',');
    if (idsString !== this.route.snapshot.queryParams['ids']) {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { ids: idsString },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      });
    }
  }

  removeProduct(productId: number): void {
    const current = this.productsData();
    const next = current.filter((p) => p.product.productId !== productId);
    this.productsData.set(next);
    this.compareService.removeFromCompare(productId);

    const ids = next.map((i) => i.product.productId).join(',');
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ids: ids || null },
      replaceUrl: true,
    });
  }

  clearAll(): void {
    this.productsData.set([]);
    this.compareService.clear();
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ids: null },
      replaceUrl: true,
    });
  }

  toggleHighlightDifferences(): void {
    this.highlightDifferencesOnly.update((v) => !v);
  }

  // Mở modal thêm sản phẩm
  openAddModal(): void {
    if (this.productsData().length >= 4) {
      alert('Đã đạt giới hạn tối đa 4 sản phẩm so sánh.');
      return;
    }
    this.searchKeyword.set('');
    this.searchResults.set([]);
    this.showAddModal.set(true);

    // Mặc định tải sản phẩm cùng danh mục với sản phẩm đầu tiên
    const current = this.productsData();
    if (current.length > 0 && current[0].product.categoryId) {
      this.fetchCategoryRecommendations(current[0].product.categoryId);
    }
  }

  closeAddModal(): void {
    this.showAddModal.set(false);
  }

  fetchCategoryRecommendations(categoryId: number): void {
    this.isSearching.set(true);
    this.productService
      .getProducts({ categoryId, size: 8, status: 'active' })
      .pipe(catchError(() => of(null)))
      .subscribe((res) => {
        this.isSearching.set(false);
        if (res && res.success && res.data) {
          const currentIds = this.productsData().map((p) => p.product.productId);
          this.searchResults.set(res.data.content.filter((p) => !currentIds.includes(p.productId)));
        }
      });
  }

  onSearchInput(keyword: string): void {
    this.searchKeyword.set(keyword);
    if (!keyword || keyword.trim().length < 2) {
      const current = this.productsData();
      if (current.length > 0 && current[0].product.categoryId) {
        this.fetchCategoryRecommendations(current[0].product.categoryId);
      }
      return;
    }

    this.isSearching.set(true);
    this.productService
      .getProducts({ keyword: keyword.trim(), size: 8, status: 'active' })
      .pipe(catchError(() => of(null)))
      .subscribe((res) => {
        this.isSearching.set(false);
        if (res && res.success && res.data) {
          const currentIds = this.productsData().map((p) => p.product.productId);
          this.searchResults.set(res.data.content.filter((p) => !currentIds.includes(p.productId)));
        }
      });
  }

  selectProductToAdd(product: ProductResponse): void {
    const current = this.productsData();
    if (current.length >= 4) return;

    this.compareService.addToCompare({
      productId: product.productId,
      name: product.name,
      slug: product.slug,
      imageUrl: this.getMainImageUrl(product),
      price: product.variants?.[0]?.price || 0,
      salePrice: product.variants?.[0]?.salePrice || null,
      categoryId: product.categoryId,
      categoryName: product.categoryName,
      brandName: product.brandName,
    });

    this.closeAddModal();

    // Reload comparison
    const newIds = [...current.map((p) => p.product.productId), product.productId];
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ids: newIds.join(',') },
    });
  }

  // Thêm vào giỏ hàng
  addToCart(product: ProductResponse): void {
    const activeVariants = product.variants?.filter((v) => v.status === 'active') || [];
    const candidates = activeVariants.length > 0 ? activeVariants : product.variants || [];

    if (candidates.length === 0) {
      this.router.navigate(['/products', product.slug]);
      return;
    }

    const sorted = [...candidates].sort((a, b) => {
      const priceA = a.salePrice != null && a.salePrice > 0 ? a.salePrice : a.price;
      const priceB = b.salePrice != null && b.salePrice > 0 ? b.salePrice : b.price;
      return priceA - priceB;
    });

    const targetVariant = sorted[0];
    this.addingToCartId.set(product.productId);

    this.cartService.addToCart(targetVariant.variantId, 1).subscribe({
      next: () => {
        this.addingToCartId.set(null);
      },
      error: () => {
        this.addingToCartId.set(null);
      },
    });
  }

  copyShareLink(): void {
    if (typeof window !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href).then(() => {
        alert('Đã sao chép liên kết so sánh vào bộ nhớ tạm!');
      });
    }
  }

  getMainImageUrl(product: ProductResponse): string {
    if (product.mainImageUrl) {
      const normalized = normalizeImageUrl(product.mainImageUrl);
      if (normalized) return normalized;
    }
    if (product.images && product.images.length > 0) {
      const main = product.images.find(
        (img) => (img.imageType as string)?.toLowerCase() === 'main'
      );
      const url = main ? main.imageUrl : product.images[0].imageUrl;
      const normalized = normalizeImageUrl(url);
      if (normalized) return normalized;
    }
    return '/assets/images/complexus-logo.png';
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    const fallback = '/assets/images/complexus-logo.png';
    if (img && !img.src.includes('complexus-logo.png')) {
      img.src = fallback;
    }
  }

  getDisplayPrice(product: ProductResponse): DisplayPriceInfo {
    if (!product.variants || product.variants.length === 0) {
      return { price: 0, salePrice: null, discountPercent: null };
    }

    const activeVariants = product.variants.filter((v) => v.status === 'active');
    const candidates = activeVariants.length > 0 ? activeVariants : product.variants;

    const sorted = [...candidates].sort((a, b) => {
      const priceA = a.salePrice != null && a.salePrice > 0 ? a.salePrice : a.price;
      const priceB = b.salePrice != null && b.salePrice > 0 ? b.salePrice : b.price;
      return priceA - priceB;
    });

    const best = sorted[0];
    let discountPercent: number | null = null;
    if (best.salePrice != null && best.salePrice > 0 && best.price > best.salePrice) {
      discountPercent = Math.round(((best.price - best.salePrice) / best.price) * 100);
    }

    return {
      price: best.price,
      salePrice: best.salePrice,
      discountPercent,
    };
  }

  formatCurrency(val: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(val);
  }
}
