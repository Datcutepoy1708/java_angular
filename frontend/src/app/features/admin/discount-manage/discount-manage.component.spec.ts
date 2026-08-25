import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { DiscountManageComponent } from './discount-manage.component';
import { DiscountService } from '../../../core/services/discount.service';
import { CategoryService } from '../../../core/services/category.service';

describe('DiscountManageComponent', () => {
  let component: DiscountManageComponent;
  let fixture: ComponentFixture<DiscountManageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DiscountManageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        DiscountService,
        CategoryService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DiscountManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize discountForm', () => {
    expect(component.discountForm).toBeDefined();
    expect(component.discountForm.get('code')).toBeDefined();
    expect(component.discountForm.get('discountType')).toBeDefined();
  });

  it('should open create modal with clean form', () => {
    component.openCreateModal();
    expect(component.isModalOpen()).toBe(true);
    expect(component.isEditing()).toBe(false);
    expect(component.selectedDiscountId()).toBeNull();
  });

  it('should close modals on closeModal', () => {
    component.openCreateModal();
    expect(component.isModalOpen()).toBe(true);
    component.closeModal();
    expect(component.isModalOpen()).toBe(false);
  });
});
