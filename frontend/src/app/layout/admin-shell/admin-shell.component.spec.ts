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

  it('should create and render the admin shell with grouped navigation', () => {
    expect(component).toBeTruthy();
    // visibleNavGroups() should return all groups for an admin
    const groups = component.visibleNavGroups();
    expect(groups.length).toBeGreaterThan(0);
    expect(component.getUserInitials()).toBe('AT');
  });

  it('should expose all navGroups as source of truth', () => {
    // navGroups is the source data (public readonly array)
    expect(component.navGroups.length).toBeGreaterThan(0);
    // visibleNavGroups is the filtered computed signal
    const visibleGroups = component.visibleNavGroups();
    // An admin sees all groups
    expect(visibleGroups.length).toBe(component.navGroups.length);
  });

  it('should filter out unauthorized groups/children for limited Staff user', () => {
    authServiceMock.isAdmin.set(false);
    authServiceMock.hasAnyRole.mockImplementation((roles: string[]) => roles.includes('ROLE_STAFF'));
    // Staff only has PRODUCT_VIEW permission
    authServiceMock.hasAnyPermission.mockImplementation((perms: string[]) => perms.includes('PRODUCT_VIEW'));

    fixture.detectChanges();

    const visibleGroups = component.visibleNavGroups();

    // Flatten all visible child paths
    const visiblePaths = visibleGroups.flatMap(g =>
      g.children ? g.children.map((c: { path: string }) => c.path) : (g.path ? [g.path] : [])
    );

    // Dashboard (ROLE_STAFF allowed) should be visible
    expect(visiblePaths).toContain('/admin/dashboard');
    // Products (PRODUCT_VIEW permission allowed) should be visible
    expect(visiblePaths).toContain('/admin/products');

    // Admin-only items should be HIDDEN
    expect(visiblePaths).not.toContain('/admin/staff');
    expect(visiblePaths).not.toContain('/admin/roles');
    expect(visiblePaths).not.toContain('/admin/settings');
    expect(visiblePaths).not.toContain('/admin/audit-logs');
  });

  it('should toggle accordion group open and closed', () => {
    // products group starts open (default in expandedGroups signal)
    const wasExpanded = component.isGroupExpanded('products');
    component.toggleGroup('products');
    expect(component.isGroupExpanded('products')).toBe(!wasExpanded);
    // Toggle back
    component.toggleGroup('products');
    expect(component.isGroupExpanded('products')).toBe(wasExpanded);
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
