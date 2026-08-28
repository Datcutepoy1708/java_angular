import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ProductResponse } from '../../../core/models/product.model';
import { CartService } from '../../../core/services/cart.service';

export interface DisplayPriceResult {
  price: number;
  salePrice: number | null;
  discountPercent: number | null;
}

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductCardComponent {
  private readonly cartService = inject(CartService);
  private readonly router = inject(Router);

  readonly product = input.required<ProductResponse>();
  readonly addToCart = output<ProductResponse>();

  readonly isAdding = signal(false);
  readonly isAdded = signal(false);

  getMainImageUrl(product: ProductResponse): string {
    if (product.images && product.images.length > 0) {
      const main = product.images.find((img) => img.imageType === 'MAIN');
      return main ? main.imageUrl : product.images[0].imageUrl;
    }
    return 'assets/placeholder-product.png';
  }

  getDisplayPrice(product: ProductResponse): DisplayPriceResult {
    if (!product.variants || product.variants.length === 0) {
      return { price: 0, salePrice: null, discountPercent: null };
    }

    const activeVariants = product.variants.filter((v) => v.status === 'active');
    const candidates = activeVariants.length > 0 ? activeVariants : product.variants;

    // Pick variant with lowest effective price
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

  onAddToCartClick(event: Event): void {
    event.preventDefault();
    event.stopPropagation();

    const p = this.product();
    const activeVariants = p.variants?.filter((v) => v.status === 'active') || [];
    const candidates = activeVariants.length > 0 ? activeVariants : (p.variants || []);

    if (candidates.length === 0) {
      this.router.navigate(['/products', p.slug]);
      return;
    }

    // Pick variant with lowest effective price
    const sorted = [...candidates].sort((a, b) => {
      const priceA = a.salePrice != null && a.salePrice > 0 ? a.salePrice : a.price;
      const priceB = b.salePrice != null && b.salePrice > 0 ? b.salePrice : b.price;
      return priceA - priceB;
    });

    const targetVariant = sorted[0];
    this.isAdding.set(true);

    this.cartService.addToCart(targetVariant.variantId, 1).subscribe({
      next: (success) => {
        this.isAdding.set(false);
        if (success) {
          this.isAdded.set(true);
          setTimeout(() => this.isAdded.set(false), 1500);
        }
      },
      error: () => {
        this.isAdding.set(false);
      }
    });

    this.addToCart.emit(p);
  }
}
