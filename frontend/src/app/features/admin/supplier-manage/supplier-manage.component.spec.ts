import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SupplierManageComponent } from './supplier-manage.component';
import { SupplierService } from '../../../core/services/supplier.service';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { SupplierPage, SupplierResponse } from '../../../core/models/supplier.model';

describe('SupplierManageComponent', () => {
  let component: SupplierManageComponent;
  let fixture: ComponentFixture<SupplierManageComponent>;
  let supplierServiceMock: any;

  const mockSupplier: SupplierResponse = {
    supplierId: 1,
    name: 'Công ty TNHH ASUS Việt Nam',
    contactName: 'Nguyễn Văn A',
    phone: '0901234567',
    email: 'contact@asus.vn',
    address: 'Hà Nội',
    status: 'active',
    createdAt: '2026-08-20T10:00:00',
    productCount: 5
  };

  const mockPage: SupplierPage = {
    content: [mockSupplier],
    pageNumber: 0,
    pageSize: 10,
    totalElements: 1,
    totalPages: 1,
    last: true
  };

  beforeEach(async () => {
    supplierServiceMock = {
      getSuppliersPaginated: vi.fn().mockReturnValue(
        of({ success: true, message: 'OK', data: mockPage })
      ),
      createSupplier: vi.fn().mockReturnValue(
        of({ success: true, message: 'Created', data: { ...mockSupplier, supplierId: 2, name: 'FPT Synnex' } })
      ),
      updateSupplier: vi.fn().mockReturnValue(
        of({ success: true, message: 'Updated', data: { ...mockSupplier, name: 'ASUS Vietnam Updated' } })
      ),
      deleteSupplier: vi.fn().mockReturnValue(
        of({ success: true, message: 'Deleted', data: null })
      )
    };

    await TestBed.configureTestingModule({
      imports: [SupplierManageComponent],
      providers: [{ provide: SupplierService, useValue: supplierServiceMock }]
    }).compileComponents();

    fixture = TestBed.createComponent(SupplierManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component and load suppliers on init', () => {
    expect(component).toBeTruthy();
    expect(supplierServiceMock.getSuppliersPaginated).toHaveBeenCalled();
    expect(component.suppliers().length).toBe(1);
    expect(component.totalElements()).toBe(1);
  });

  it('should open create modal and reset form', () => {
    component.openCreateModal();
    expect(component.isModalOpen()).toBe(true);
    expect(component.isEditing()).toBe(false);
    expect(component.supplierForm.get('status')?.value).toBe('active');
  });

  it('should open edit modal with populated form', () => {
    component.openEditModal(mockSupplier);
    expect(component.isModalOpen()).toBe(true);
    expect(component.isEditing()).toBe(true);
    expect(component.selectedSupplierId()).toBe(1);
    expect(component.supplierForm.get('name')?.value).toBe('Công ty TNHH ASUS Việt Nam');
  });

  it('should create new supplier when form is valid', () => {
    component.openCreateModal();
    component.supplierForm.setValue({
      name: 'FPT Synnex',
      contactName: 'Trần B',
      phone: '0987654321',
      email: 'fpt@synnex.vn',
      address: 'HCM',
      status: 'active'
    });

    component.saveSupplier();

    expect(supplierServiceMock.createSupplier).toHaveBeenCalled();
    expect(component.isModalOpen()).toBe(false);
  });

  it('should update existing supplier when edit form is saved', () => {
    component.openEditModal(mockSupplier);
    component.supplierForm.patchValue({ name: 'ASUS Vietnam Updated' });

    component.saveSupplier();

    expect(supplierServiceMock.updateSupplier).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ name: 'ASUS Vietnam Updated' })
    );
    expect(component.isModalOpen()).toBe(false);
  });

  it('should delete supplier after confirmation', () => {
    component.openConfirmDelete(mockSupplier);
    expect(component.isConfirmDeleteOpen()).toBe(true);

    component.confirmDelete();

    expect(supplierServiceMock.deleteSupplier).toHaveBeenCalledWith(1);
    expect(component.isConfirmDeleteOpen()).toBe(false);
  });
});
