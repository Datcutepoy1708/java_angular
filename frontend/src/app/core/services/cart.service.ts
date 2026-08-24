import { computed, effect, inject, Injectable, PLATFORM_ID, signal, untracked } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { Observable, of, tap, catchError, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/auth.model';
import { AddToCartRequest, Cart, CartItem, LocalCartItem, MergeCartRequest, UpdateCartItemRequest } from '../models/cart.model';
import { AuthService } from './auth.service';
import { ProductService } from './product.service';

const GUEST_CART_KEY = 'complexus_guest_cart';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly productService = inject(ProductService);
  private readonly platformId = inject(PLATFORM_ID);

  private readonly baseUrl = `${environment.apiUrl}/api/v1/cart`;

  // Signals
  readonly cart = signal<Cart>(this.getEmptyCart());
  readonly isLoading = signal<boolean>(false);
  readonly toastMessage = signal<{ text: string; type: 'success' | 'warning' | 'error' } | null>(null);

  // Computed signals
  readonly totalItems = computed(() => this.cart().totalItems);
  readonly totalQuantity = computed(() => this.cart().totalQuantity);
  readonly totalAmount = computed(() => this.cart().totalAmount);
  readonly originalTotalAmount = computed(() => this.cart().originalTotalAmount);
  readonly savingsAmount = computed(() => this.cart().savingsAmount);
  readonly hasItems = computed(() => this.cart().totalItems > 0);

  constructor() {
    // Watch auth changes: when user logs in, merge guest cart; when logged out, load guest cart
    effect(() => {
      const isAuth = this.authService.isAuthenticated();
      untracked(() => {
        if (isAuth) {
          this.mergeGuestCart();
        } else {
          this.loadCart();
        }
      });
    });
  }

  loadCart(): void {
    if (this.authService.isAuthenticated()) {
      this.isLoading.set(true);
      this.http.get<ApiResponse<Cart>>(`${this.baseUrl}`).subscribe({
        next: (res) => {
          if (res.success && res.data) {
            this.cart.set(res.data);
            if (res.data.removedStaleItemsCount && res.data.removedStaleItemsCount > 0) {
              this.showToast(`Đã tự động gỡ ${res.data.removedStaleItemsCount} sản phẩm không còn kinh doanh khỏi giỏ hàng`, 'warning');
            }
          }
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        }
      });
    } else {
      this.loadGuestCart();
    }
  }

  addToCart(variantId: number, quantity: number = 1): Observable<boolean> {
    if (quantity <= 0) {
      quantity = 1;
    }

    if (this.authService.isAuthenticated()) {
      this.isLoading.set(true);
      const req: AddToCartRequest = { variantId, quantity };
      return this.http.post<ApiResponse<Cart>>(`${this.baseUrl}/items`, req).pipe(
        map((res) => {
          this.isLoading.set(false);
          if (res.success && res.data) {
            this.cart.set(res.data);
            this.showToast('Đã thêm sản phẩm vào giỏ hàng thành công!', 'success');
            return true;
          }
          return false;
        }),
        catchError((err) => {
          this.isLoading.set(false);
          const msg = err.error?.message || 'Không thể thêm sản phẩm vào giỏ hàng';
          this.showToast(msg, 'error');
          return of(false);
        })
      );
    } else {
      // Guest localStorage
      const guestItems = this.getGuestStorage();
      const existing = guestItems.find((i) => i.variantId === variantId);
      if (existing) {
        existing.quantity += quantity;
      } else {
        guestItems.push({ variantId, quantity, addedAt: Date.now() });
      }
      this.saveGuestStorage(guestItems);
      this.loadGuestCart();
      this.showToast('Đã thêm sản phẩm vào giỏ hàng (Khách vãng lai)!', 'success');
      return of(true);
    }
  }

  updateQuantity(cartItemId: number | null | undefined, variantId: number, quantity: number): Observable<boolean> {
    if (this.authService.isAuthenticated() && cartItemId) {
      this.isLoading.set(true);
      const req: UpdateCartItemRequest = { quantity };
      return this.http.put<ApiResponse<Cart>>(`${this.baseUrl}/items/${cartItemId}`, req).pipe(
        map((res) => {
          this.isLoading.set(false);
          if (res.success && res.data) {
            this.cart.set(res.data);
            return true;
          }
          return false;
        }),
        catchError((err) => {
          this.isLoading.set(false);
          const msg = err.error?.message || 'Lỗi cập nhật số lượng';
          this.showToast(msg, 'error');
          return of(false);
        })
      );
    } else {
      // Guest Mode
      let guestItems = this.getGuestStorage();
      if (quantity <= 0) {
        guestItems = guestItems.filter((i) => i.variantId !== variantId);
      } else {
        const item = guestItems.find((i) => i.variantId === variantId);
        if (item) {
          item.quantity = quantity;
        }
      }
      this.saveGuestStorage(guestItems);
      this.loadGuestCart();
      return of(true);
    }
  }

  removeItem(cartItemId: number | null | undefined, variantId: number): Observable<boolean> {
    if (this.authService.isAuthenticated() && cartItemId) {
      this.isLoading.set(true);
      return this.http.delete<ApiResponse<Cart>>(`${this.baseUrl}/items/${cartItemId}`).pipe(
        map((res) => {
          this.isLoading.set(false);
          if (res.success && res.data) {
            this.cart.set(res.data);
            this.showToast('Đã xóa sản phẩm khỏi giỏ hàng', 'success');
            return true;
          }
          return false;
        }),
        catchError((err) => {
          this.isLoading.set(false);
          const msg = err.error?.message || 'Lỗi xóa sản phẩm';
          this.showToast(msg, 'error');
          return of(false);
        })
      );
    } else {
      // Guest Mode
      let guestItems = this.getGuestStorage();
      guestItems = guestItems.filter((i) => i.variantId !== variantId);
      this.saveGuestStorage(guestItems);
      this.loadGuestCart();
      this.showToast('Đã xóa sản phẩm khỏi giỏ hàng', 'success');
      return of(true);
    }
  }

  clearCart(): Observable<boolean> {
    if (this.authService.isAuthenticated()) {
      this.isLoading.set(true);
      return this.http.delete<ApiResponse<void>>(`${this.baseUrl}`).pipe(
        map(() => {
          this.isLoading.set(false);
          this.cart.set(this.getEmptyCart());
          this.showToast('Đã xóa toàn bộ giỏ hàng', 'success');
          return true;
        }),
        catchError((err) => {
          this.isLoading.set(false);
          const msg = err.error?.message || 'Lỗi xóa giỏ hàng';
          this.showToast(msg, 'error');
          return of(false);
        })
      );
    } else {
      this.clearGuestStorage();
      this.cart.set(this.getEmptyCart());
      this.showToast('Đã xóa toàn bộ giỏ hàng', 'success');
      return of(true);
    }
  }

  mergeGuestCart(): void {
    const guestItems = this.getGuestStorage();
    if (guestItems.length === 0) {
      this.loadCart();
      return;
    }

    const req: MergeCartRequest = {
      items: guestItems.map((i) => ({ variantId: i.variantId, quantity: i.quantity }))
    };

    this.isLoading.set(true);
    this.http.post<ApiResponse<Cart>>(`${this.baseUrl}/merge`, req).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res.success && res.data) {
          this.clearGuestStorage();
          this.cart.set(res.data);
          this.showToast('Đã tự động đồng bộ giỏ hàng từ phiên khách trước đó!', 'success');
        }
      },
      error: () => {
        this.isLoading.set(false);
        this.loadCart();
      }
    });
  }

  showToast(text: string, type: 'success' | 'warning' | 'error' = 'success'): void {
    this.toastMessage.set({ text, type });
    setTimeout(() => {
      if (this.toastMessage()?.text === text) {
        this.toastMessage.set(null);
      }
    }, 4000);
  }

  clearToast(): void {
    this.toastMessage.set(null);
  }

  private loadGuestCart(): void {
    const guestItems = this.getGuestStorage();
    if (guestItems.length === 0) {
      this.cart.set(this.getEmptyCart());
      return;
    }

    // Call product service to resolve variant info
    this.productService.getProducts({ page: 0, size: 100 }).subscribe({
      next: (res) => {
        const products = res.data.content || [];
        const cartItems: CartItem[] = [];
        let totalAmount = 0;
        let originalTotalAmount = 0;
        let totalQuantity = 0;

        for (const gItem of guestItems) {
          for (const p of products) {
            const v = p.variants?.find((varItem) => varItem.variantId === gItem.variantId);
            if (v) {
              const price = v.salePrice && v.salePrice > 0 ? v.salePrice : v.price;
              const originalPrice = v.price;
              const subtotal = price * gItem.quantity;
              const originalSubtotal = originalPrice * gItem.quantity;

              totalAmount += subtotal;
              originalTotalAmount += originalSubtotal;
              totalQuantity += gItem.quantity;

              const imageUrl = (v.images && v.images.length > 0)
                ? v.images[0].imageUrl
                : (p.images && p.images.length > 0 ? p.images[0].imageUrl : null);

              cartItems.push({
                cartId: null,
                variantId: v.variantId,
                variantName: v.variantName,
                skuVariant: v.skuVariant || '',
                price: price,
                originalPrice: originalPrice,
                imageUrl: imageUrl,
                productId: p.productId,
                productName: p.name,
                productSlug: p.slug,
                quantity: gItem.quantity,
                subtotal: subtotal,
                availableQty: 999, // Soft default for guest
                isAvailable: true,
                isExceededStock: false
              });
              break;
            }
          }
        }

        const savingsAmount = Math.max(0, originalTotalAmount - totalAmount);

        this.cart.set({
          items: cartItems,
          totalItems: cartItems.length,
          totalQuantity: totalQuantity,
          totalAmount: totalAmount,
          originalTotalAmount: originalTotalAmount,
          savingsAmount: savingsAmount
        });
      },
      error: () => {
        this.cart.set(this.getEmptyCart());
      }
    });
  }

  private getGuestStorage(): LocalCartItem[] {
    if (!isPlatformBrowser(this.platformId)) {
      return [];
    }
    const json = localStorage.getItem(GUEST_CART_KEY);
    if (!json) {
      return [];
    }
    try {
      return JSON.parse(json) as LocalCartItem[];
    } catch {
      this.clearGuestStorage();
      return [];
    }
  }

  private saveGuestStorage(items: LocalCartItem[]): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(GUEST_CART_KEY, JSON.stringify(items));
    }
  }

  private clearGuestStorage(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(GUEST_CART_KEY);
    }
  }

  private getEmptyCart(): Cart {
    return {
      items: [],
      totalItems: 0,
      totalQuantity: 0,
      totalAmount: 0,
      originalTotalAmount: 0,
      savingsAmount: 0
    };
  }
}
