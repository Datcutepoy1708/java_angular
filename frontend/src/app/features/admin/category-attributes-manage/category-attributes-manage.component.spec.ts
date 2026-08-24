import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CategoryAttributesManageComponent } from './category-attributes-manage.component';
import { AttributeService } from '../../../core/services/attribute.service';
import { CategoryService } from '../../../core/services/category.service';
import { of } from 'rxjs';

describe('CategoryAttributesManageComponent', () => {
  let component: CategoryAttributesManageComponent;
  let fixture: ComponentFixture<CategoryAttributesManageComponent>;
  let attributeService: AttributeService;
  let categoryService: CategoryService;

  const mockCategories = [
    { categoryId: 10, name: 'CPU - Bộ vi xử lý', slug: 'cpu' }
  ];

  const mockAttributes = [
    { attributeId: 1, categoryId: 10, name: 'Socket', dataType: 'text' as const, sortOrder: 1 }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoryAttributesManageComponent],
      providers: [
        {
          provide: AttributeService,
          useValue: {
            getByCategory: vi.fn().mockReturnValue(of({ success: true, data: mockAttributes })),
            create: vi.fn().mockReturnValue(of({ success: true, data: mockAttributes[0] })),
            update: vi.fn().mockReturnValue(of({ success: true, data: mockAttributes[0] })),
            delete: vi.fn().mockReturnValue(of({ success: true, data: null }))
          }
        },
        {
          provide: CategoryService,
          useValue: {
            getAll: vi.fn().mockReturnValue(of({ success: true, data: mockCategories }))
          }
        }
      ]
    }).compileComponents();

    attributeService = TestBed.inject(AttributeService);
    categoryService = TestBed.inject(CategoryService);
    fixture = TestBed.createComponent(CategoryAttributesManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load initial categories and attributes', () => {
    expect(component).toBeTruthy();
    expect(component.categories().length).toBe(1);
    expect(component.selectedCategoryId()).toBe(10);
    expect(component.attributes().length).toBe(1);
  });

  it('should open and populate modal in edit mode', () => {
    component.openEditModal(mockAttributes[0]);
    expect(component.isModalOpen()).toBe(true);
    expect(component.modalMode()).toBe('edit');
    expect(component.attributeForm.get('name')?.value).toBe('Socket');
  });

  it('should call delete service on confirm', () => {
    component.confirmDelete(mockAttributes[0]);
    expect(component.isConfirmOpen()).toBe(true);

    component.onDeleteConfirmed();
    expect(attributeService.delete).toHaveBeenCalledWith(1);
  });
});
