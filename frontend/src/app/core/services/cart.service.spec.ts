import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PLATFORM_ID, signal } from '@angular/core';
import { CartService } from './cart.service';
import { AuthService } from './auth.service';
import { ProductService } from './product.service';
import { environment } from '../../../environments/environment';
import { Cart } from '../models/cart.model';
import { ApiResponse } from '../models/auth.model';
import { of, firstValueFrom } from 'rxjs';

describe('CartService', () => {
  let service: CartService;
  let httpTesting: HttpTestingController;

  const mockIsAuthenticatedSignal = signal<boolean>(false);
  const mockAuthService = {
    isAuthenticated: mockIsAuthenticatedSignal
  };

  const mockProductService = {
    getProducts: vi.fn().mockReturnValue(of({
      success: true,
      message: 'OK',
      data: {
        content: [
          {
            productId: 1,
            name: 'Laptop Dell XPS',
            slug: 'laptop-dell-xps',
            variants: [
              {
                variantId: 101,
                variantName: 'Core i7 / 16GB',
                skuVariant: 'DELL-XPS-101',
                price: 35000000,
                salePrice: 32000000,
                images: [{ imageUrl: 'https://example.com/dell.jpg' }]
              }
            ]
          }
        ]
      }
    }))
  };

  const mockServerCart: Cart = {
    items: [
      {
        cartId: 1,
        variantId: 101,
        variantName: 'Core i7 / 16GB',
        skuVariant: 'DELL-XPS-101',
        price: 32000000,
        originalPrice: 35000000,
        imageUrl: 'https://example.com/dell.jpg',
        productId: 1,
        productName: 'Laptop Dell XPS',
        productSlug: 'laptop-dell-xps',
        quantity: 2,
        subtotal: 64000000,
        availableQty: 10,
        isAvailable: true,
        isExceededStock: false
      }
    ],
    totalItems: 1,
    totalQuantity: 2,
    totalAmount: 64000000,
    originalTotalAmount: 70000000,
    savingsAmount: 6000000
  };

  beforeEach(() => {
    localStorage.clear();
    mockIsAuthenticatedSignal.set(false);

    TestBed.configureTestingModule({
      providers: [
        CartService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService },
        { provide: ProductService, useValue: mockProductService },
        { provide: PLATFORM_ID, useValue: 'browser' }
      ]
    });

    service = TestBed.inject(CartService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('should be created and start with empty cart in guest mode', () => {
    expect(service).toBeTruthy();
    expect(service.totalItems()).toBe(0);
    expect(service.totalQuantity()).toBe(0);
    expect(service.totalAmount()).toBe(0);
  });

  it('should add item to guest cart and calculate totals', async () => {
    const success = await firstValueFrom(service.addToCart(101, 2));
    expect(success).toBe(true);
    expect(service.totalItems()).toBe(1);
    expect(service.totalQuantity()).toBe(2);
    expect(service.totalAmount()).toBe(64000000);
    expect(service.savingsAmount()).toBe(6000000);
  });

  it('should add item via server API when authenticated', async () => {
    mockIsAuthenticatedSignal.set(true);

    const promise = firstValueFrom(service.addToCart(101, 2));

    const req = httpTesting.expectOne(`${environment.apiUrl}/api/v1/cart/items`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ variantId: 101, quantity: 2 });
    req.flush({ success: true, message: 'OK', data: mockServerCart } as ApiResponse<Cart>);

    const success = await promise;
    expect(success).toBe(true);
    expect(service.totalQuantity()).toBe(2);
  });

  it('should update item quantity in guest mode', async () => {
    await firstValueFrom(service.addToCart(101, 1));
    expect(service.totalQuantity()).toBe(1);

    await firstValueFrom(service.updateQuantity(null, 101, 3));
    expect(service.totalQuantity()).toBe(3);
    expect(service.totalAmount()).toBe(96000000);
  });

  it('should remove item from guest cart', async () => {
    await firstValueFrom(service.addToCart(101, 2));
    expect(service.totalItems()).toBe(1);

    await firstValueFrom(service.removeItem(null, 101));
    expect(service.totalItems()).toBe(0);
    expect(service.totalQuantity()).toBe(0);
  });

  it('should clear guest cart', async () => {
    await firstValueFrom(service.addToCart(101, 2));
    expect(service.totalItems()).toBe(1);

    await firstValueFrom(service.clearCart());
    expect(service.totalItems()).toBe(0);
    expect(service.totalQuantity()).toBe(0);
  });
});
