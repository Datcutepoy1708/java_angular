import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { InventoryService } from './inventory.service';
import { environment } from '../../../environments/environment';
import {
  InventoryItem,
  InventoryMetrics,
  StockAdjustmentRequest,
  StockImportRequest,
  StockTransferRequest,
  VariantStockSummary,
  Warehouse
} from '../models/inventory.model';

describe('InventoryService', () => {
  let service: InventoryService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1/inventory`;
  const warehouseUrl = `${environment.apiUrl}/api/v1/warehouses`;

  const mockWarehouse: Warehouse = {
    warehouseId: 1,
    name: 'Kho Hà Nội',
    address: '123 Cầu Giấy, Hà Nội',
    phone: '0241234567'
  };

  const mockInventoryItem: InventoryItem = {
    inventoryId: 101,
    variantId: 1,
    variantName: '32GB Đen',
    skuVariant: 'RAM-DDR5-32-BLK',
    price: 3200000,
    productId: 10,
    productName: 'Corsair Vengeance RGB DDR5',
    productSlug: 'corsair-vengeance-rgb-ddr5',
    warehouseId: 1,
    warehouseName: 'Kho Hà Nội',
    quantity: 50,
    reservedQty: 5,
    availableQty: 45,
    stockStatus: 'IN_STOCK',
    updatedAt: '2026-08-25T00:00:00Z'
  };

  const mockMetrics: InventoryMetrics = {
    totalTrackedItems: 120,
    lowStockItemsCount: 8,
    outOfStockItemsCount: 2,
    totalPhysicalQuantity: 3450
  };

  const mockVariantSummary: VariantStockSummary = {
    variantId: 1,
    variantName: '32GB Đen',
    skuVariant: 'RAM-DDR5-32-BLK',
    productId: 10,
    productName: 'Corsair Vengeance RGB DDR5',
    totalQuantity: 100,
    totalReservedQty: 10,
    totalAvailableQty: 90,
    inStock: true,
    stockStatus: 'IN_STOCK',
    warehouseBreakdown: [
      {
        warehouseId: 1,
        warehouseName: 'Kho Hà Nội',
        quantity: 50,
        reservedQty: 5,
        availableQty: 45
      }
    ]
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        InventoryService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(InventoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch all warehouses', () => {
    service.getWarehouses().subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.length).toBe(1);
      expect(res.data[0].name).toBe('Kho Hà Nội');
    });

    const req = httpMock.expectOne(warehouseUrl);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: [mockWarehouse] });
  });

  it('should fetch inventory metrics', () => {
    service.getMetrics().subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.totalTrackedItems).toBe(120);
      expect(res.data.totalPhysicalQuantity).toBe(3450);
    });

    const req = httpMock.expectOne(`${baseUrl}/metrics`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockMetrics });
  });

  it('should fetch paginated inventory items with filters', () => {
    service.getInventory({ warehouseId: 1, stockStatus: 'IN_STOCK', page: 0, size: 10 }).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.content.length).toBe(1);
      expect(res.data.content[0].variantName).toBe('32GB Đen');
    });

    const req = httpMock.expectOne(`${baseUrl}?warehouseId=1&stockStatus=IN_STOCK&page=0&size=10`);
    expect(req.request.method).toBe('GET');
    req.flush({
      success: true,
      message: 'OK',
      data: {
        content: [mockInventoryItem],
        totalElements: 1,
        totalPages: 1,
        pageSize: 10,
        pageNumber: 0,
        last: true
      }
    });
  });

  it('should fetch variant stock summary', () => {
    service.getVariantStockSummary(1).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.totalAvailableQty).toBe(90);
      expect(res.data.inStock).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/variants/1/summary`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockVariantSummary });
  });

  it('should execute stock adjustment', () => {
    const adjustReq: StockAdjustmentRequest = {
      variantId: 1,
      warehouseId: 1,
      quantityChange: -5,
      reason: 'Kiểm kê định kỳ: Hỏng hóc'
    };

    service.adjustStock(adjustReq).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.quantity).toBe(45);
    });

    const req = httpMock.expectOne(`${baseUrl}/adjust`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(adjustReq);
    req.flush({ success: true, message: 'Adjusted', data: { ...mockInventoryItem, quantity: 45, availableQty: 40 } });
  });

  it('should execute stock transfer between warehouses', () => {
    const transferReq: StockTransferRequest = {
      fromWarehouseId: 1,
      toWarehouseId: 2,
      variantId: 1,
      quantity: 10,
      note: 'Điều phối chi nhánh'
    };

    service.transferStock(transferReq).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.length).toBe(2);
    });

    const req = httpMock.expectOne(`${baseUrl}/transfer`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(transferReq);
    req.flush({ success: true, message: 'Transferred', data: [mockInventoryItem, { ...mockInventoryItem, warehouseId: 2 }] });
  });

  it('should execute stock import', () => {
    const importReq: StockImportRequest = {
      warehouseId: 1,
      items: [{ variantId: 1, quantity: 20 }]
    };

    service.importStock(importReq).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.length).toBe(1);
    });

    const req = httpMock.expectOne(`${baseUrl}/import`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(importReq);
    req.flush({ success: true, message: 'Imported', data: [{ ...mockInventoryItem, quantity: 70 }] });
  });
});
