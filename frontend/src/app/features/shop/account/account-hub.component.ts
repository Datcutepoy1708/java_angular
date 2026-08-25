import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService } from '../../../core/services/user.service';
import { AddressService } from '../../../core/services/address.service';
import { OrderService } from '../../../core/services/order.service';
import { UploadService } from '../../../core/services/upload.service';
import { AuthService } from '../../../core/services/auth.service';
import { GenderType, UserProfile } from '../../../core/models/user.model';
import { Address, AddressRequest } from '../../../core/models/address.model';
import { Order } from '../../../core/models/order.model';

export type AccountTab = 'profile' | 'password' | 'addresses' | 'orders';

@Component({
  selector: 'app-account-hub',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './account-hub.component.html',
  styleUrl: './account-hub.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AccountHubComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly addressService = inject(AddressService);
  private readonly orderService = inject(OrderService);
  private readonly uploadService = inject(UploadService);
  readonly authService = inject(AuthService);

  readonly activeTab = signal<AccountTab>('profile');
  readonly isLoading = signal<boolean>(false);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  // Tab 1: Profile Form
  readonly profile = signal<UserProfile | null>(null);
  readonly fullName = signal<string>('');
  readonly phone = signal<string>('');
  readonly gender = signal<GenderType | null>(null);
  readonly birthDate = signal<string>('');
  readonly avatarUrl = signal<string | null>(null);
  readonly isUploadingAvatar = signal<boolean>(false);

  // Tab 2: Password Form
  readonly oldPassword = signal<string>('');
  readonly newPassword = signal<string>('');
  readonly confirmPassword = signal<string>('');
  readonly showOldPassword = signal<boolean>(false);
  readonly showNewPassword = signal<boolean>(false);
  readonly showConfirmPassword = signal<boolean>(false);

  // Tab 3: Address Book
  readonly addresses = signal<Address[]>([]);
  readonly isAddressModalOpen = signal<boolean>(false);
  readonly editingAddressId = signal<number | null>(null);
  readonly addressForm = signal<AddressRequest>({
    receiverName: '',
    phone: '',
    province: '',
    district: '',
    ward: '',
    detailAddress: '',
    isDefault: false
  });

  // Tab 4: My Orders
  readonly orders = signal<Order[]>([]);
  readonly orderStatusFilter = signal<string>('ALL');
  readonly selectedOrderDetail = signal<Order | null>(null);
  readonly isOrderDetailModalOpen = signal<boolean>(false);
  readonly cancelReason = signal<string>('');
  readonly orderToCancel = signal<Order | null>(null);
  readonly isCancelModalOpen = signal<boolean>(false);

  ngOnInit(): void {
    this.loadProfile();
    this.loadAddresses();
    this.loadOrders();
  }

  setTab(tab: AccountTab): void {
    this.activeTab.set(tab);
    this.clearMessages();
  }

  clearMessages(): void {
    this.successMessage.set(null);
    this.errorMessage.set(null);
  }

  // ==========================================
  // TAB 1: PROFILE MANAGEMENT
  // ==========================================
  loadProfile(): void {
    this.userService.getMyProfile().subscribe({
      next: (res) => {
        if (res.data) {
          this.profile.set(res.data);
          this.fullName.set(res.data.fullName || '');
          this.phone.set(res.data.phone || '');
          this.gender.set(res.data.gender || null);
          this.birthDate.set(res.data.birthDate || '');
          this.avatarUrl.set(res.data.avatarUrl || null);
        }
      },
      error: (err) => console.error('Error loading profile', err)
    });
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      this.isUploadingAvatar.set(true);
      this.uploadService.uploadImage(file).subscribe({
        next: (url) => {
          this.avatarUrl.set(url);
          this.isUploadingAvatar.set(false);
        },
        error: (err) => {
          this.errorMessage.set('Tải ảnh đại diện thất bại: ' + (err.error?.message || 'Lỗi server'));
          this.isUploadingAvatar.set(false);
        }
      });
    }
  }

  saveProfile(): void {
    this.clearMessages();
    if (!this.fullName().trim()) {
      this.errorMessage.set('Họ và tên không được để trống');
      return;
    }

    this.isLoading.set(true);
    this.userService.updateMyProfile({
      fullName: this.fullName().trim(),
      phone: this.phone().trim() || null,
      gender: this.gender(),
      birthDate: this.birthDate() || null,
      avatarUrl: this.avatarUrl()
    }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res.data) this.profile.set(res.data);
        this.successMessage.set('Cập nhật thông tin tài khoản thành công');
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Cập nhật tài khoản thất bại');
      }
    });
  }

  // ==========================================
  // TAB 2: PASSWORD CHANGE
  // ==========================================
  changePassword(): void {
    this.clearMessages();
    if (!this.oldPassword()) {
      this.errorMessage.set('Vui lòng nhập mật khẩu hiện tại');
      return;
    }
    if (!this.newPassword() || this.newPassword().length < 6) {
      this.errorMessage.set('Mật khẩu mới phải có ít nhất 6 ký tự');
      return;
    }
    if (this.newPassword() !== this.confirmPassword()) {
      this.errorMessage.set('Mật khẩu xác nhận không khớp');
      return;
    }

    this.isLoading.set(true);
    this.userService.changePassword({
      oldPassword: this.oldPassword(),
      newPassword: this.newPassword(),
      confirmPassword: this.confirmPassword()
    }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.successMessage.set('Đổi mật khẩu thành công! Hãy ghi nhớ mật khẩu mới của bạn.');
        this.oldPassword.set('');
        this.newPassword.set('');
        this.confirmPassword.set('');
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Đổi mật khẩu thất bại');
      }
    });
  }

  // ==========================================
  // TAB 3: ADDRESS BOOK
  // ==========================================
  loadAddresses(): void {
    this.addressService.getMyAddresses().subscribe({
      next: (res) => {
        if (res.data) this.addresses.set(res.data);
      },
      error: (err) => console.error('Error loading addresses', err)
    });
  }

  openAddAddressModal(): void {
    this.editingAddressId.set(null);
    this.addressForm.set({
      receiverName: this.fullName() || '',
      phone: this.phone() || '',
      province: '',
      district: '',
      ward: '',
      detailAddress: '',
      isDefault: this.addresses().length === 0
    });
    this.isAddressModalOpen.set(true);
  }

  openEditAddressModal(addr: Address): void {
    this.editingAddressId.set(addr.addressId);
    this.addressForm.set({
      receiverName: addr.receiverName,
      phone: addr.phone,
      province: addr.province,
      district: addr.district,
      ward: addr.ward,
      detailAddress: addr.detailAddress,
      isDefault: addr.isDefault
    });
    this.isAddressModalOpen.set(true);
  }

  closeAddressModal(): void {
    this.isAddressModalOpen.set(false);
  }

  saveAddress(): void {
    const form = this.addressForm();
    if (!form.receiverName.trim() || !form.phone.trim() || !form.province.trim() || !form.detailAddress.trim()) {
      alert('Vui lòng điền đầy đủ các thông tin địa chỉ bắt buộc');
      return;
    }

    const editId = this.editingAddressId();
    if (editId) {
      this.addressService.updateAddress(editId, form).subscribe({
        next: () => {
          this.closeAddressModal();
          this.loadAddresses();
          this.successMessage.set('Đã cập nhật địa chỉ giao hàng');
        },
        error: (err) => alert(err.error?.message || 'Cập nhật địa chỉ thất bại')
      });
    } else {
      this.addressService.createAddress(form).subscribe({
        next: () => {
          this.closeAddressModal();
          this.loadAddresses();
          this.successMessage.set('Đã thêm địa chỉ giao hàng mới');
        },
        error: (err) => alert(err.error?.message || 'Thêm địa chỉ thất bại')
      });
    }
  }

  deleteAddress(addressId: number): void {
    if (confirm('Bạn có chắc chắn muốn xóa địa chỉ này khỏi sổ địa chỉ?')) {
      this.addressService.deleteAddress(addressId).subscribe({
        next: () => {
          this.loadAddresses();
          this.successMessage.set('Đã xóa địa chỉ thành công');
        },
        error: (err) => alert(err.error?.message || 'Xóa địa chỉ thất bại')
      });
    }
  }

  setDefaultAddress(addressId: number): void {
    this.addressService.setDefaultAddress(addressId).subscribe({
      next: () => {
        this.loadAddresses();
        this.successMessage.set('Đã đặt làm địa chỉ mặc định');
      },
      error: (err) => alert(err.error?.message || 'Thao tác thất bại')
    });
  }

  // ==========================================
  // TAB 4: MY ORDERS
  // ==========================================
  loadOrders(): void {
    this.orderService.getMyOrders(0, 50).subscribe({
      next: (res) => {
        if (res.data && res.data.content) {
          this.orders.set(res.data.content);
        }
      },
      error: (err) => console.error('Error loading orders', err)
    });
  }

  getFilteredOrders(): Order[] {
    const filter = this.orderStatusFilter().toLowerCase();
    const all = this.orders();
    if (filter === 'all') return all;
    return all.filter(o => o.orderStatus.toLowerCase() === filter);
  }

  viewOrderDetail(order: Order): void {
    this.selectedOrderDetail.set(order);
    this.isOrderDetailModalOpen.set(true);
  }

  closeOrderDetailModal(): void {
    this.isOrderDetailModalOpen.set(false);
    this.selectedOrderDetail.set(null);
  }

  openCancelModal(order: Order): void {
    this.orderToCancel.set(order);
    this.cancelReason.set('');
    this.isCancelModalOpen.set(true);
  }

  closeCancelModal(): void {
    this.isCancelModalOpen.set(false);
    this.orderToCancel.set(null);
  }

  confirmCancelOrder(): void {
    const order = this.orderToCancel();
    if (!order) return;

    this.orderService.cancelMyOrder(order.orderCode, this.cancelReason() || 'Khách hàng yêu cầu hủy').subscribe({
      next: () => {
        this.closeCancelModal();
        this.loadOrders();
        this.successMessage.set(`Đơn hàng ${order.orderCode} đã được hủy thành công.`);
      },
      error: (err) => alert(err.error?.message || 'Hủy đơn hàng thất bại')
    });
  }

  formatCurrency(value: number | undefined): string {
    if (value === undefined || value === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
  }

  getStatusClass(status: string): string {
    return status ? status.toLowerCase() : 'pending';
  }

  getStatusLabel(status: string): string {
    const key = (status || '').toLowerCase();
    const map: Record<string, string> = {
      pending: 'Chờ xác nhận',
      confirmed: 'Đã xác nhận',
      processing: 'Đang chuẩn bị hàng',
      shipping: 'Đang giao hàng',
      delivered: 'Đã giao hàng',
      completed: 'Hoàn tất',
      cancelled: 'Đã hủy',
      refunded: 'Đã hoàn tiền'
    };
    return map[key] || status;
  }
}
