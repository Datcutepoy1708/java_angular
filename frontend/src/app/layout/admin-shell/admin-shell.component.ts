import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
  OnInit,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  RouterOutlet,
  RouterLink,
  RouterLinkActive,
  Router,
  NavigationEnd,
} from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';

export interface NavChildItem {
  id: string;
  label: string;
  path: string;
  icon?: string;
  roles?: string[];
  permissions?: string[];
}

export interface NavGroup {
  id: string;
  title: string;
  icon: string; // SVG path string
  path?: string; // If it's a direct single item
  roles?: string[];
  permissions?: string[];
  children?: NavChildItem[];
}

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-shell.component.html',
  styleUrl: './admin-shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminShellComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly themeService = inject(ThemeService);

  readonly currentUser = this.authService.currentUser;
  readonly isDark = this.themeService.isDark;

  // Track expanded dropdown groups
  readonly expandedGroups = signal<Record<string, boolean>>({
    overview: true,
    products: true,
    sales: true,
    customers: false,
    marketing: false,
    support: false,
    staff: false,
  });

  // Grouped Navigation Structure
  readonly navGroups: NavGroup[] = [
    {
      id: 'overview',
      title: 'Tổng Quan & Báo Cáo',
      icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6',
      roles: ['ROLE_ADMIN', 'ROLE_STAFF'],
      children: [
        {
          id: 'dashboard',
          label: 'Bảng điều khiển',
          path: '/admin/dashboard',
          roles: ['ROLE_ADMIN', 'ROLE_STAFF'],
        },
        {
          id: 'statistics',
          label: 'Thống kê doanh thu',
          path: '/admin/statistics',
          roles: ['ROLE_ADMIN'],
          permissions: ['STATISTIC_VIEW', 'STATISTICS_VIEW'],
        },
      ],
    },
    {
      id: 'products',
      title: 'Quản Lý Sản Phẩm',
      icon: 'M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4',
      roles: ['ROLE_ADMIN'],
      children: [
        {
          id: 'product-list',
          label: 'Danh sách sản phẩm',
          path: '/admin/products',
          roles: ['ROLE_ADMIN'],
          permissions: ['PRODUCT_VIEW', 'PRODUCT_CREATE', 'PRODUCT_UPDATE', 'PRODUCT_DELETE'],
        },
        {
          id: 'categories',
          label: 'Danh mục ngành hàng',
          path: '/admin/categories',
          roles: ['ROLE_ADMIN'],
          permissions: ['CATEGORY_VIEW', 'CATEGORY_MANAGE'],
        },
        {
          id: 'category-attributes',
          label: 'Thuộc tính kỹ thuật (EAV)',
          path: '/admin/category-attributes',
          roles: ['ROLE_ADMIN'],
          permissions: ['ATTRIBUTE_VIEW', 'ATTRIBUTE_MANAGE'],
        },
        {
          id: 'inventory',
          label: 'Quản lý kho hàng',
          path: '/admin/inventory',
          roles: ['ROLE_ADMIN'],
          permissions: ['INVENTORY_VIEW', 'INVENTORY_MANAGE', 'INVENTORY_IMPORT', 'INVENTORY_TRANSFER'],
        },
        {
          id: 'brands',
          label: 'Thương hiệu sản xuất',
          path: '/admin/brands',
          roles: ['ROLE_ADMIN'],
          permissions: ['BRAND_VIEW', 'BRAND_MANAGE'],
        },
        {
          id: 'suppliers',
          label: 'Nhà cung cấp',
          path: '/admin/suppliers',
          roles: ['ROLE_ADMIN'],
          permissions: ['SUPPLIER_VIEW', 'SUPPLIER_MANAGE'],
        },
      ],
    },
    {
      id: 'sales',
      title: 'Bán Hàng & Đơn Hàng',
      icon: 'M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z',
      roles: ['ROLE_ADMIN'],
      children: [
        {
          id: 'orders',
          label: 'Quản lý đơn hàng',
          path: '/admin/orders',
          roles: ['ROLE_ADMIN'],
          permissions: ['ORDER_VIEW', 'ORDER_MANAGE', 'ORDER_UPDATE_STATUS'],
        },
        {
          id: 'discounts',
          label: 'Mã giảm giá & Voucher',
          path: '/admin/discounts',
          roles: ['ROLE_ADMIN'],
          permissions: ['DISCOUNT_VIEW', 'DISCOUNT_MANAGE', 'DISCOUNT_CREATE', 'DISCOUNT_UPDATE'],
        },
        {
          id: 'returns',
          label: 'Đổi trả & Hoàn tiền',
          path: '/admin/returns',
          roles: ['ROLE_ADMIN'],
          permissions: ['ORDER_VIEW', 'ORDER_MANAGE'],
        },
      ],
    },
    {
      id: 'customers',
      title: 'Khách Hàng & Đánh Giá',
      icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z',
      roles: ['ROLE_ADMIN'],
      children: [
        {
          id: 'customers-list',
          label: 'Danh sách khách hàng',
          path: '/admin/customers',
          roles: ['ROLE_ADMIN'],
          permissions: ['CUSTOMER_VIEW', 'USER_VIEW', 'USER_MANAGE'],
        },
        {
          id: 'reviews',
          label: 'Đánh giá sản phẩm',
          path: '/admin/reviews',
          roles: ['ROLE_ADMIN'],
          permissions: ['REVIEW_VIEW', 'REVIEW_REPLY', 'REVIEW_DELETE'],
        },
      ],
    },
    {
      id: 'marketing',
      title: 'Nội Dung & Marketing',
      icon: 'M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z',
      roles: ['ROLE_ADMIN'],
      children: [
        {
          id: 'banners',
          label: 'Banner quảng cáo',
          path: '/admin/banners',
          roles: ['ROLE_ADMIN'],
          permissions: ['BANNER_VIEW', 'BANNER_MANAGE', 'BANNER_CREATE', 'BANNER_UPDATE'],
        },
        {
          id: 'news',
          label: 'Tin tức & Bài viết CMS',
          path: '/admin/news',
          roles: ['ROLE_ADMIN'],
          permissions: ['NEWS_VIEW', 'NEWS_MANAGE', 'NEWS_CREATE', 'NEWS_UPDATE'],
        },
      ],
    },
    {
      id: 'support',
      title: 'Chăm Sóc & Live Chat',
      icon: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z',
      roles: ['ROLE_ADMIN', 'ROLE_STAFF'],
      children: [
        {
          id: 'chat',
          label: 'Tin nhắn Live Chat',
          path: '/admin/chat',
          roles: ['ROLE_ADMIN', 'ROLE_STAFF'],
        },
        {
          id: 'bot-rules',
          label: 'Kịch bản Bot Rules',
          path: '/admin/bot-rules',
          roles: ['ROLE_ADMIN'],
        },
      ],
    },
    {
      id: 'staff',
      title: 'Nhân Sự & Phân Quyền',
      icon: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z',
      roles: ['ROLE_ADMIN'],
      children: [
        {
          id: 'staff-list',
          label: 'Quản lý nhân sự',
          path: '/admin/staff',
          roles: ['ROLE_ADMIN'],
          permissions: ['STAFF_VIEW', 'STAFF_MANAGE'],
        },
        {
          id: 'roles',
          label: 'Vai trò & Phân quyền',
          path: '/admin/roles',
          roles: ['ROLE_ADMIN'],
          permissions: ['ROLE_VIEW', 'ROLE_MANAGE'],
        },
        {
          id: 'audit-logs',
          label: 'Nhật ký Audit Logs',
          path: '/admin/audit-logs',
          roles: ['ROLE_ADMIN'],
        },
      ],
    },
    {
      id: 'settings',
      title: 'Cài Đặt Hệ Thống',
      icon: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z',
      path: '/admin/settings',
      roles: ['ROLE_ADMIN'],
      permissions: ['SETTING_MANAGE', 'SETTING_VIEW'],
    },
  ];

  // Dynamically filter accessible groups and their children
  readonly visibleNavGroups = computed(() => {
    return this.navGroups
      .map((group) => {
        if (!this.canAccessItem(group)) {
          return null;
        }
        if (group.children && group.children.length > 0) {
          const filteredChildren = group.children.filter((child) => this.canAccessItem(child));
          if (filteredChildren.length === 0) return null;
          return { ...group, children: filteredChildren };
        }
        return group;
      })
      .filter((g): g is NavGroup => g !== null);
  });

  readonly notificationCount = signal(0);

  ngOnInit(): void {
    // Auto-expand group matching current URL
    this.autoExpandActiveGroup(this.router.url);

    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((e) => {
        this.autoExpandActiveGroup(e.urlAfterRedirects || e.url);
      });
  }

  toggleGroup(groupId: string): void {
    const current = this.expandedGroups();
    this.expandedGroups.set({
      ...current,
      [groupId]: !current[groupId],
    });
  }

  isGroupExpanded(groupId: string): boolean {
    return !!this.expandedGroups()[groupId];
  }

  isGroupActive(group: NavGroup): boolean {
    const currentUrl = this.router.url;
    if (group.path && currentUrl.startsWith(group.path)) {
      return true;
    }
    if (group.children) {
      return group.children.some((child) => currentUrl.startsWith(child.path));
    }
    return false;
  }

  private autoExpandActiveGroup(url: string): void {
    for (const group of this.navGroups) {
      if (group.children?.some((child) => url.startsWith(child.path))) {
        this.expandedGroups.update((prev) => ({
          ...prev,
          [group.id]: true,
        }));
        break;
      }
    }
  }

  private canAccessItem(item: { roles?: string[]; permissions?: string[] }): boolean {
    if (this.authService.isAdmin()) {
      return true;
    }
    if (item.roles && item.roles.includes('ROLE_ADMIN') && item.roles.length === 1 && !item.permissions) {
      return false;
    }
    if (item.permissions && item.permissions.length > 0) {
      return this.authService.hasAnyPermission(item.permissions);
    }
    if (item.roles && item.roles.length > 0) {
      return this.authService.hasAnyRole(item.roles);
    }
    return true;
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  logout(): void {
    this.authService.logout();
  }

  getUserInitials(): string {
    const name = this.currentUser()?.fullName ?? 'A';
    return name
      .split(' ')
      .map((w) => w[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();
  }
}
