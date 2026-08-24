import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { CartItem } from '../../../core/models/cart.model';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CartComponent implements OnInit {
  readonly cartService = inject(CartService);
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // Expose signals from CartService
  readonly cart = this.cartService.cart;
  readonly isLoading = this.cartService.isLoading;
  readonly totalItems = this.cartService.totalItems;
  readonly totalQuantity = this.cartService.totalQuantity;
  readonly totalAmount = this.cartService.totalAmount;
  readonly originalTotalAmount = this.cartService.originalTotalAmount;
  readonly savingsAmount = this.cartService.savingsAmount;

  ngOnInit(): void {
    this.cartService.loadCart();
  }

  increaseQuantity(item: CartItem): void {
    this.cartService.updateQuantity(item.cartId, item.variantId, item.quantity + 1).subscribe();
  }

  decreaseQuantity(item: CartItem): void {
    if (item.quantity > 1) {
      this.cartService.updateQuantity(item.cartId, item.variantId, item.quantity - 1).subscribe();
    } else {
      this.removeItem(item);
    }
  }

  onQuantityInput(item: CartItem, event: Event): void {
    const input = event.target as HTMLInputElement;
    let qty = parseInt(input.value, 10);
    if (isNaN(qty) || qty <= 0) {
      qty = 1;
    }
    input.value = qty.toString();
    this.cartService.updateQuantity(item.cartId, item.variantId, qty).subscribe();
  }

  removeItem(item: CartItem): void {
    this.cartService.removeItem(item.cartId, item.variantId).subscribe();
  }

  clearCart(): void {
    if (confirm('Bạn có chắc chắn muốn xóa toàn bộ giỏ hàng?')) {
      this.cartService.clearCart().subscribe();
    }
  }

  proceedToCheckout(): void {
    if (this.cart().items.length === 0) {
      return;
    }

    if (!this.authService.isAuthenticated()) {
      this.cartService.showToast('Vui lòng đăng nhập để tiến hành đặt hàng', 'warning');
      this.router.navigate(['/auth/login'], { queryParams: { redirectUrl: '/cart' } });
      return;
    }

    // Check if any items are out of stock or exceeded
    const hasInvalidItems = this.cart().items.some(
      (item) => !item.isAvailable || item.isExceededStock
    );

    if (hasInvalidItems) {
      this.cartService.showToast(
        'Giỏ hàng có sản phẩm hết hàng hoặc vượt quá số lượng tồn. Vui lòng điều chỉnh trước khi đặt hàng!',
        'warning'
      );
      return;
    }

    // Ready for Phase 6 Checkout
    this.cartService.showToast('Đang chuyển tới trang Thanh toán (Phase 6)...', 'success');
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount);
  }
}
