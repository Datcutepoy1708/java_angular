import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';
import { OrderService } from '../../../core/services/order.service';
import { AddressService } from '../../../core/services/address.service';
import { AuthService } from '../../../core/services/auth.service';
import { DiscountService } from '../../../core/services/discount.service';
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
  private readonly router = inject(Router);

  readonly savedAddresses = signal<Address[]>([]);
  readonly selectedAddressId = signal<number | null>(null);
  readonly useNewAddress = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

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
  }

  private initForm(): void {
    const currentUser = this.authService.currentUser();

    this.checkoutForm = this.fb.group({
      receiverName: [currentUser?.fullName || '', [Validators.required, Validators.maxLength(150)]],
      receiverPhone: [currentUser?.phone || '', [Validators.required, Validators.pattern(/^[0-9]{10,11}$/)]],
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
    this.checkoutForm.patchValue({ paymentMethod: method });
  }

  applyCoupon(codeToApply?: string): void {
    const code = (codeToApply || this.couponInput()).trim();
    if (!code) {
      this.couponError.set('Vui lòng nhập mã giảm giá.');
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

    const paymentMethod = this.checkoutForm.get('paymentMethod')?.value as PaymentMethod;
    const note = this.checkoutForm.get('note')?.value;
    const discountCode = this.appliedDiscount() ? this.appliedDiscount()!.code : undefined;

    let request: CreateOrderRequest;

    if (!this.useNewAddress() && this.selectedAddressId()) {
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
      request = {
        receiverName: formVal.receiverName,
        receiverPhone: formVal.receiverPhone,
        province: formVal.province,
        district: formVal.district,
        ward: formVal.ward,
        detailAddress: formVal.detailAddress,
        paymentMethod,
        discountCode,
        note
      };
    }

    this.isSubmitting.set(true);

    this.orderService.createOrder(request).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        if (res.success && res.data) {
          // Clear cart in service
          this.cartService.loadCart();
          this.router.navigate(['/order-success', res.data.orderCode]);
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
