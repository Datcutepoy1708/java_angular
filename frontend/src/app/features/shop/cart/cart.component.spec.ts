import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { CartComponent } from './cart.component';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { Cart, CartItem } from '../../../core/models/cart.model';
import { of } from 'rxjs';

describe('CartComponent', () => {
  let component: CartComponent;
  let fixture: ComponentFixture<CartComponent>;

  const mockCartSignal = signal<Cart>({
    items: [],
    totalItems: 0,
    totalQuantity: 0,
    totalAmount: 0,
    originalTotalAmount: 0,
    savingsAmount: 0
  });

  const mockIsLoadingSignal = signal<boolean>(false);
  const mockIsAuthSignal = signal<boolean>(true);

  const mockCartService = {
    cart: mockCartSignal,
    isLoading: mockIsLoadingSignal,
    totalItems: signal(0),
    totalQuantity: signal(0),
    totalAmount: signal(0),
    originalTotalAmount: signal(0),
    savingsAmount: signal(0),
    loadCart: vi.fn(),
    updateQuantity: vi.fn().mockReturnValue(of(true)),
    removeItem: vi.fn().mockReturnValue(of(true)),
    clearCart: vi.fn().mockReturnValue(of(true)),
    showToast: vi.fn()
  };

  const mockAuthService = {
    isAuthenticated: mockIsAuthSignal,
    currentUser: signal(null)
  };

  beforeEach(async () => {
    mockCartSignal.set({
      items: [],
      totalItems: 0,
      totalQuantity: 0,
      totalAmount: 0,
      originalTotalAmount: 0,
      savingsAmount: 0
    });

    await TestBed.configureTestingModule({
      imports: [CartComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: CartService, useValue: mockCartService },
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and render empty cart state initially', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.empty-cart-card')).toBeTruthy();
    expect(compiled.textContent).toContain('Giỏ hàng của bạn đang trống');
  });

  it('should render items when cart has items', () => {
    const mockItem: CartItem = {
      cartId: 1,
      variantId: 101,
      variantName: '16GB / 512GB',
      skuVariant: 'MBP-16-512',
      price: 40000000,
      originalPrice: 42000000,
      productId: 1,
      productName: 'MacBook Pro M5',
      productSlug: 'macbook-pro-m5',
      quantity: 2,
      subtotal: 80000000,
      availableQty: 10,
      isAvailable: true,
      isExceededStock: false
    };

    mockCartSignal.set({
      items: [mockItem],
      totalItems: 1,
      totalQuantity: 2,
      totalAmount: 80000000,
      originalTotalAmount: 84000000,
      savingsAmount: 4000000
    });
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.cart-grid')).toBeTruthy();
    expect(compiled.querySelector('.cart-item-card')).toBeTruthy();
    expect(compiled.textContent).toContain('MacBook Pro M5');
  });

  it('should call updateQuantity when clicking stepper plus', () => {
    const mockItem: CartItem = {
      cartId: 1,
      variantId: 101,
      variantName: '16GB / 512GB',
      skuVariant: 'MBP-16-512',
      price: 40000000,
      originalPrice: 42000000,
      productId: 1,
      productName: 'MacBook Pro M5',
      productSlug: 'macbook-pro-m5',
      quantity: 2,
      subtotal: 80000000,
      availableQty: 10,
      isAvailable: true,
      isExceededStock: false
    };

    component.increaseQuantity(mockItem);
    expect(mockCartService.updateQuantity).toHaveBeenCalledWith(1, 101, 3);
  });

  it('should call removeItem when clicking remove', () => {
    const mockItem: CartItem = {
      cartId: 1,
      variantId: 101,
      variantName: '16GB / 512GB',
      skuVariant: 'MBP-16-512',
      price: 40000000,
      originalPrice: 42000000,
      productId: 1,
      productName: 'MacBook Pro M5',
      productSlug: 'macbook-pro-m5',
      quantity: 2,
      subtotal: 80000000,
      availableQty: 10,
      isAvailable: true,
      isExceededStock: false
    };

    component.removeItem(mockItem);
    expect(mockCartService.removeItem).toHaveBeenCalledWith(1, 101);
  });
});
