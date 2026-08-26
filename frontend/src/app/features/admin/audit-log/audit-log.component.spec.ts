import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { AuditLogComponent } from './audit-log.component';
import { AuditLogService } from '../../../core/services/audit-log.service';

describe('AuditLogComponent', () => {
  let component: AuditLogComponent;
  let fixture: ComponentFixture<AuditLogComponent>;
  let mockAuditLogService: any;

  beforeEach(async () => {
    mockAuditLogService = {
      getAuditLogs: vi.fn().mockReturnValue(of({
        success: true,
        message: 'Success',
        data: {
          content: [
            {
              logId: 1,
              userId: 1,
              userEmail: 'admin@store.com',
              actionType: 'UPDATE',
              module: 'ROLE',
              description: 'Cập nhật phân quyền',
              status: 'SUCCESS',
              createdAt: '2026-08-26T10:00:00'
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
      exportAuditLogsToCsv: vi.fn().mockReturnValue(of(new Blob()))
    };

    await TestBed.configureTestingModule({
      imports: [AuditLogComponent],
      providers: [
        { provide: AuditLogService, useValue: mockAuditLogService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditLogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load initial logs', () => {
    expect(component).toBeTruthy();
    expect(mockAuditLogService.getAuditLogs).toHaveBeenCalled();
    expect(component.logs().length).toBe(1);
    expect(component.logs()[0].module).toBe('ROLE');
  });

  it('should open and close detail modal', () => {
    const sample = component.logs()[0];
    component.openDetailModal(sample);
    expect(component.showDetailModal()).toBe(true);
    expect(component.selectedLog()).toBe(sample);

    component.closeDetailModal();
    expect(component.showDetailModal()).toBe(false);
    expect(component.selectedLog()).toBeNull();
  });

  it('should filter logs when onFilterChange is called', () => {
    component.filter.module = 'STAFF';
    component.onFilterChange();
    expect(mockAuditLogService.getAuditLogs).toHaveBeenCalled();
    expect(component.filter.page).toBe(0);
  });

  it('should reset filter to defaults', () => {
    component.filter.module = 'STAFF';
    component.filter.keyword = 'abc';
    component.resetFilter();
    expect(component.filter.module).toBe('');
    expect(component.filter.keyword).toBe('');
  });
});
