import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ProductCardComponent } from './product-card.component';
import { ProductResponse } from '../../../core/models/product.model';

describe('ProductCardComponent', () => {
  let component: ProductCardComponent;
  let fixture: ComponentFixture<ProductCardComponent>;

  const mockProduct = {
    productId: 1,
    categoryId: 1,
    categoryName: 'Laptop Gaming',
    categorySlug: 'laptop-gaming',
    brandId: 1,
    brandName: 'ASUS',
    brandSlug: 'asus',
    supplierId: 1,
    supplierName: 'ASUS VN',
    name: 'Laptop ASUS ROG Strix G16',
    slug: 'laptop-asus-rog-strix-g16',
    sku: 'ROG-G16',
    shortDesc: 'Laptop gaming đỉnh cao',
    description: '<p>Mô tả chi tiết</p>',
    warrantyMonths: 24,
    status: 'active' as const,
    viewCount: 150,
    mainImageUrl: 'http://localhost:8080/uploads/products/asus-rog.png',
    images: [
      {
        imageId: 1,
        productId: 1,
        variantId: null,
        imageUrl: 'http://localhost:8080/uploads/products/asus-rog.png',
        imageType: 'MAIN' as const,
        altText: 'ROG G16',
        sortOrder: 0,
      },
    ],
    variants: [
      {
        variantId: 1,
        productId: 1,
        productName: 'Laptop ASUS ROG Strix G16',
        variantName: '16GB / 512GB',
        skuVariant: 'ROG-G16-16-512',
        price: 32990000,
        salePrice: 29990000,
        costPrice: 25000000,
        status: 'active' as const,
        mainImageUrl: null,
        images: [],
        createdAt: '2026-08-24T00:00:00Z',
      },
      {
        variantId: 2,
        productId: 1,
        productName: 'Laptop ASUS ROG Strix G16',
        variantName: '32GB / 1TB',
        skuVariant: 'ROG-G16-32-1T',
        price: 39990000,
        salePrice: 37990000,
        costPrice: 30000000,
        status: 'active' as const,
        mainImageUrl: null,
        images: [],
        createdAt: '2026-08-24T00:00:00Z',
      },
    ],
  } as unknown as ProductResponse;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductCardComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductCardComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('product', mockProduct);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should calculate lowest active price and discount percent correctly', () => {
    const priceInfo = component.getDisplayPrice(mockProduct);
    expect(priceInfo.price).toBe(32990000);
    expect(priceInfo.salePrice).toBe(29990000);
    expect(priceInfo.discountPercent).toBe(9); // (32990000 - 29990000) / 32990000 ~ 9.09% -> 9%
  });

  it('should emit addToCart event on button click', () => {
    let emittedProduct: ProductResponse | null = null;
    component.addToCart.subscribe((p) => (emittedProduct = p));

    const button = fixture.nativeElement.querySelector('.quick-cart-btn');
    button.click();

    expect(emittedProduct).toEqual(mockProduct);
  });
});
