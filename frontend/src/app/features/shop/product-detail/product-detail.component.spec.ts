import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ProductDetailComponent } from './product-detail.component';
import { ProductService } from '../../../core/services/product.service';

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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductDetailComponent],
      providers: [
        provideRouter([]),
        { provide: ProductService, useValue: mockProductService },
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

  it('should toggle cartFeatureNotice when clicking addToCart', () => {
    expect(component.cartFeatureNotice()).toBe(false);
    component.addToCart();
    expect(component.cartFeatureNotice()).toBe(true);
  });
});
