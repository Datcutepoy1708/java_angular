import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { SettingManageComponent } from './setting-manage.component';
import { SettingService } from '../../../core/services/setting.service';
import { of } from 'rxjs';
import { SettingItem } from '../../../core/models/setting.model';

describe('SettingManageComponent', () => {
  let component: SettingManageComponent;
  let fixture: ComponentFixture<SettingManageComponent>;

  const mockSettings: SettingItem[] = [
    {
      settingId: 1,
      settingKey: 'STORE_NAME',
      settingValue: 'Complexus Tech Test',
      settingGroup: 'GENERAL',
      isPublic: true,
    },
    {
      settingId: 2,
      settingKey: 'FOOTER_BRAND_TITLE',
      settingValue: 'COMPLEXUS FOOTER',
      settingGroup: 'FOOTER',
      isPublic: true,
    },
    {
      settingId: 3,
      settingKey: 'FREE_SHIPPING_THRESHOLD',
      settingValue: '8000000',
      settingGroup: 'ORDER_SHIPPING',
      isPublic: true,
    },
    {
      settingId: 4,
      settingKey: 'MAINTENANCE_MODE',
      settingValue: 'false',
      settingGroup: 'SYSTEM_NOTIFICATION',
      isPublic: true,
    },
  ];

  const mockSettingService = {
    getAllSettings: vi.fn().mockReturnValue(
      of({ success: true, message: 'OK', data: mockSettings })
    ),
    updateSettings: vi.fn().mockReturnValue(
      of({ success: true, message: 'Updated', data: undefined })
    ),
    resetDefaults: vi.fn().mockReturnValue(
      of({ success: true, message: 'Reset', data: undefined })
    ),
    loadPublicSettings: vi.fn().mockReturnValue(of({})),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    mockSettingService.getAllSettings.mockReturnValue(
      of({ success: true, message: 'OK', data: mockSettings })
    );

    await TestBed.configureTestingModule({
      imports: [SettingManageComponent],
      providers: [
        { provide: SettingService, useValue: mockSettingService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SettingManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load settings into form', () => {
    expect(component).toBeDefined();
    expect(mockSettingService.getAllSettings).toHaveBeenCalled();
    expect(component.settingForm.get('STORE_NAME')?.value).toBe('Complexus Tech Test');
    expect(component.settingForm.get('FOOTER_BRAND_TITLE')?.value).toBe('COMPLEXUS FOOTER');
    expect(component.settingForm.get('FREE_SHIPPING_THRESHOLD')?.value).toBe(8000000);
  });

  it('should switch active tabs correctly', () => {
    expect(component.activeTab()).toBe('general');

    component.setTab('footer');
    expect(component.activeTab()).toBe('footer');

    component.setTab('orderShipping');
    expect(component.activeTab()).toBe('orderShipping');

    component.setTab('systemNotification');
    expect(component.activeTab()).toBe('systemNotification');
  });

  it('should save settings when form is valid', () => {
    mockSettingService.updateSettings.mockReturnValue(
      of({ success: true, message: 'Updated', data: undefined })
    );

    component.settingForm.patchValue({
      STORE_NAME: 'Updated Store Name',
      FOOTER_BRAND_TITLE: 'Updated Brand Title',
    });

    component.saveSettings();

    expect(mockSettingService.updateSettings).toHaveBeenCalled();
    expect(component.toast()?.type).toBe('success');
  });

  it('should not save settings and show error when form is invalid', () => {
    component.settingForm.patchValue({
      STORE_NAME: '', // required field empty
    });

    component.saveSettings();

    expect(mockSettingService.updateSettings).not.toHaveBeenCalled();
    expect(component.toast()?.type).toBe('error');
  });
});
