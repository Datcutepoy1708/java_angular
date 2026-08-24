import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InventoryManageComponent } from './inventory-manage.component';
import { InventoryService } from '../../../core/services/inventory.service';
import { ProductService } from '../../../core/services/product.service';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { InventoryItem, InventoryMetrics, Warehouse } from '../../../core/models/inventory.model';

describe('InventoryManageComponent', () => {
  let component: InventoryManageComponent;
  let fixture: ComponentFixture<InventoryManageComponent>;
  let inventoryService: InventoryService;
  let productService: ProductService;

  const mockWarehouses: Warehouse[] = [
    { warehouseId: 1, name: 'Kho Hà Nội' },
    { warehouseId: 2, name: 'Kho TP.HCM' }
  ];

  const mockMetrics: InventoryMetrics = {
    totalTrackedItems: 50,
    lowStockItemsCount: 5,
    outOfStockItemsCount: 1,
    totalPhysicalQuantity: 1200
  };

  const mockInventoryItem: InventoryItem = {
    inventoryId: 1,
    variantId: 10,
    variantName: '32GB Đen',
    skuVariant: 'RAM-DDR5-32-BLK',
    productId: 1,
    productName: 'Corsair Vengeance RGB DDR5',
    productSlug: 'corsair-vengeance-rgb-ddr5',
    warehouseId: 1,
    warehouseName: 'Kho Hà Nội',
    quantity: 25,
    reservedQty: 2,
    availableQty: 23,
    stockStatus: 'IN_STOCK',
    updatedAt: '2026-08-25T00:00:00Z'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InventoryManageComponent],
      providers: [
        {
          provide: InventoryService,
          useValue: {
            getWarehouses: vi.fn().mockReturnValue(of({ success: true, data: mockWarehouses })),
            getMetrics: vi.fn().mockReturnValue(of({ success: true, data: mockMetrics })),
            getInventory: vi.fn().mockReturnValue(of({
              success: true,
              data: {
                content: [mockInventoryItem],
                totalElements: 1,
                totalPages: 1,
                pageSize: 15,
                pageNumber: 0,
                last: true
              }
            })),
            getLogs: vi.fn().mockReturnValue(of({
              success: true,
              data: {
                content: [],
                totalElements: 0,
                totalPages: 0,
                pageSize: 15,
                pageNumber: 0,
                last: true
              }
            })),
            adjustStock: vi.fn().mockReturnValue(of({ success: true, data: mockInventoryItem })),
            transferStock: vi.fn().mockReturnValue(of({ success: true, data: [mockInventoryItem] })),
            importStock: vi.fn().mockReturnValue(of({ success: true, data: [mockInventoryItem] }))
          }
        },
        {
          provide: ProductService,
          useValue: {
            getProducts: vi.fn().mockReturnValue(of({
              success: true,
              data: { content: [], totalElements: 0, totalPages: 0, size: 100, number: 0 }
            }))
          }
        }
      ]
    }).compileComponents();

    inventoryService = TestBed.inject(InventoryService);
    productService = TestBed.inject(ProductService);
    fixture = TestBed.createComponent(InventoryManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load initial warehouses, metrics, and inventory', () => {
    expect(component).toBeTruthy();
    expect(component.warehouses().length).toBe(2);
    expect(component.metrics().totalTrackedItems).toBe(50);
    expect(component.inventoryItems().length).toBe(1);
    expect(component.inventoryItems()[0].productName).toBe('Corsair Vengeance RGB DDR5');
  });

  it('should switch tabs between stock overview and audit logs', () => {
    component.onTabChange('logs');
    expect(component.activeTab()).toBe('logs');
    expect(inventoryService.getLogs).toHaveBeenCalled();
  });

  it('should open and populate adjust modal', () => {
    component.openAdjustModal(mockInventoryItem);
    expect(component.isAdjustModalOpen()).toBe(true);
    expect(component.activeInventoryItem()?.inventoryId).toBe(1);
  });

  it('should open transfer modal with preselected source warehouse', () => {
    component.openTransferModal(mockInventoryItem);
    expect(component.isTransferModalOpen()).toBe(true);
    expect(component.transferForm.get('fromWarehouseId')?.value).toBe(1);
    expect(component.transferForm.get('toWarehouseId')?.value).toBe(2);
  });

  it('should open import modal with initial item row', () => {
    component.openImportModal();
    expect(component.isImportModalOpen()).toBe(true);
    expect(component.importItemsArray.length).toBe(1);
  });
});
