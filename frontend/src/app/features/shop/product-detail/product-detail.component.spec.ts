import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, vi } from 'vitest';
import { ProductDetailComponent } from './product-detail.component';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { ReviewService } from '../../../core/services/review.service';
import { InventoryService } from '../../../core/services/inventory.service';
import { AuthService } from '../../../core/services/auth.service';

describe('ProductDetailComponent', () => {
  let component: ProductDetailComponent;
  let fixture: ComponentFixture<ProductDetailComponent>;

  const mockProduct = {
    productId: 1,
    name: 'Laptop ASUS ROG Strix',
    slug: 'laptop-asus-rog-strix',
    sku: 'ROG-01',
    variants: [
      {
        variantId: 1,
        variantName: '16GB / 512GB',
        skuVariant: 'ROG-01-V1',
        price: 32000000,
        salePrice: 29000000,
        status: 'active',
      },
    ],
  };

  const mockProductService = {
    getProductBySlug: () => of({ success: true, message: 'OK', data: mockProduct }),
    getProductById: () => of({ success: true, message: 'OK', data: mockProduct }),
    getProducts: () =>
      of({
        success: true,
        message: 'OK',
        data: {
          content: [],
          page: 0,
          size: 5,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
        },
      }),
  };

  const mockCartService = {
    addToCart: vi.fn().mockReturnValue(of(true)),
    showToast: vi.fn(),
  };

  const mockReviewService = {
    getProductRatingSummary: () => of({ success: true, data: { averageRating: 5, totalReviews: 0, ratingBreakdown: {} } }),
    getProductReviews: () => of({ success: true, data: { content: [], totalElements: 0 } })
  };

  const mockInventoryService = {
    getVariantStockSummary: () => of({ success: true, data: { totalQuantity: 10, totalReserved: 0, totalAvailable: 10 } })
  };

  const mockAuthService = {
    currentUser: () => null,
    isLoggedIn: () => false
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductDetailComponent],
      providers: [
        provideRouter([]),
        { provide: ProductService, useValue: mockProductService },
        { provide: CartService, useValue: mockCartService },
        { provide: ReviewService, useValue: mockReviewService },
        { provide: InventoryService, useValue: mockInventoryService },
        { provide: AuthService, useValue: mockAuthService }
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should adjust quantity on increase and decrease', () => {
    expect(component.quantity()).toBe(1);
    component.increaseQuantity();
    expect(component.quantity()).toBe(2);
    component.decreaseQuantity();
    expect(component.quantity()).toBe(1);
    component.decreaseQuantity(); // should not go below 1
    expect(component.quantity()).toBe(1);
  });

  it('should call CartService.addToCart when clicking addToCart', () => {
    component.loadProduct('laptop-asus-rog-strix');
    component.addToCart();
    expect(mockCartService.addToCart).toHaveBeenCalledWith(1, 1);
  });
});
