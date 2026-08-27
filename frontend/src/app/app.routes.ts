import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  // 1. Auth routes (Split-screen for customers: /auth/login, /auth/register)
  {
    path: 'auth',
    loadComponent: () =>
      import('./features/auth/auth-layout/auth-layout.component').then((m) => m.AuthLayoutComponent),
    children: [
      { path: '', redirectTo: 'login', pathMatch: 'full' },
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/login/login.component').then((m) => m.LoginComponent)
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./features/auth/register/register.component').then((m) => m.RegisterComponent)
      },
      {
        path: 'forgot-password',
        loadComponent: () =>
          import('./features/auth/forgot-password/forgot-password.component').then((m) => m.ForgotPasswordComponent)
      }
    ]
  },

  // Shorthand aliases
  { path: 'login', redirectTo: 'auth/login', pathMatch: 'full' },
  { path: 'register', redirectTo: 'auth/register', pathMatch: 'full' },
  { path: 'forgot-password', redirectTo: 'auth/forgot-password', pathMatch: 'full' },

  // 2. Admin area — parent route handles both login AND protected dashboard
  {
    path: 'admin',
    children: [
      // 2a. Admin Login: /admin/login — NO guard, public
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/admin-login/admin-login.component').then((m) => m.AdminLoginComponent)
      },

      // 2b. Admin protected area: /admin, /admin/dashboard etc — guarded
      {
        path: '',
        loadComponent: () =>
          import('./layout/admin-shell/admin-shell.component').then((m) => m.AdminShellComponent),
        canActivate: [authGuard, roleGuard],
        data: { roles: ['ROLE_ADMIN', 'ROLE_STAFF'] },
        children: [
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
          {
            path: 'dashboard',
            loadComponent: () =>
              import('./features/admin/dashboard/dashboard.component').then((m) => m.DashboardComponent)
          },
          {
            path: 'brands',
            canActivate: [roleGuard],
            data: { permissions: ['BRAND_VIEW', 'BRAND_MANAGE'] },
            loadComponent: () =>
              import('./features/admin/brand-manage/brand-manage.component').then((m) => m.BrandManageComponent)
          },
          {
            path: 'categories',
            canActivate: [roleGuard],
            data: { permissions: ['CATEGORY_VIEW', 'CATEGORY_MANAGE'] },
            loadComponent: () =>
              import('./features/admin/category-manage/category-manage.component').then((m) => m.CategoryManageComponent)
          },
          {
            path: 'category-attributes',
            canActivate: [roleGuard],
            data: { permissions: ['ATTRIBUTE_VIEW', 'ATTRIBUTE_MANAGE'] },
            loadComponent: () =>
              import('./features/admin/category-attributes-manage/category-attributes-manage.component').then((m) => m.CategoryAttributesManageComponent)
          },
          {
            path: 'products',
            canActivate: [roleGuard],
            data: { permissions: ['PRODUCT_VIEW', 'PRODUCT_CREATE', 'PRODUCT_UPDATE', 'PRODUCT_DELETE'] },
            loadComponent: () =>
              import('./features/admin/product-manage/product-manage.component').then((m) => m.ProductManageComponent)
          },
          {
            path: 'products/new',
            canActivate: [roleGuard],
            data: { permissions: ['PRODUCT_CREATE'] },
            loadComponent: () =>
              import('./features/admin/product-manage/product-form/product-form.component').then((m) => m.ProductFormComponent)
          },
          {
            path: 'products/:id/edit',
            canActivate: [roleGuard],
            data: { permissions: ['PRODUCT_UPDATE'] },
            loadComponent: () =>
              import('./features/admin/product-manage/product-form/product-form.component').then((m) => m.ProductFormComponent)
          },
          {
            path: 'inventory',
            canActivate: [roleGuard],
            data: { permissions: ['INVENTORY_VIEW', 'INVENTORY_MANAGE', 'INVENTORY_IMPORT', 'INVENTORY_TRANSFER'] },
            loadComponent: () =>
              import('./features/admin/inventory-manage/inventory-manage.component').then((m) => m.InventoryManageComponent)
          },
          {
            path: 'orders',
            canActivate: [roleGuard],
            data: { permissions: ['ORDER_VIEW', 'ORDER_MANAGE', 'ORDER_UPDATE_STATUS'] },
            loadComponent: () =>
              import('./features/admin/order-manage/order-manage.component').then((m) => m.OrderManageComponent)
          },
          {
            path: 'discounts',
            canActivate: [roleGuard],
            data: { permissions: ['DISCOUNT_VIEW', 'DISCOUNT_MANAGE', 'DISCOUNT_CREATE', 'DISCOUNT_UPDATE'] },
            loadComponent: () =>
              import('./features/admin/discount-manage/discount-manage.component').then((m) => m.DiscountManageComponent)
          },
          {
            path: 'banners',
            canActivate: [roleGuard],
            data: { permissions: ['BANNER_VIEW', 'BANNER_MANAGE', 'BANNER_CREATE', 'BANNER_UPDATE'] },
            loadComponent: () =>
              import('./features/admin/banner-manage/banner-manage.component').then((m) => m.BannerManageComponent)
          },
          {
            path: 'news',
            canActivate: [roleGuard],
            data: { permissions: ['NEWS_VIEW', 'NEWS_MANAGE', 'NEWS_CREATE', 'NEWS_UPDATE'] },
            loadComponent: () =>
              import('./features/admin/news-manage/news-manage.component').then((m) => m.NewsManageComponent)
          },
          {
            path: 'reviews',
            canActivate: [roleGuard],
            data: { permissions: ['REVIEW_VIEW', 'REVIEW_REPLY', 'REVIEW_DELETE'] },
            loadComponent: () =>
              import('./features/admin/review-manage/review-manage.component').then((m) => m.ReviewManageComponent)
          },
          {
            path: 'settings',
            canActivate: [roleGuard],
            data: { roles: ['ROLE_ADMIN'], permissions: ['SETTING_MANAGE', 'SETTING_VIEW'] },
            loadComponent: () =>
              import('./features/admin/setting-manage/setting-manage.component').then((m) => m.SettingManageComponent)
          },
          {
            path: 'statistics',
            canActivate: [roleGuard],
            data: { permissions: ['STATISTIC_VIEW', 'STATISTICS_VIEW'] },
            loadComponent: () =>
              import('./features/admin/statistics/statistics.component').then((m) => m.StatisticsComponent)
          },
          {
            path: 'suppliers',
            canActivate: [roleGuard],
            data: { permissions: ['SUPPLIER_VIEW', 'SUPPLIER_MANAGE'] },
            loadComponent: () =>
              import('./features/admin/supplier-manage/supplier-manage.component').then((m) => m.SupplierManageComponent)
          },
          {
            path: 'customers',
            canActivate: [roleGuard],
            data: { permissions: ['CUSTOMER_VIEW', 'USER_VIEW', 'USER_MANAGE'] },
            loadComponent: () =>
              import('./features/admin/customer-manage/customer-manage.component').then((m) => m.CustomerManageComponent)
          },
          {
            path: 'staff',
            canActivate: [roleGuard],
            data: { roles: ['ROLE_ADMIN'], permissions: ['STAFF_VIEW', 'STAFF_MANAGE'] },
            loadComponent: () =>
              import('./features/admin/staff-manage/staff-manage.component').then((m) => m.StaffManageComponent)
          },
          {
            path: 'roles',
            canActivate: [roleGuard],
            data: { roles: ['ROLE_ADMIN'], permissions: ['ROLE_VIEW', 'ROLE_MANAGE'] },
            loadComponent: () =>
              import('./features/admin/role-manage/role-manage.component').then((m) => m.RoleManageComponent)
          },
          {
            path: 'returns',
            canActivate: [roleGuard],
            data: { permissions: ['ORDER_VIEW', 'ORDER_MANAGE'] },
            loadComponent: () =>
              import('./features/admin/return-manage/return-manage.component').then((m) => m.ReturnManageComponent)
          },
          {
            path: 'audit-logs',
            canActivate: [roleGuard],
            data: { roles: ['ROLE_ADMIN'] },
            loadComponent: () =>
              import('./features/admin/audit-log/audit-log.component').then((m) => m.AuditLogComponent)
          },
          {
            path: 'forbidden',
            loadComponent: () =>
              import('./features/admin/forbidden/forbidden.component').then((m) => m.ForbiddenComponent)
          },
          {
            path: '403',
            redirectTo: 'forbidden',
            pathMatch: 'full'
          },
          {
            path: 'users',
            redirectTo: 'staff',
            pathMatch: 'full'
          }
        ]
      }
    ]
  },

  // 3. Storefront public routes (PublicShell)
  {
    path: '',
    loadComponent: () =>
      import('./layout/public-shell/public-shell.component').then((m) => m.PublicShellComponent),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/shop/home/home.component').then((m) => m.HomeComponent),
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./features/shop/product-listing/product-listing.component').then((m) => m.ProductListingComponent),
      },
      {
        path: 'products/:slug',
        loadComponent: () =>
          import('./features/shop/product-detail/product-detail.component').then((m) => m.ProductDetailComponent),
      },
      {
        path: 'cart',
        loadComponent: () =>
          import('./features/shop/cart/cart.component').then((m) => m.CartComponent),
      },
      {
        path: 'checkout',
        loadComponent: () =>
          import('./features/shop/checkout/checkout.component').then((m) => m.CheckoutComponent),
      },
      {
        path: 'order-success/:orderCode',
        loadComponent: () =>
          import('./features/shop/order-success/order-success.component').then((m) => m.OrderSuccessComponent),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./features/shop/order-tracking/order-tracking.component').then((m) => m.OrderTrackingComponent),
      },
      {
        path: 'orders/track',
        loadComponent: () =>
          import('./features/shop/order-tracking/order-tracking.component').then((m) => m.OrderTrackingComponent),
      },
      {
        path: 'orders/:orderCode',
        loadComponent: () =>
          import('./features/shop/order-tracking/order-tracking.component').then((m) => m.OrderTrackingComponent),
      },
      {
        path: 'account',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/shop/account/account-hub.component').then((m) => m.AccountHubComponent),
      },
      {
        path: 'account/orders',
        redirectTo: 'account',
        pathMatch: 'full'
      },
      {
        path: 'news',
        loadComponent: () =>
          import('./features/shop/news-listing/news-listing.component').then((m) => m.NewsListingComponent),
      },
      {
        path: 'news/:slug',
        loadComponent: () =>
          import('./features/shop/news-detail/news-detail.component').then((m) => m.NewsDetailComponent),
      },
    ],
  },

  // 4. Wildcard fallback
  { path: '**', redirectTo: '' }
];
