import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { CheckoutComponent } from './checkout.component';
import { CartService } from '../../../core/services/cart.service';
import { OrderService } from '../../../core/services/order.service';
import { AddressService } from '../../../core/services/address.service';
import { AuthService } from '../../../core/services/auth.service';
import { DiscountService } from '../../../core/services/discount.service';

import { SettingService } from '../../../core/services/setting.service';

describe('CheckoutComponent', () => {
  let component: CheckoutComponent;
  let fixture: ComponentFixture<CheckoutComponent>;
  let settingService: SettingService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CheckoutComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        CartService,
        OrderService,
        AddressService,
        AuthService,
        DiscountService,
        SettingService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CheckoutComponent);
    component = fixture.componentInstance;
    settingService = TestBed.inject(SettingService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize checkout form with default payment method COD', () => {
    expect(component.checkoutForm).toBeDefined();
    expect(component.checkoutForm.get('paymentMethod')?.value).toBe('cod');
  });

  it('should set couponError when applying empty coupon code', () => {
    component.couponInput.set('   ');
    component.applyCoupon();
    expect(component.couponError()).toBe('Vui lòng nhập mã giảm giá.');
  });

  it('should clear applied coupon on removeCoupon', () => {
    component.couponInput.set('SALE10');
    component.appliedDiscount.set({
      valid: true,
      discountId: 1,
      code: 'SALE10',
      discountType: 'percent',
      discountValue: 10,
      discountAmount: 50000,
      subtotal: 500000,
      finalTotal: 450000,
      message: 'OK'
    });
    component.removeCoupon();
    expect(component.appliedDiscount()).toBeNull();
    expect(component.couponInput()).toBe('');
  });

  it('should not allow selecting bank_transfer if enableBankTransfer is false', () => {
    settingService.publicSettings.set({
      ...settingService.publicSettings(),
      enableBankTransfer: false,
      enableCod: true
    });
    component.checkoutForm.patchValue({ paymentMethod: 'cod' });

    component.setPaymentMethod('bank_transfer');
    expect(component.checkoutForm.get('paymentMethod')?.value).toBe('cod');
  });

  it('should allow selecting bank_transfer if enableBankTransfer is true', () => {
    settingService.publicSettings.set({
      ...settingService.publicSettings(),
      enableBankTransfer: true,
      enableCod: true
    });

    component.setPaymentMethod('bank_transfer');
    expect(component.checkoutForm.get('paymentMethod')?.value).toBe('bank_transfer');
  });
});

