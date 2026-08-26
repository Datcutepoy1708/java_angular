import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReturnService } from '../../../core/services/return.service';
import {
  ReturnDetail,
  ReturnFilter,
  ReturnMetrics,
  ReturnStatus,
  ItemCondition
} from '../../../core/models/return.model';
import { InventoryService } from '../../../core/services/inventory.service';
import { Warehouse } from '../../../core/models/inventory.model';

@Component({
  selector: 'app-return-manage',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './return-manage.component.html',
  styleUrl: './return-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReturnManageComponent implements OnInit {
  private readonly returnService = inject(ReturnService);
  private readonly inventoryService = inject(InventoryService);

  readonly requests = signal<ReturnDetail[]>([]);
  readonly metrics = signal<ReturnMetrics | null>(null);
  readonly warehouses = signal<Warehouse[]>([]);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly isLoading = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Selected Return Request for Detail & Action Modal
  readonly selectedRequest = signal<ReturnDetail | null>(null);
  readonly showDetailModal = signal<boolean>(false);
  readonly activeModalTab = signal<'DETAIL' | 'REVIEW' | 'RECEIVE' | 'REFUND'>('DETAIL');

  // Preview Image Modal
  readonly previewImageUrl = signal<string | null>(null);

  // Form states for Actions
  reviewForm = {
    approved: true,
    adminNote: ''
  };

  receiveForm = {
    warehouseId: 1,
    adminNote: '',
    itemConditions: {} as Record<number, ItemCondition>
  };

  refundForm = {
    refundTransactionCode: '',
    adminNote: ''
  };

  // Filter State
  filter: ReturnFilter = {
    keyword: '',
    status: '',
    reason: '',
    fromDate: '',
    toDate: '',
    page: 0,
    size: 15,
    sortBy: 'requestedAt',
    sortDirection: 'DESC'
  };

  readonly statusTabs: { label: string; value: string; countKey?: keyof ReturnMetrics }[] = [
    { label: 'Tất cả', value: '' },
    { label: 'Chờ duyệt', value: 'REQUESTED', countKey: 'pendingReviewCount' },
    { label: 'Đã duyệt (Chờ nhận hàng)', value: 'APPROVED', countKey: 'awaitingItemCount' },
    { label: 'Đã nhận hàng tại kho', value: 'ITEM_RECEIVED' },
    { label: 'Đã hoàn tiền & Hoàn kho', value: 'REFUNDED', countKey: 'refundedCount' },
    { label: 'Đã từ chối', value: 'REJECTED', countKey: 'rejectedCount' }
  ];

  ngOnInit(): void {
    this.loadMetrics();
    this.loadWarehouses();
    this.loadRequests();
  }

  loadMetrics(): void {
    this.returnService.getReturnMetrics().subscribe({
      next: (res) => {
        if (res.success) {
          this.metrics.set(res.data);
        }
      }
    });
  }

  loadWarehouses(): void {
    this.inventoryService.getWarehouses().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.warehouses.set(res.data);
          if (res.data.length > 0) {
            this.receiveForm.warehouseId = res.data[0].warehouseId;
          }
        }
      }
    });
  }

  loadRequests(page: number = 0): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.filter.page = page;

    this.returnService.getAdminReturnRequests(this.filter).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.requests.set(res.data.content || []);
          this.totalElements.set(res.data.totalElements || 0);
          this.totalPages.set(res.data.totalPages || 0);
          this.currentPage.set(res.data.number || 0);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || 'Không thể tải danh sách yêu cầu đổi trả.');
        this.isLoading.set(false);
      }
    });
  }

  onTabChange(status: string): void {
    this.filter.status = status;
    this.loadRequests(0);
  }

  onFilterChange(): void {
    this.loadRequests(0);
  }

  resetFilter(): void {
    this.filter = {
      keyword: '',
      status: '',
      reason: '',
      fromDate: '',
      toDate: '',
      page: 0,
      size: 15,
      sortBy: 'requestedAt',
      sortDirection: 'DESC'
    };
    this.loadRequests(0);
  }

  openDetailModal(req: ReturnDetail, tab: 'DETAIL' | 'REVIEW' | 'RECEIVE' | 'REFUND' = 'DETAIL'): void {
    this.selectedRequest.set(req);
    this.activeModalTab.set(tab);
    this.reviewForm = { approved: true, adminNote: '' };
    this.refundForm = { refundTransactionCode: '', adminNote: '' };

    const conditions: Record<number, ItemCondition> = {};
    if (req.items) {
      for (const item of req.items) {
        conditions[item.id] = item.itemCondition || 'OPENED';
      }
    }
    this.receiveForm = {
      warehouseId: req.restockWarehouseId || (this.warehouses()[0]?.warehouseId ?? 1),
      adminNote: '',
      itemConditions: conditions
    };

    this.showDetailModal.set(true);
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.selectedRequest.set(null);
  }

  submitReview(): void {
    const current = this.selectedRequest();
    if (!current) return;

    this.isSubmitting.set(true);
    this.returnService.reviewReturnRequest(current.returnId, this.reviewForm).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.showSuccess('Cập nhật trạng thái duyệt thành công');
        this.closeDetailModal();
        this.loadRequests(this.currentPage());
        this.loadMetrics();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        alert(err?.error?.message || 'Có lỗi xảy ra khi duyệt yêu cầu.');
      }
    });
  }

  submitReceive(): void {
    const current = this.selectedRequest();
    if (!current) return;

    const itemConditionsList = Object.entries(this.receiveForm.itemConditions).map(([id, condition]) => ({
      returnItemId: Number(id),
      condition
    }));

    this.isSubmitting.set(true);
    this.returnService.receiveReturnedItems(current.returnId, {
      warehouseId: this.receiveForm.warehouseId,
      adminNote: this.receiveForm.adminNote,
      itemConditions: itemConditionsList
    }).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.showSuccess('Đã tiếp nhận hàng thành công tại kho');
        this.closeDetailModal();
        this.loadRequests(this.currentPage());
        this.loadMetrics();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        alert(err?.error?.message || 'Có lỗi xảy ra khi tiếp nhận hàng.');
      }
    });
  }

  submitRefund(): void {
    const current = this.selectedRequest();
    if (!current) return;

    if (!this.refundForm.refundTransactionCode || !this.refundForm.refundTransactionCode.trim()) {
      alert('Vui lòng nhập mã giao dịch chuyển khoản ngân hàng hoàn tiền.');
      return;
    }

    this.isSubmitting.set(true);
    this.returnService.processRefund(current.returnId, this.refundForm).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.showSuccess('Đã hoàn tiền và hoàn nhập kho thành công');
        this.closeDetailModal();
        this.loadRequests(this.currentPage());
        this.loadMetrics();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        alert(err?.error?.message || 'Có lỗi xảy ra khi xử lý hoàn tiền.');
      }
    });
  }

  openImagePreview(url: string): void {
    this.previewImageUrl.set(url);
  }

  closeImagePreview(): void {
    this.previewImageUrl.set(null);
  }

  private showSuccess(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(null), 4000);
  }

  getStatusBadgeClass(status: ReturnStatus): string {
    switch (status) {
      case 'REQUESTED':
        return 'badge-warning';
      case 'APPROVED':
        return 'badge-info';
      case 'ITEM_RECEIVED':
        return 'badge-primary';
      case 'REFUNDED':
        return 'badge-success';
      case 'REJECTED':
        return 'badge-danger';
      case 'CANCELLED':
        return 'badge-secondary';
      default:
        return 'badge-secondary';
    }
  }

  getStatusLabel(status: ReturnStatus): string {
    switch (status) {
      case 'REQUESTED':
        return 'Chờ duyệt';
      case 'APPROVED':
        return 'Đã duyệt (Chờ gửi hàng)';
      case 'ITEM_RECEIVED':
        return 'Đã nhận tại kho';
      case 'REFUNDED':
        return 'Đã hoàn tiền & kho';
      case 'REJECTED':
        return 'Đã từ chối';
      case 'CANCELLED':
        return 'Khách đã hủy';
      default:
        return status;
    }
  }

  getReasonLabel(reason: string): string {
    switch (reason) {
      case 'DEFECTIVE':
        return 'Lỗi kỹ thuật / Hỏng hóc';
      case 'WRONG_ITEM':
        return 'Giao sai sản phẩm';
      case 'DAMAGED_IN_TRANSIT':
        return 'Hư hại do vận chuyển';
      case 'NOT_AS_DESCRIBED':
        return 'Không đúng mô tả';
      case 'CHANGE_OF_MIND':
        return 'Đổi ý không muốn mua';
      case 'OTHER':
        return 'Lý do khác';
      default:
        return reason;
    }
  }

  getConditionLabel(condition: string): string {
    switch (condition) {
      case 'NEW_SEAL':
        return 'Nguyên Seal (Mới 100%)';
      case 'OPENED':
        return 'Đã mở hộp (Hoạt động tốt)';
      case 'DEFECTIVE':
        return 'Lỗi kỹ thuật (Cần bảo hành)';
      case 'DAMAGED':
        return 'Móp vỡ / Hư hỏng vật lý';
      default:
        return condition;
    }
  }
}
