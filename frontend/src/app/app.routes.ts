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
      }
    ]
  },

  // Shorthand aliases
  { path: 'login', redirectTo: 'auth/login', pathMatch: 'full' },
  { path: 'register', redirectTo: 'auth/register', pathMatch: 'full' },

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
            loadComponent: () =>
              import('./features/admin/brand-manage/brand-manage.component').then((m) => m.BrandManageComponent)
          },
          {
            path: 'categories',
            loadComponent: () =>
              import('./features/admin/category-manage/category-manage.component').then((m) => m.CategoryManageComponent)
          },
          {
            path: 'category-attributes',
            loadComponent: () =>
              import('./features/admin/category-attributes-manage/category-attributes-manage.component').then((m) => m.CategoryAttributesManageComponent)
          },
          {
            path: 'products',
            loadComponent: () =>
              import('./features/admin/product-manage/product-manage.component').then((m) => m.ProductManageComponent)
          },
          {
            path: 'products/new',
            loadComponent: () =>
              import('./features/admin/product-manage/product-form/product-form.component').then((m) => m.ProductFormComponent)
          },
          {
            path: 'products/:id/edit',
            loadComponent: () =>
              import('./features/admin/product-manage/product-form/product-form.component').then((m) => m.ProductFormComponent)
          },
          {
            path: 'inventory',
            loadComponent: () =>
              import('./features/admin/inventory-manage/inventory-manage.component').then((m) => m.InventoryManageComponent)
          },
          {
            path: 'orders',
            loadComponent: () =>
              import('./features/admin/order-manage/order-manage.component').then((m) => m.OrderManageComponent)
          },
          {
            path: 'discounts',
            loadComponent: () =>
              import('./features/admin/discount-manage/discount-manage.component').then((m) => m.DiscountManageComponent)
          },
          {
            path: 'banners',
            loadComponent: () =>
              import('./features/admin/banner-manage/banner-manage.component').then((m) => m.BannerManageComponent)
          },
          {
            path: 'news',
            loadComponent: () =>
              import('./features/admin/news-manage/news-manage.component').then((m) => m.NewsManageComponent)
          },
          {
            path: 'reviews',
            loadComponent: () =>
              import('./features/admin/review-manage/review-manage.component').then((m) => m.ReviewManageComponent)
          },
          {
            path: 'settings',
            loadComponent: () =>
              import('./features/admin/setting-manage/setting-manage.component').then((m) => m.SettingManageComponent)
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
        path: 'orders/:orderCode',
        loadComponent: () =>
          import('./features/shop/order-tracking/order-tracking.component').then((m) => m.OrderTrackingComponent),
      },
      {
        path: 'account/orders',
        loadComponent: () =>
          import('./features/shop/order-history/order-history.component').then((m) => m.OrderHistoryComponent),
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
