import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../../core/services/product.service';
import { InventoryService } from '../../../core/services/inventory.service';
import {
  ProductResponse,
  ProductVariantResponse,
} from '../../../core/models/product.model';
import { VariantStockSummary } from '../../../core/models/inventory.model';
import { ProductCardComponent } from '../../../shared/components/product-card/product-card.component';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductDetailComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly inventoryService = inject(InventoryService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  // ── Data Signals ───────────────────────────────────────────────
  readonly product = signal<ProductResponse | null>(null);
  readonly selectedVariant = signal<ProductVariantResponse | null>(null);
  readonly variantStock = signal<VariantStockSummary | null>(null);
  readonly activeImageIndex = signal<number>(0);
  readonly quantity = signal<number>(1);
  readonly activeTab = signal<'desc' | 'specs' | 'reviews'>('desc');
  readonly relatedProducts = signal<ProductResponse[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.route.params.subscribe((params) => {
      const slugOrId = params['slug'];
      if (slugOrId) {
        this.loadProduct(slugOrId);
      }
    });
  }

  loadProduct(slugOrId: string): void {
    this.loading.set(true);
    this.activeImageIndex.set(0);
    this.quantity.set(1);

    const isNumericId = /^\d+$/.test(slugOrId);
    const req$ = isNumericId
      ? this.productService.getProductById(Number(slugOrId))
      : this.productService.getProductBySlug(slugOrId);

    req$.subscribe({
      next: (res) => {
        const prod = res.data;
        this.product.set(prod);

        // Select default variant (first active variant or first variant)
        if (prod.variants && prod.variants.length > 0) {
          const activeVars = prod.variants.filter((v) => v.status === 'active');
          const defaultVar = activeVars.length > 0 ? activeVars[0] : prod.variants[0];
          this.selectedVariant.set(defaultVar);
          this.loadVariantStock(defaultVar.variantId);
        } else {
          this.selectedVariant.set(null);
          this.variantStock.set(null);
        }

        this.loading.set(false);

        // Load related products in the same category
        if (prod.categoryId) {
          this.loadRelatedProducts(prod.categoryId, prod.productId);
        }
      },
      error: (err) => {
        console.error('Error loading product:', err);
        this.loading.set(false);
      },
    });
  }

  loadVariantStock(variantId: number): void {
    this.inventoryService.getVariantStockSummary(variantId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.variantStock.set(res.data);
        }
      },
      error: () => {
        this.variantStock.set(null);
      }
    });
  }

  loadRelatedProducts(categoryId: number, excludeId: number): void {
    this.productService.getProducts({ categoryId, size: 5 }).subscribe({
      next: (res) => {
        const related = res.data.content.filter((p) => p.productId !== excludeId);
        this.relatedProducts.set(related.slice(0, 4));
      },
    });
  }

  selectVariant(variant: ProductVariantResponse): void {
    this.selectedVariant.set(variant);
    this.loadVariantStock(variant.variantId);
  }

  selectImage(index: number): void {
    this.activeImageIndex.set(index);
  }

  increaseQuantity(): void {
    this.quantity.update((q) => q + 1);
  }

  decreaseQuantity(): void {
    this.quantity.update((q) => (q > 1 ? q - 1 : 1));
  }

  get currentPrice(): number {
    const v = this.selectedVariant();
    if (v) {
      return v.salePrice != null && v.salePrice > 0 ? v.salePrice : v.price;
    }
    return 0;
  }

  get originalPrice(): number | null {
    const v = this.selectedVariant();
    if (v && v.salePrice != null && v.salePrice > 0 && v.price > v.salePrice) {
      return v.price;
    }
    return null;
  }

  get discountPercent(): number | null {
    const v = this.selectedVariant();
    if (v && v.salePrice != null && v.salePrice > 0 && v.price > v.salePrice) {
      return Math.round(((v.price - v.salePrice) / v.price) * 100);
    }
    return null;
  }

  get savingsAmount(): number | null {
    const v = this.selectedVariant();
    if (v && v.salePrice != null && v.salePrice > 0 && v.price > v.salePrice) {
      return v.price - v.salePrice;
    }
    return null;
  }

  formatCurrency(val: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(val);
  }

  // ── UI States ──────────────────────────────────────────────────
  readonly cartFeatureNotice = signal<boolean>(false);

  addToCart(): void {
    // TODO (Phase 5): Wire to CartService.addToCart({ variantId, quantity })
    this.cartFeatureNotice.set(true);
    setTimeout(() => this.cartFeatureNotice.set(false), 4000);
  }

  buyNow(): void {
    // TODO (Phase 5): Wire to CartService and navigate to /checkout
    this.cartFeatureNotice.set(true);
    setTimeout(() => this.cartFeatureNotice.set(false), 4000);
  }
}
