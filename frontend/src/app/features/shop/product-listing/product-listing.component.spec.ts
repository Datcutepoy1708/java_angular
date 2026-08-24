import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ProductListingComponent } from './product-listing.component';
import { ProductService } from '../../../core/services/product.service';
import { CategoryService } from '../../../core/services/category.service';
import { BrandService } from '../../../core/services/brand.service';
import { AttributeService } from '../../../core/services/attribute.service';

describe('ProductListingComponent', () => {
  let component: ProductListingComponent;
  let fixture: ComponentFixture<ProductListingComponent>;

  const mockProductService = {
    getProducts: () =>
      of({
        success: true,
        message: 'OK',
        data: {
          content: [],
          page: 0,
          size: 12,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
        },
      }),
  };

  const mockCategoryService = {
    getTree: () => of({ success: true, message: 'OK', data: [] }),
  };

  const mockBrandService = {
    getAll: () => of({ success: true, message: 'OK', data: [] }),
  };

  const mockAttributeService = {
    getByCategory: () =>
      of({
        success: true,
        message: 'OK',
        data: [
          { attributeId: 1, categoryId: 10, name: 'Socket', dataType: 'text', sortOrder: 1 }
        ]
      }),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductListingComponent],
      providers: [
        provideRouter([]),
        { provide: ProductService, useValue: mockProductService },
        { provide: CategoryService, useValue: mockCategoryService },
        { provide: BrandService, useValue: mockBrandService },
        { provide: AttributeService, useValue: mockAttributeService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductListingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should update price preset when selected', () => {
    component.selectPricePreset({ label: '10 - 20 triệu', min: 10000000, max: 20000000 });
    expect(component.selectedMinPrice()).toBe(10000000);
    expect(component.selectedMaxPrice()).toBe(20000000);
  });

  it('should toggle attribute filter and build query string correctly', () => {
    component.toggleAttributeFilter(1, 'LGA1700');
    expect(component.isAttributeSelected(1, 'LGA1700')).toBe(true);
    expect(component.buildAttributeQueryString()).toBe('1:LGA1700');

    component.toggleAttributeFilter(1, 'AM5');
    expect(component.buildAttributeQueryString()).toBe('1:LGA1700,AM5');

    component.toggleAttributeFilter(1, 'LGA1700');
    expect(component.buildAttributeQueryString()).toBe('1:AM5');
  });

  it('should parse attribute query param accurately', () => {
    component.parseAttributeParams('1:LGA1700,AM5;2:24,16');
    expect(component.isAttributeSelected(1, 'LGA1700')).toBe(true);
    expect(component.isAttributeSelected(1, 'AM5')).toBe(true);
    expect(component.isAttributeSelected(2, '24')).toBe(true);
    expect(component.isAttributeSelected(2, '16')).toBe(true);
  });
});
