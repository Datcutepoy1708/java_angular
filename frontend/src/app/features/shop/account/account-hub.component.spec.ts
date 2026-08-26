import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AccountHubComponent } from './account-hub.component';
import { UserService } from '../../../core/services/user.service';
import { AddressService } from '../../../core/services/address.service';
import { OrderService } from '../../../core/services/order.service';
import { UploadService } from '../../../core/services/upload.service';
import { AuthService } from '../../../core/services/auth.service';
import { ReturnService } from '../../../core/services/return.service';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { provideRouter } from '@angular/router';
import { UserProfile } from '../../../core/models/user.model';
import { Address } from '../../../core/models/address.model';

describe('AccountHubComponent', () => {
  let component: AccountHubComponent;
  let fixture: ComponentFixture<AccountHubComponent>;

  const mockProfile: UserProfile = {
    userId: 1,
    fullName: 'Nguyễn Văn A',
    email: 'an@example.com',
    phone: '0987654321',
    avatarUrl: null,
    gender: 'MALE',
    birthDate: '1995-05-15',
    status: 'ACTIVE',
    emailVerified: true,
    provider: 'LOCAL',
    roles: ['ROLE_CUSTOMER'],
    createdAt: '2026-08-01T00:00:00'
  };

  const mockAddresses: Address[] = [
    {
      addressId: 10,
      receiverName: 'Nguyễn Văn A',
      phone: '0987654321',
      province: 'Hà Nội',
      district: 'Cầu Giấy',
      ward: 'Dịch Vọng',
      detailAddress: 'Số 123 Đường Cầu Giấy',
      isDefault: true
    }
  ];

  beforeEach(async () => {
    const userServiceMock = {
      getMyProfile: vi.fn().mockReturnValue(of({ success: true, data: mockProfile })),
      updateMyProfile: vi.fn().mockReturnValue(of({ success: true, data: mockProfile })),
      changePassword: vi.fn().mockReturnValue(of({ success: true, data: null }))
    };

    const addressServiceMock = {
      getMyAddresses: vi.fn().mockReturnValue(of({ success: true, data: mockAddresses })),
      createAddress: vi.fn().mockReturnValue(of({ success: true, data: mockAddresses[0] })),
      updateAddress: vi.fn().mockReturnValue(of({ success: true, data: mockAddresses[0] })),
      deleteAddress: vi.fn().mockReturnValue(of({ success: true, data: null })),
      setDefaultAddress: vi.fn().mockReturnValue(of({ success: true, data: mockAddresses[0] }))
    };

    const orderServiceMock = {
      getMyOrders: vi.fn().mockReturnValue(of({ success: true, data: { content: [], totalElements: 0 } })),
      cancelMyOrder: vi.fn().mockReturnValue(of({ success: true, data: null }))
    };

    const uploadServiceMock = {
      uploadImage: vi.fn().mockReturnValue(of('https://example.com/avatar.png'))
    };

    const authServiceMock = {
      logout: vi.fn()
    };

    const returnServiceMock = {
      getMyReturnRequests: vi.fn().mockReturnValue(of({ success: true, data: { content: [], totalElements: 0 } })),
      createReturnRequest: vi.fn().mockReturnValue(of({ success: true, data: { returnId: 1, returnCode: 'RET-1' } }))
    };

    await TestBed.configureTestingModule({
      imports: [AccountHubComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userServiceMock },
        { provide: AddressService, useValue: addressServiceMock },
        { provide: OrderService, useValue: orderServiceMock },
        { provide: UploadService, useValue: uploadServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: ReturnService, useValue: returnServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AccountHubComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize and load user profile and addresses', () => {
    expect(component).toBeTruthy();
    expect(component.fullName()).toBe('Nguyễn Văn A');
    expect(component.addresses().length).toBe(1);
    expect(component.activeTab()).toBe('profile');
  });

  it('should switch tabs', () => {
    component.setTab('orders');
    expect(component.activeTab()).toBe('orders');

    component.setTab('addresses');
    expect(component.activeTab()).toBe('addresses');

    component.setTab('password');
    expect(component.activeTab()).toBe('password');
  });

  it('should open and close address modal', () => {
    component.openAddAddressModal();
    expect(component.isAddressModalOpen()).toBe(true);

    component.closeAddressModal();
    expect(component.isAddressModalOpen()).toBe(false);
  });
});
