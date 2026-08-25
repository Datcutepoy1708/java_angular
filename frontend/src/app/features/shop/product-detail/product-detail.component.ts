import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { InventoryService } from '../../../core/services/inventory.service';
import { CartService } from '../../../core/services/cart.service';
import { ReviewService } from '../../../core/services/review.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  ProductResponse,
  ProductVariantResponse,
} from '../../../core/models/product.model';
import { VariantStockSummary } from '../../../core/models/inventory.model';
import { RatingSummary, Review } from '../../../core/models/review.model';
import { ProductCardComponent } from '../../../shared/components/product-card/product-card.component';
import { RatingStarsComponent } from '../../../shared/components/rating-stars/rating-stars.component';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ProductCardComponent, RatingStarsComponent],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductDetailComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly inventoryService = inject(InventoryService);
  private readonly cartService = inject(CartService);
  private readonly reviewService = inject(ReviewService);
  readonly authService = inject(AuthService);
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

  // ── Review Signals ─────────────────────────────────────────────
  readonly ratingSummary = signal<RatingSummary | null>(null);
  readonly reviews = signal<Review[]>([]);
  readonly selectedRatingFilter = signal<number | null>(null);
  readonly showWriteReviewModal = signal<boolean>(false);
  readonly reviewFormRating = signal<number>(5);
  readonly reviewFormTitle = signal<string>('');
  readonly reviewFormComment = signal<string>('');
  readonly submittingReview = signal<boolean>(false);
  readonly reviewSuccessMessage = signal<string | null>(null);
  readonly reviewErrorMessage = signal<string | null>(null);

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

        // Load reviews & rating summary
        this.loadRatingSummary(prod.productId);
        this.loadReviews(prod.productId);

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

  loadRatingSummary(productId: number): void {
    this.reviewService.getProductRatingSummary(productId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.ratingSummary.set(res.data);
        }
      },
      error: (err) => console.error('Error loading rating summary:', err)
    });
  }

  loadReviews(productId: number, rating?: number): void {
    this.reviewService.getProductReviews(productId, rating, 0, 20).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.reviews.set(res.data.content);
        }
      },
      error: (err) => console.error('Error loading reviews:', err)
    });
  }

  filterReviewsByRating(rating: number | null): void {
    this.selectedRatingFilter.set(rating);
    const prod = this.product();
    if (prod) {
      this.loadReviews(prod.productId, rating !== null ? rating : undefined);
    }
  }

  openWriteReviewModal(): void {
    if (!this.authService.currentUser()) {
      this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    this.reviewFormRating.set(5);
    this.reviewFormTitle.set('');
    this.reviewFormComment.set('');
    this.reviewSuccessMessage.set(null);
    this.reviewErrorMessage.set(null);
    this.showWriteReviewModal.set(true);
  }

  closeWriteReviewModal(): void {
    this.showWriteReviewModal.set(false);
  }

  setReviewRating(stars: number): void {
    this.reviewFormRating.set(stars);
  }

  submitReview(): void {
    const prod = this.product();
    if (!prod) return;

    if (!this.reviewFormComment().trim()) {
      this.reviewErrorMessage.set('Vui lòng nhập nội dung nhận xét chi tiết.');
      return;
    }

    this.submittingReview.set(true);
    this.reviewErrorMessage.set(null);

    this.reviewService.submitReview(prod.productId, {
      rating: this.reviewFormRating(),
      title: this.reviewFormTitle().trim() || undefined,
      comment: this.reviewFormComment().trim()
    }).subscribe({
      next: (res) => {
        this.submittingReview.set(false);
        this.reviewSuccessMessage.set('Cảm ơn bạn! Đánh giá của bạn đã được ghi nhận thành công.');
        this.loadRatingSummary(prod.productId);
        this.loadReviews(prod.productId, this.selectedRatingFilter() ?? undefined);
        setTimeout(() => {
          this.closeWriteReviewModal();
        }, 1500);
      },
      error: (err) => {
        this.submittingReview.set(false);
        const msg = err.error?.message || 'Không thể gửi đánh giá. Vui lòng thử lại sau.';
        this.reviewErrorMessage.set(msg);
      }
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

  addToCart(): void {
    const v = this.selectedVariant();
    if (v) {
      this.cartService.addToCart(v.variantId, this.quantity()).subscribe();
    }
  }

  buyNow(): void {
    const v = this.selectedVariant();
    if (v) {
      this.cartService.addToCart(v.variantId, this.quantity()).subscribe((success) => {
        if (success) {
          this.router.navigate(['/checkout']);
        }
      });
    }
  }
}
