import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CustomerManageComponent } from './customer-manage.component';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { AdminUser, AdminUserPage } from '../../../core/models/admin-user.model';

describe('CustomerManageComponent', () => {
  let component: CustomerManageComponent;
  let fixture: ComponentFixture<CustomerManageComponent>;
  let adminUserServiceMock: any;

  const mockCustomer: AdminUser = {
    userId: 10,
    fullName: 'Nguyễn Văn Khách',
    email: 'khach@gmail.com',
    phone: '0901234567',
    avatarUrl: null,
    gender: 'male',
    birthDate: '1995-01-01',
    status: 'active',
    emailVerified: true,
    provider: 'local',
    roles: ['ROLE_CUSTOMER'],
    createdAt: '2026-08-20T10:00:00',
    updatedAt: '2026-08-20T10:00:00',
    totalOrders: 4,
    totalSpend: 25000000
  };

  const mockPage: AdminUserPage = {
    content: [mockCustomer],
    pageNumber: 0,
    pageSize: 10,
    totalElements: 1,
    totalPages: 1,
    last: true
  };

  beforeEach(async () => {
    adminUserServiceMock = {
      getCustomersPaginated: vi.fn().mockReturnValue(
        of({ success: true, message: 'OK', data: mockPage })
      ),
      updateUser: vi.fn().mockReturnValue(
        of({ success: true, message: 'Updated', data: { ...mockCustomer, fullName: 'Nguyễn Văn Khách VIP' } })
      ),
      updateUserStatus: vi.fn().mockReturnValue(
        of({ success: true, message: 'Updated', data: { ...mockCustomer, status: 'banned' } })
      ),
      resetUserPassword: vi.fn().mockReturnValue(
        of({ success: true, message: 'Reset', data: null })
      )
    };

    await TestBed.configureTestingModule({
      imports: [CustomerManageComponent],
      providers: [
        { provide: AdminUserService, useValue: adminUserServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load customers on init', () => {
    expect(component).toBeTruthy();
    expect(adminUserServiceMock.getCustomersPaginated).toHaveBeenCalled();
    expect(component.customers().length).toBe(1);
    expect(component.activeCustomersCount()).toBe(1);
  });

  it('should open detail modal', () => {
    component.openDetailModal(mockCustomer);
    expect(component.isDetailModalOpen()).toBe(true);
    expect(component.selectedCustomer()?.userId).toBe(10);
  });

  it('should open edit modal and submit updates', () => {
    component.openEditModal(mockCustomer);
    expect(component.isEditModalOpen()).toBe(true);
    expect(component.editForm.get('fullName')?.value).toBe('Nguyễn Văn Khách');

    component.editForm.patchValue({ fullName: 'Nguyễn Văn Khách VIP' });
    component.submitEdit();

    expect(adminUserServiceMock.updateUser).toHaveBeenCalledWith(
      10,
      expect.objectContaining({ fullName: 'Nguyễn Văn Khách VIP' })
    );
    expect(component.isEditModalOpen()).toBe(false);
  });

  it('should open reset password modal and submit', () => {
    component.openResetPasswordModal(mockCustomer);
    expect(component.isResetPasswordModalOpen()).toBe(true);

    component.resetPasswordForm.patchValue({ newPassword: 'NewPassword#123' });
    component.submitResetPassword();

    expect(adminUserServiceMock.resetUserPassword).toHaveBeenCalledWith(10, { newPassword: 'NewPassword#123' });
    expect(component.isResetPasswordModalOpen()).toBe(false);
  });

  it('should prompt and toggle status', () => {
    component.promptToggleStatus(mockCustomer);
    expect(component.isConfirmStatusOpen()).toBe(true);
    expect(component.statusActionTarget()?.nextStatus).toBe('banned');

    component.confirmToggleStatus();

    expect(adminUserServiceMock.updateUserStatus).toHaveBeenCalledWith(10, { status: 'banned' });
    expect(component.isConfirmStatusOpen()).toBe(false);
  });
});
