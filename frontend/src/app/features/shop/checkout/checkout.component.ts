import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';
import { OrderService } from '../../../core/services/order.service';
import { AddressService } from '../../../core/services/address.service';
import { AuthService } from '../../../core/services/auth.service';
import { DiscountService } from '../../../core/services/discount.service';
import { SettingService } from '../../../core/services/setting.service';
import { Address } from '../../../core/models/address.model';
import { CreateOrderRequest, PaymentMethod } from '../../../core/models/order.model';
import { Discount, DiscountValidationResult } from '../../../core/models/discount.model';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly addressService = inject(AddressService);
  private readonly authService = inject(AuthService);
  private readonly discountService = inject(DiscountService);
  private readonly settingService = inject(SettingService);
  private readonly router = inject(Router);

  readonly savedAddresses = signal<Address[]>([]);
  readonly selectedAddressId = signal<number | null>(null);
  readonly useNewAddress = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  readonly enableBankTransfer = computed(() => this.settingService.publicSettings().enableBankTransfer);
  readonly enableCod = computed(() => this.settingService.publicSettings().enableCod);

  // Discount & Coupon signals
  readonly couponInput = signal<string>('');
  readonly isValidatingCoupon = signal<boolean>(false);
  readonly couponError = signal<string | null>(null);
  readonly couponSuccess = signal<string | null>(null);
  readonly appliedDiscount = signal<DiscountValidationResult | null>(null);
  readonly publicDiscounts = signal<Discount[]>([]);

  checkoutForm!: FormGroup;

  ngOnInit(): void {
    this.initForm();
    this.loadSavedAddresses();
    this.loadPublicDiscounts();
    this.loadPaymentSettings();
  }

  private loadPaymentSettings(): void {
    this.settingService.loadPublicSettings().subscribe({
      next: (settings) => {
        const current = this.checkoutForm?.get('paymentMethod')?.value;
        if (current === 'cod' && !settings.enableCod && settings.enableBankTransfer) {
          this.setPaymentMethod('bank_transfer');
        } else if (current === 'bank_transfer' && !settings.enableBankTransfer && settings.enableCod) {
          this.setPaymentMethod('cod');
        }
      }
    });
  }

  get isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  private initForm(): void {
    const currentUser = this.authService.currentUser();
    const isAuth = this.authService.isAuthenticated();

    this.checkoutForm = this.fb.group({
      receiverName: [currentUser?.fullName || '', [Validators.required, Validators.maxLength(150)]],
      receiverPhone: [currentUser?.phone || '', [Validators.required, Validators.pattern(/^[0-9]{10,11}$/)]],
      customerEmail: [currentUser?.email || '', isAuth ? [Validators.email, Validators.maxLength(150)] : [Validators.required, Validators.email, Validators.maxLength(150)]],
      province: ['', [Validators.required]],
      district: ['', [Validators.required]],
      ward: ['', [Validators.required]],
      detailAddress: ['', [Validators.required, Validators.maxLength(255)]],
      paymentMethod: ['cod' as PaymentMethod, [Validators.required]],
      note: ['', [Validators.maxLength(500)]]
    });
  }

  loadSavedAddresses(): void {
    if (!this.authService.isAuthenticated()) {
      this.useNewAddress.set(true);
      return;
    }

    this.addressService.getMyAddresses().subscribe({
      next: (res) => {
        if (res.success && res.data && res.data.length > 0) {
          this.savedAddresses.set(res.data);
          const defaultAddr = res.data.find(a => a.isDefault) || res.data[0];
          this.selectAddress(defaultAddr.addressId);
        } else {
          this.useNewAddress.set(true);
        }
      },
      error: () => {
        this.useNewAddress.set(true);
      }
    });
  }

  loadPublicDiscounts(): void {
    if (!this.authService.isAuthenticated()) {
      this.publicDiscounts.set([]);
      return;
    }

    this.discountService.getPublicDiscounts().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.publicDiscounts.set(res.data);
        }
      },
      error: () => {}
    });
  }

  selectAddress(addressId: number): void {
    this.selectedAddressId.set(addressId);
    this.useNewAddress.set(false);
  }

  switchToNewAddress(): void {
    this.selectedAddressId.set(null);
    this.useNewAddress.set(true);
  }

  setPaymentMethod(method: PaymentMethod): void {
    if (method === 'bank_transfer' && !this.enableBankTransfer()) return;
    if (method === 'cod' && !this.enableCod()) return;
    this.checkoutForm.patchValue({ paymentMethod: method });
  }

  applyCoupon(codeToApply?: string): void {
    const code = (codeToApply || this.couponInput()).trim();
    if (!code) {
      this.couponError.set('Vui lòng nhập mã giảm giá.');
      return;
    }

    if (!this.authService.isAuthenticated()) {
      this.couponError.set('Mã giảm giá chỉ áp dụng cho thành viên. Quý khách vui lòng đăng nhập tài khoản.');
      return;
    }

    this.couponError.set(null);
    this.couponSuccess.set(null);
    this.isValidatingCoupon.set(true);

    this.discountService.validateDiscount(code).subscribe({
      next: (res) => {
        this.isValidatingCoupon.set(false);
        if (res.success && res.data && res.data.valid) {
          this.appliedDiscount.set(res.data);
          this.couponSuccess.set(res.data.message || `Đã áp dụng mã ${res.data.code}`);
          this.couponInput.set(res.data.code);
        } else {
          this.couponError.set(res.message || 'Mã giảm giá không hợp lệ.');
        }
      },
      error: (err) => {
        this.isValidatingCoupon.set(false);
        const msg = err.error?.message || 'Mã giảm giá không hợp lệ hoặc đã hết lượt sử dụng.';
        this.couponError.set(msg);
      }
    });
  }

  removeCoupon(): void {
    this.appliedDiscount.set(null);
    this.couponSuccess.set(null);
    this.couponError.set(null);
    this.couponInput.set('');
  }

  get finalTotal(): number {
    const subtotal = this.cartService.cart().totalAmount;
    const discount = this.appliedDiscount()?.discountAmount || 0;
    return Math.max(0, subtotal - discount);
  }

  onSubmit(): void {
    this.errorMessage.set(null);

    if (this.cartService.cart().items.length === 0) {
      this.errorMessage.set('Giỏ hàng của bạn đang trống.');
      return;
    }

    const isAuth = this.authService.isAuthenticated();
    const paymentMethod = this.checkoutForm.get('paymentMethod')?.value as PaymentMethod;

    if (paymentMethod === 'bank_transfer' && !this.enableBankTransfer()) {
      this.errorMessage.set('Phương thức chuyển khoản qua mã QR hiện đang tạm khóa.');
      return;
    }
    if (paymentMethod === 'cod' && !this.enableCod()) {
      this.errorMessage.set('Phương thức thanh toán khi nhận hàng (COD) hiện đang tạm khóa.');
      return;
    }

    const note = this.checkoutForm.get('note')?.value;
    const discountCode = isAuth && this.appliedDiscount() ? this.appliedDiscount()!.code : undefined;

    let request: CreateOrderRequest;

    if (isAuth && !this.useNewAddress() && this.selectedAddressId()) {
      request = {
        addressId: this.selectedAddressId()!,
        paymentMethod,
        discountCode,
        note
      };
    } else {
      if (this.checkoutForm.invalid) {
        this.checkoutForm.markAllAsTouched();
        this.errorMessage.set('Vui lòng điền đầy đủ và chính xác các thông tin giao hàng.');
        return;
      }

      const formVal = this.checkoutForm.value;
      const guestItems = !isAuth
        ? this.cartService.cart().items.map(item => ({ variantId: item.variantId, quantity: item.quantity }))
        : undefined;

      request = {
        receiverName: formVal.receiverName,
        receiverPhone: formVal.receiverPhone,
        customerEmail: formVal.customerEmail ? formVal.customerEmail.trim() : undefined,
        province: formVal.province,
        district: formVal.district,
        ward: formVal.ward,
        detailAddress: formVal.detailAddress,
        paymentMethod,
        discountCode,
        note,
        items: guestItems
      };
    }

    this.isSubmitting.set(true);

    this.orderService.createOrder(request).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        if (res.success && res.data) {
          if (!isAuth) {
            this.cartService.clearCart().subscribe();
          } else {
            this.cartService.loadCart();
          }
          const pollingToken = res.data.paymentInstruction?.paymentPollingToken;
          if (pollingToken) {
            try {
              sessionStorage.setItem(`payment_polling_${res.data.orderCode}`, pollingToken);
            } catch {
              // Ignore session storage access error
            }
          }
          this.router.navigate(['/order-success', res.data.orderCode], {
            state: {
              order: res.data,
              pollingToken: pollingToken
            }
          });
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const serverMsg = err.error?.message || 'Có lỗi xảy ra trong quá trình đặt hàng. Vui lòng thử lại.';
        this.errorMessage.set(serverMsg);
      }
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0);
  }
}
