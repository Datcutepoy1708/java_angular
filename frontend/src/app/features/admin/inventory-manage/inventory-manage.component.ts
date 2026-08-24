import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InventoryService } from '../../../core/services/inventory.service';
import { ProductService } from '../../../core/services/product.service';
import {
  InventoryItem,
  InventoryLog,
  InventoryMetrics,
  StockAdjustmentRequest,
  StockImportRequest,
  StockTransferRequest,
  Warehouse
} from '../../../core/models/inventory.model';
import { ProductResponse } from '../../../core/models/product.model';
import { PaginationComponent } from '../../../shared';

@Component({
  selector: 'app-inventory-manage',
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent],
  templateUrl: './inventory-manage.component.html',
  styleUrl: './inventory-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InventoryManageComponent implements OnInit {
  private readonly inventoryService = inject(InventoryService);
  private readonly productService = inject(ProductService);
  private readonly fb = inject(FormBuilder);

  // Active Tab: 'stock' | 'logs'
  readonly activeTab = signal<'stock' | 'logs'>('stock');

  // Warehouses & Metrics
  readonly warehouses = signal<Warehouse[]>([]);
  readonly metrics = signal<InventoryMetrics>({
    totalTrackedItems: 0,
    lowStockItemsCount: 0,
    outOfStockItemsCount: 0,
    totalPhysicalQuantity: 0
  });

  // Stock Items State
  readonly inventoryItems = signal<InventoryItem[]>([]);
  readonly loading = signal(false);
  readonly selectedWarehouseId = signal<number | null>(null);
  readonly selectedStockStatus = signal<'ALL' | 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'>('ALL');
  readonly searchKeyword = signal<string>('');
  readonly currentPage = signal(0);
  readonly pageSize = signal(15);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  // Audit Logs State
  readonly logs = signal<InventoryLog[]>([]);
  readonly logsLoading = signal(false);
  readonly logsPage = signal(0);
  readonly logsPageSize = signal(15);
  readonly logsTotalElements = signal(0);
  readonly logsTotalPages = signal(0);
  readonly logsWarehouseFilter = signal<number | null>(null);
  readonly logsSearchKeyword = signal<string>('');

  // Products & Variants for Dropdowns
  readonly productsList = signal<ProductResponse[]>([]);

  // Modals State
  readonly isAdjustModalOpen = signal(false);
  readonly isImportModalOpen = signal(false);
  readonly isTransferModalOpen = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Selected item for Adjust / Transfer
  readonly activeInventoryItem = signal<InventoryItem | null>(null);

  // Forms
  readonly adjustForm: FormGroup = this.fb.group({
    quantityChange: [0, [Validators.required, Validators.min(-99999), Validators.max(99999)]],
    reasonCategory: ['Kiểm kê định kỳ', [Validators.required]],
    customReason: ['', [Validators.maxLength(255)]]
  });

  readonly transferForm: FormGroup = this.fb.group({
    fromWarehouseId: [null as number | null, [Validators.required]],
    toWarehouseId: [null as number | null, [Validators.required]],
    quantity: [1, [Validators.required, Validators.min(1)]],
    note: ['', [Validators.maxLength(255)]]
  });

  readonly importForm: FormGroup = this.fb.group({
    warehouseId: [null as number | null, [Validators.required]],
    note: ['', [Validators.maxLength(255)]],
    items: this.fb.array([])
  });

  get importItemsArray(): FormArray {
    return this.importForm.get('items') as FormArray;
  }

  ngOnInit(): void {
    this.loadWarehouses();
    this.loadMetrics();
    this.loadInventory();
    this.loadProductsList();
  }

  loadWarehouses(): void {
    this.inventoryService.getWarehouses().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.warehouses.set(res.data);
        }
      }
    });
  }

  loadMetrics(): void {
    this.inventoryService.getMetrics().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.metrics.set(res.data);
        }
      }
    });
  }

  loadProductsList(): void {
    this.productService.getProducts({ page: 0, size: 100 }).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.productsList.set(res.data.content);
        }
      }
    });
  }

  loadInventory(): void {
    this.loading.set(true);
    this.inventoryService.getInventory({
      warehouseId: this.selectedWarehouseId() || undefined,
      keyword: this.searchKeyword() || undefined,
      stockStatus: this.selectedStockStatus(),
      page: this.currentPage(),
      size: this.pageSize(),
      sortBy: 'updatedAt',
      sortDir: 'desc'
    }).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.inventoryItems.set(res.data.content);
          this.totalElements.set(res.data.totalElements);
          this.totalPages.set(res.data.totalPages);
        }
      },
      error: () => {
        this.loading.set(false);
        this.inventoryItems.set([]);
      }
    });
  }

  loadLogs(): void {
    this.logsLoading.set(true);
    this.inventoryService.getLogs({
      warehouseId: this.logsWarehouseFilter() || undefined,
      keyword: this.logsSearchKeyword() || undefined,
      page: this.logsPage(),
      size: this.logsPageSize()
    }).subscribe({
      next: (res) => {
        this.logsLoading.set(false);
        if (res.success && res.data) {
          this.logs.set(res.data.content);
          this.logsTotalElements.set(res.data.totalElements);
          this.logsTotalPages.set(res.data.totalPages);
        }
      },
      error: () => {
        this.logsLoading.set(false);
        this.logs.set([]);
      }
    });
  }

  onTabChange(tab: 'stock' | 'logs'): void {
    this.activeTab.set(tab);
    if (tab === 'logs' && this.logs().length === 0) {
      this.loadLogs();
    }
  }

  onWarehouseFilter(warehouseId: number | null): void {
    this.selectedWarehouseId.set(warehouseId);
    this.currentPage.set(0);
    this.loadInventory();
  }

  onStatusFilter(status: 'ALL' | 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'): void {
    this.selectedStockStatus.set(status);
    this.currentPage.set(0);
    this.loadInventory();
  }

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchKeyword.set(value);
    this.currentPage.set(0);
    this.loadInventory();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadInventory();
  }

  onLogsPageChange(page: number): void {
    this.logsPage.set(page);
    this.loadLogs();
  }

  onLogsWarehouseFilter(warehouseId: number | null): void {
    this.logsWarehouseFilter.set(warehouseId);
    this.logsPage.set(0);
    this.loadLogs();
  }

  onLogsSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.logsSearchKeyword.set(value);
    this.logsPage.set(0);
    this.loadLogs();
  }

  // ── ADJUST MODAL ────────────────────────────────────────────────

  openAdjustModal(item: InventoryItem): void {
    this.activeInventoryItem.set(item);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.adjustForm.reset({
      quantityChange: 0,
      reasonCategory: 'Kiểm kê định kỳ',
      customReason: ''
    });
    this.isAdjustModalOpen.set(true);
  }

  closeAdjustModal(): void {
    this.isAdjustModalOpen.set(false);
    this.activeInventoryItem.set(null);
  }

  submitAdjust(): void {
    const item = this.activeInventoryItem();
    if (!item || this.adjustForm.invalid) return;

    const val = this.adjustForm.value;
    if (val.quantityChange === 0) {
      this.errorMessage.set('Vui lòng nhập số lượng thay đổi khác 0');
      return;
    }

    const reason = val.customReason?.trim()
      ? `${val.reasonCategory}: ${val.customReason.trim()}`
      : val.reasonCategory;

    const request: StockAdjustmentRequest = {
      variantId: item.variantId,
      warehouseId: item.warehouseId,
      quantityChange: val.quantityChange,
      reason
    };

    this.saving.set(true);
    this.errorMessage.set(null);

    this.inventoryService.adjustStock(request).subscribe({
      next: (res) => {
        this.saving.set(false);
        if (res.success) {
          this.closeAdjustModal();
          this.loadInventory();
          this.loadMetrics();
          if (this.activeTab() === 'logs') this.loadLogs();
        }
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(err?.error?.message || 'Có lỗi xảy ra khi điều chỉnh tồn kho');
      }
    });
  }

  // ── TRANSFER MODAL ──────────────────────────────────────────────

  openTransferModal(item: InventoryItem): void {
    this.activeInventoryItem.set(item);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const availableWarehouses = this.warehouses().filter(w => w.warehouseId !== item.warehouseId);
    const defaultTo = availableWarehouses.length > 0 ? availableWarehouses[0].warehouseId : null;

    this.transferForm.reset({
      fromWarehouseId: item.warehouseId,
      toWarehouseId: defaultTo,
      quantity: 1,
      note: ''
    });

    this.isTransferModalOpen.set(true);
  }

  closeTransferModal(): void {
    this.isTransferModalOpen.set(false);
    this.activeInventoryItem.set(null);
  }

  submitTransfer(): void {
    const item = this.activeInventoryItem();
    if (!item || this.transferForm.invalid) return;

    const val = this.transferForm.value;
    if (val.fromWarehouseId === val.toWarehouseId) {
      this.errorMessage.set('Kho nguồn và kho đích không được trùng nhau');
      return;
    }

    if (val.quantity > item.availableQty) {
      this.errorMessage.set(`Số lượng chuyển không thể lớn hơn tồn khả dụng (${item.availableQty})`);
      return;
    }

    const request: StockTransferRequest = {
      fromWarehouseId: val.fromWarehouseId,
      toWarehouseId: val.toWarehouseId,
      variantId: item.variantId,
      quantity: val.quantity,
      note: val.note
    };

    this.saving.set(true);
    this.errorMessage.set(null);

    this.inventoryService.transferStock(request).subscribe({
      next: (res) => {
        this.saving.set(false);
        if (res.success) {
          this.closeTransferModal();
          this.loadInventory();
          this.loadMetrics();
          if (this.activeTab() === 'logs') this.loadLogs();
        }
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(err?.error?.message || 'Có lỗi xảy ra khi chuyển kho');
      }
    });
  }

  // ── IMPORT MODAL ────────────────────────────────────────────────

  openImportModal(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
    const defaultWarehouse = this.warehouses().length > 0 ? this.warehouses()[0].warehouseId : null;

    this.importForm.reset({
      warehouseId: defaultWarehouse,
      note: ''
    });

    this.importItemsArray.clear();
    this.addImportItem();
    this.isImportModalOpen.set(true);
  }

  closeImportModal(): void {
    this.isImportModalOpen.set(false);
  }

  addImportItem(): void {
    const itemGroup = this.fb.group({
      variantId: [null as number | null, [Validators.required]],
      quantity: [10, [Validators.required, Validators.min(1)]],
      costPrice: [null]
    });
    this.importItemsArray.push(itemGroup);
  }

  removeImportItem(index: number): void {
    if (this.importItemsArray.length > 1) {
      this.importItemsArray.removeAt(index);
    }
  }

  submitImport(): void {
    if (this.importForm.invalid || this.importItemsArray.length === 0) {
      this.errorMessage.set('Vui lòng điền đầy đủ thông tin các mặt hàng nhập kho');
      return;
    }

    const val = this.importForm.value;
    const request: StockImportRequest = {
      warehouseId: val.warehouseId,
      note: val.note,
      items: val.items.map((i: { variantId: number; quantity: number; costPrice?: number }) => ({
        variantId: Number(i.variantId),
        quantity: Number(i.quantity),
        costPrice: i.costPrice ? Number(i.costPrice) : undefined
      }))
    };

    this.saving.set(true);
    this.errorMessage.set(null);

    this.inventoryService.importStock(request).subscribe({
      next: (res) => {
        this.saving.set(false);
        if (res.success) {
          this.closeImportModal();
          this.loadInventory();
          this.loadMetrics();
          if (this.activeTab() === 'logs') this.loadLogs();
        }
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(err?.error?.message || 'Có lỗi xảy ra khi nhập kho');
      }
    });
  }

  getChangeTypeLabel(type: string): string {
    switch (type?.toLowerCase()) {
      case 'import': return 'Nhập kho';
      case 'sale': return 'Xuất bán';
      case 'return': return 'Hoàn trả';
      case 'adjust': return 'Điều chỉnh';
      case 'transfer': return 'Chuyển kho';
      default: return type;
    }
  }

  getChangeTypeClass(type: string): string {
    switch (type?.toLowerCase()) {
      case 'import': return 'badge-import';
      case 'sale': return 'badge-sale';
      case 'return': return 'badge-return';
      case 'adjust': return 'badge-adjust';
      case 'transfer': return 'badge-transfer';
      default: return 'badge-default';
    }
  }
}
