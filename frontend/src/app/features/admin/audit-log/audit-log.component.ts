import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditLogService } from '../../../core/services/audit-log.service';
import { AuditLogFilter, AuditLogItem } from '../../../core/models/audit-log.model';

@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-log.component.html',
  styleUrl: './audit-log.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuditLogComponent implements OnInit {
  private readonly auditLogService = inject(AuditLogService);

  readonly logs = signal<AuditLogItem[]>([]);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(15);
  readonly isLoading = signal<boolean>(false);
  readonly isExporting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Selected Log for JSON Diff modal
  readonly selectedLog = signal<AuditLogItem | null>(null);
  readonly showDetailModal = signal<boolean>(false);

  // Filter State
  filter: AuditLogFilter = {
    keyword: '',
    module: '',
    actionType: '',
    status: '',
    fromDate: '',
    toDate: '',
    page: 0,
    size: 15,
    sortBy: 'createdAt',
    sortDirection: 'DESC'
  };

  readonly modules = [
    { label: 'Tất cả phân hệ', value: '' },
    { label: 'Vai trò & Quyền (ROLE)', value: 'ROLE' },
    { label: 'Nhân sự (STAFF)', value: 'STAFF' },
    { label: 'Khách hàng (CUSTOMER)', value: 'CUSTOMER' },
    { label: 'Đơn hàng (ORDER)', value: 'ORDER' },
    { label: 'Kho hàng (INVENTORY)', value: 'INVENTORY' },
    { label: 'Sản phẩm (PRODUCT)', value: 'PRODUCT' },
    { label: 'Mã giảm giá (DISCOUNT)', value: 'DISCOUNT' },
    { label: 'Đổi trả & Hoàn tiền (RETURN_REFUND)', value: 'RETURN_REFUND' },
    { label: 'Cài đặt hệ thống (SETTING)', value: 'SETTING' }
  ];

  readonly actionTypes = [
    { label: 'Tất cả hành vi', value: '' },
    { label: 'Tạo mới (CREATE)', value: 'CREATE' },
    { label: 'Cập nhật (UPDATE)', value: 'UPDATE' },
    { label: 'Xóa (DELETE)', value: 'DELETE' },
    { label: 'Đổi trạng thái (STATUS_CHANGE)', value: 'STATUS_CHANGE' },
    { label: 'Hoàn tiền / Hoàn kho (REFUND)', value: 'REFUND' },
    { label: 'Đăng nhập (LOGIN)', value: 'LOGIN' },
    { label: 'Xuất dữ liệu (EXPORT)', value: 'EXPORT' }
  ];

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(page: number = 0): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.filter.page = page;

    this.auditLogService.getAuditLogs(this.filter).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.logs.set(res.data.content || []);
          this.totalElements.set(res.data.totalElements || 0);
          this.totalPages.set(res.data.totalPages || 0);
          this.currentPage.set(res.data.number || 0);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || 'Không thể tải danh sách nhật ký kiểm toán.');
        this.isLoading.set(false);
      }
    });
  }

  onFilterChange(): void {
    this.loadLogs(0);
  }

  resetFilter(): void {
    this.filter = {
      keyword: '',
      module: '',
      actionType: '',
      status: '',
      fromDate: '',
      toDate: '',
      page: 0,
      size: 15,
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    };
    this.loadLogs(0);
  }

  openDetailModal(log: AuditLogItem): void {
    this.selectedLog.set(log);
    this.showDetailModal.set(true);
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.selectedLog.set(null);
  }

  exportCsv(): void {
    this.isExporting.set(true);
    this.auditLogService.exportAuditLogsToCsv(this.filter).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `audit_logs_${new Date().toISOString().slice(0, 10)}.csv`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.isExporting.set(false);
      },
      error: () => {
        alert('Không thể xuất file CSV. Vui lòng thử lại sau.');
        this.isExporting.set(false);
      }
    });
  }

  formatJson(raw: string | undefined): string {
    if (!raw) return 'Không có dữ liệu';
    try {
      const parsed = JSON.parse(raw);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return raw;
    }
  }

  getActionBadgeClass(action: string): string {
    switch (action?.toUpperCase()) {
      case 'CREATE':
        return 'badge-create';
      case 'UPDATE':
        return 'badge-update';
      case 'DELETE':
        return 'badge-delete';
      case 'REFUND':
        return 'badge-refund';
      case 'STATUS_CHANGE':
        return 'badge-status';
      default:
        return 'badge-default';
    }
  }
}
