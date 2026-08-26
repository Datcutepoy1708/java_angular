import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ReturnManageComponent } from './return-manage.component';
import { ReturnService } from '../../../core/services/return.service';
import { InventoryService } from '../../../core/services/inventory.service';

describe('ReturnManageComponent', () => {
  let component: ReturnManageComponent;
  let fixture: ComponentFixture<ReturnManageComponent>;
  let mockReturnService: any;
  let mockInventoryService: any;

  beforeEach(async () => {
    mockReturnService = {
      getAdminReturnRequests: vi.fn().mockReturnValue(of({
        success: true,
        message: 'Success',
        data: {
          content: [
            {
              returnId: 1,
              returnCode: 'RET-20260826-0001',
              orderId: 10,
              orderTrackingNumber: 'ORD-10',
              userId: 100,
              customerName: 'Nguyễn Văn A',
              customerEmail: 'a@store.com',
              status: 'REQUESTED',
              returnReason: 'DEFECTIVE',
              refundAmount: 15000000,
              requestedAt: '2026-08-26T10:00:00',
              createdAt: '2026-08-26T10:00:00',
              updatedAt: '2026-08-26T10:00:00',
              items: [],
              imageUrls: []
            }
          ],
          totalElements: 1,
          totalPages: 1,
          size: 15,
          number: 0,
          first: true,
          last: true,
          empty: false
        },
        timestamp: '2026-08-26T10:00:00'
      })),
      getReturnMetrics: vi.fn().mockReturnValue(of({
        success: true,
        message: 'Success',
        data: {
          totalRequests: 10,
          pendingReviewCount: 2,
          awaitingItemCount: 3,
          refundedCount: 4,
          rejectedCount: 1,
          totalRefundedAmount: 50000000
        },
        timestamp: '2026-08-26T10:00:00'
      })),
      reviewReturnRequest: vi.fn().mockReturnValue(of({ success: true, data: null })),
      receiveReturnedItems: vi.fn().mockReturnValue(of({ success: true, data: null })),
      processRefund: vi.fn().mockReturnValue(of({ success: true, data: null }))
    };

    mockInventoryService = {
      getWarehouses: vi.fn().mockReturnValue(of({
        success: true,
        message: 'Success',
        data: [
          { warehouseId: 1, name: 'Kho Tổng Hà Nội', address: 'Hà Nội' }
        ],
        timestamp: '2026-08-26T10:00:00'
      }))
    };

    await TestBed.configureTestingModule({
      imports: [ReturnManageComponent],
      providers: [
        { provide: ReturnService, useValue: mockReturnService },
        { provide: InventoryService, useValue: mockInventoryService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReturnManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load metrics and requests', () => {
    expect(component).toBeTruthy();
    expect(mockReturnService.getReturnMetrics).toHaveBeenCalled();
    expect(mockReturnService.getAdminReturnRequests).toHaveBeenCalled();
    expect(component.requests().length).toBe(1);
    expect(component.metrics()?.pendingReviewCount).toBe(2);
  });

  it('should change status tab and reload requests', () => {
    component.onTabChange('APPROVED');
    expect(component.filter.status).toBe('APPROVED');
    expect(mockReturnService.getAdminReturnRequests).toHaveBeenCalled();
  });

  it('should open and close detail modal with specific tab', () => {
    const sample = component.requests()[0];
    component.openDetailModal(sample, 'REVIEW');
    expect(component.showDetailModal()).toBe(true);
    expect(component.selectedRequest()).toBe(sample);
    expect(component.activeModalTab()).toBe('REVIEW');

    component.closeDetailModal();
    expect(component.showDetailModal()).toBe(false);
    expect(component.selectedRequest()).toBeNull();
  });

  it('should open and close image preview', () => {
    component.openImagePreview('https://example.com/img.jpg');
    expect(component.previewImageUrl()).toBe('https://example.com/img.jpg');

    component.closeImagePreview();
    expect(component.previewImageUrl()).toBeNull();
  });
});
