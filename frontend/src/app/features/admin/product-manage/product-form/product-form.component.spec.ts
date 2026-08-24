import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductFormComponent } from './product-form.component';
import { ProductService } from '../../../../core/services/product.service';
import { CategoryService } from '../../../../core/services/category.service';
import { BrandService } from '../../../../core/services/brand.service';
import { UploadService } from '../../../../core/services/upload.service';
import { AttributeService } from '../../../../core/services/attribute.service';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

describe('ProductFormComponent', () => {
  let component: ProductFormComponent;
  let fixture: ComponentFixture<ProductFormComponent>;
  let attributeService: AttributeService;
  let productService: ProductService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductFormComponent],
      providers: [
        provideRouter([]),
        {
          provide: ProductService,
          useValue: {
            getById: vi.fn().mockReturnValue(of({ success: true, data: { productId: 1, name: 'Test', categoryId: 10 } })),
            create: vi.fn().mockReturnValue(of({ success: true, data: { productId: 1 } })),
            update: vi.fn().mockReturnValue(of({ success: true, data: { productId: 1 } })),
            createVariant: vi.fn().mockReturnValue(of({ success: true, data: { variantId: 10 } })),
            getDeletedVariants: vi.fn().mockReturnValue(of({ success: true, data: [] })),
            getDeletedImages: vi.fn().mockReturnValue(of({ success: true, data: [] }))
          }
        },
        {
          provide: CategoryService,
          useValue: {
            getAll: vi.fn().mockReturnValue(of({ success: true, data: [{ categoryId: 10, name: 'CPU' }] }))
          }
        },
        {
          provide: BrandService,
          useValue: {
            getAll: vi.fn().mockReturnValue(of({ success: true, data: [{ brandId: 1, name: 'Intel' }] }))
          }
        },
        {
          provide: UploadService,
          useValue: {
            uploadFile: vi.fn().mockReturnValue(of({ success: true, data: { url: 'https://example.com/test.jpg' } }))
          }
        },
        {
          provide: AttributeService,
          useValue: {
            getByCategory: vi.fn().mockReturnValue(of({
              success: true,
              data: [{ attributeId: 1, categoryId: 10, name: 'Socket', dataType: 'text', sortOrder: 1 }]
            })),
            getProductAttributes: vi.fn().mockReturnValue(of({ success: true, data: [] })),
            saveProductAttributes: vi.fn().mockReturnValue(of({ success: true, data: [] }))
          }
        }
      ]
    }).compileComponents();

    attributeService = TestBed.inject(AttributeService);
    productService = TestBed.inject(ProductService);
    fixture = TestBed.createComponent(ProductFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create ProductFormComponent and initialize', () => {
    expect(component).toBeTruthy();
    expect(component.categories().length).toBe(1);
    expect(component.brands().length).toBe(1);
  });

  it('should load category attributes when category changes', () => {
    component.form.patchValue({ categoryId: 10 });
    expect(attributeService.getByCategory).toHaveBeenCalledWith(10);
    expect(component.availableAttributes().length).toBe(1);
    expect(component.availableAttributes()[0].name).toBe('Socket');
  });

  it('should track attribute values correctly', () => {
    component.availableAttributes.set([
      { attributeId: 1, categoryId: 10, name: 'Socket', dataType: 'text', sortOrder: 1 }
    ]);
    component.onSpecChange(1, { target: { value: 'LGA1700' } } as any);
    expect(component.attributeValues()[1]).toBe('LGA1700');
  });
});
