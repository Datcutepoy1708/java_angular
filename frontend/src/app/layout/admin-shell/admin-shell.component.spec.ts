import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminShellComponent } from './admin-shell.component';
import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { UserSummary } from '../../core/models/auth.model';

describe('AdminShellComponent', () => {
  let component: AdminShellComponent;
  let fixture: ComponentFixture<AdminShellComponent>;
  let authServiceMock: any;
  let themeServiceMock: any;

  const mockAdminUser: UserSummary = {
    userId: 1,
    fullName: 'Admin Tester',
    email: 'admin@store.com',
    status: 'active',
    roles: ['ROLE_ADMIN'],
    permissions: []
  };

  beforeEach(async () => {
    authServiceMock = {
      currentUser: signal<UserSummary | null>(mockAdminUser),
      isAdmin: signal(true),
      hasAnyRole: vi.fn((roles: string[]) => roles.includes('ROLE_ADMIN')),
      hasAnyPermission: vi.fn(() => true),
      logout: vi.fn()
    };

    const isDarkSignal = signal(false);
    themeServiceMock = {
      currentTheme: signal<'light' | 'dark'>('light'),
      isDark: isDarkSignal,
      toggleTheme: vi.fn(() => {
        isDarkSignal.update(v => !v);
      })
    };

    await TestBed.configureTestingModule({
      imports: [AdminShellComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
        { provide: ThemeService, useValue: themeServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and render navigation items and user initials for Admin', () => {
    expect(component).toBeTruthy();
    expect(component.visibleNavItems().length).toBe(component.navItems.length);
    expect(component.getUserInitials()).toBe('AT');
  });

  it('should filter out unauthorized sidebar items for limited Staff user', () => {
    authServiceMock.isAdmin.set(false);
    authServiceMock.hasAnyRole.mockImplementation((roles: string[]) => roles.includes('ROLE_STAFF'));
    authServiceMock.hasAnyPermission.mockImplementation((perms: string[]) => perms.includes('PRODUCT_VIEW'));

    fixture.detectChanges();

    const visiblePaths = component.visibleNavItems().map(i => i.path);
    // Dashboard (ROLE_STAFF allowed) and Products (PRODUCT_VIEW permission allowed) should be visible
    expect(visiblePaths).toContain('/admin/dashboard');
    expect(visiblePaths).toContain('/admin/products');

    // Admin-only or non-granted permission items should be HIDDEN
    expect(visiblePaths).not.toContain('/admin/staff');
    expect(visiblePaths).not.toContain('/admin/roles');
    expect(visiblePaths).not.toContain('/admin/settings');
    expect(visiblePaths).not.toContain('/admin/audit-logs');
  });

  it('should toggle theme when toggleTheme is called', () => {
    expect(component.isDark()).toBe(false);
    component.toggleTheme();
    expect(themeServiceMock.toggleTheme).toHaveBeenCalled();
    expect(component.isDark()).toBe(true);
  });

  it('should call authService.logout on logout', () => {
    component.logout();
    expect(authServiceMock.logout).toHaveBeenCalled();
  });
});
