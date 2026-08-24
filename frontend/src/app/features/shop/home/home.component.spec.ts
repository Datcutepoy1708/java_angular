import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { HomeComponent } from './home.component';
import { ProductService } from '../../../core/services/product.service';
import { CategoryService } from '../../../core/services/category.service';
import { BrandService } from '../../../core/services/brand.service';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  const mockProductService = {
    getProducts: () =>
      of({
        success: true,
        message: 'OK',
        data: {
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
        },
      }),
  };

  const mockCategoryService = {
    getRoots: () => of({ success: true, message: 'OK', data: [] }),
  };

  const mockBrandService = {
    getAll: () => of({ success: true, message: 'OK', data: [] }),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        provideRouter([]),
        { provide: ProductService, useValue: mockProductService },
        { provide: CategoryService, useValue: mockCategoryService },
        { provide: BrandService, useValue: mockBrandService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should switch slides on next/prev calls', () => {
    expect(component.currentSlideIndex()).toBe(0);
    component.nextSlide();
    expect(component.currentSlideIndex()).toBe(1);
    component.prevSlide();
    expect(component.currentSlideIndex()).toBe(0);
  });
});
