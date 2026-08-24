import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { CategoryService } from '../../../core/services/category.service';
import { BrandService } from '../../../core/services/brand.service';
import { ProductFilterRequest, ProductResponse } from '../../../core/models/product.model';
import { CategoryResponse } from '../../../core/models/category.model';
import { BrandResponse } from '../../../core/models/brand.model';
import { ProductCardComponent } from '../../../shared/components/product-card/product-card.component';

interface HeroSlide {
  title: string;
  subtitle: string;
  tag: string;
  link: string;
  categoryQuery: string;
  image: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, ProductCardComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly brandService = inject(BrandService);

  // ── Hero Slider State ──────────────────────────────────────────
  readonly currentSlideIndex = signal(0);
  readonly slides: HeroSlide[] = [
    {
      title: 'LAPTOP GAMING & AI THẾ HỆ MỚI',
      subtitle: 'Trang bị RTX 40-Series & CPU Intel Gen 14th / AMD Ryzen AI đỉnh cao',
      tag: 'CÔNG NGHỆ ĐỈNH CAO',
      link: '/products',
      categoryQuery: 'laptop-gaming',
      image: 'http://localhost:8080/uploads/categories/laptop-gaming.png',
    },
    {
      title: 'PC WORKSTATION & GAMING STREAMER',
      subtitle: 'Tối ưu hiệu năng render đồ họa 3D, Premiere, livestream chuyên nghiệp',
      tag: 'HIỆU NĂNG TỐI ĐA',
      link: '/products',
      categoryQuery: 'pc-gaming-streamer',
      image: 'http://localhost:8080/uploads/categories/pc-gaming-streamer.png',
    },
    {
      title: 'HỆ SINH THÁI APPLE CHÍNH HÃNG',
      subtitle: 'MacBook Air M3, MacBook Pro M3 Max và phụ kiện Apple chính hãng',
      tag: 'CHÍNH HÃNG APPLE',
      link: '/products',
      categoryQuery: 'macbook-apple',
      image: 'http://localhost:8080/uploads/categories/macbook-apple.jpg',
    },
  ];

  // ── Data Signals ───────────────────────────────────────────────
  readonly categories = signal<CategoryResponse[]>([]);
  readonly hotDeals = signal<ProductResponse[]>([]);
  readonly bestSellers = signal<ProductResponse[]>([]);
  readonly brands = signal<BrandResponse[]>([]);

  // ── UI States ──────────────────────────────────────────────────
  readonly activeBestSellerTab = signal<string>('all');
  readonly loadingDeals = signal(true);
  readonly loadingBestSellers = signal(true);

  readonly bestSellerTabs = [
    { id: 'all', label: 'Tất cả', categoryId: null },
    { id: 'laptop', label: 'Laptop & Mac', categoryId: 1 },
    { id: 'pc', label: 'Máy tính PC', categoryId: 2 },
    { id: 'screen', label: 'Màn hình', categoryId: 3 },
    { id: 'hardware', label: 'Linh kiện', categoryId: 4 },
    { id: 'accessories', label: 'Phụ kiện & Gear', categoryId: 5 },
  ];

  ngOnInit(): void {
    this.loadCategories();
    this.loadHotDeals();
    this.loadBestSellers(null);
    this.loadBrands();
  }

  loadCategories(): void {
    this.categoryService.getRoots().subscribe({
      next: (res: { data: CategoryResponse[] }) => {
        this.categories.set(res.data);
      },
    });
  }

  loadHotDeals(): void {
    this.loadingDeals.set(true);
    this.productService.getProducts({ page: 0, size: 20 }).subscribe({
      next: (res) => {
        // Filter products that have at least 1 variant on sale (salePrice < price)
        const deals = res.data.content.filter((p) =>
          p.variants?.some((v) => v.status === 'active' && v.salePrice != null && v.salePrice > 0 && v.salePrice < v.price)
        );
        this.hotDeals.set(deals.slice(0, 8));
        this.loadingDeals.set(false);
      },
      error: () => this.loadingDeals.set(false),
    });
  }

  loadBestSellers(categoryId: number | null): void {
    this.loadingBestSellers.set(true);
    const filter: ProductFilterRequest = {
      page: 0,
      size: 8,
      sortBy: 'viewCount',
      sortDir: 'desc',
    };
    if (categoryId) {
      filter.categoryId = categoryId;
    }

    this.productService.getProducts(filter).subscribe({
      next: (res) => {
        this.bestSellers.set(res.data.content);
        this.loadingBestSellers.set(false);
      },
      error: () => this.loadingBestSellers.set(false),
    });
  }

  loadBrands(): void {
    this.brandService.getAll().subscribe({
      next: (res: { data: BrandResponse[] }) => {
        this.brands.set(res.data);
      },
    });
  }

  setTab(tabId: string): void {
    this.activeBestSellerTab.set(tabId);
    const target = this.bestSellerTabs.find((t) => t.id === tabId);
    this.loadBestSellers(target ? target.categoryId : null);
  }

  nextSlide(): void {
    this.currentSlideIndex.update((i) => (i + 1) % this.slides.length);
  }

  prevSlide(): void {
    this.currentSlideIndex.update((i) => (i - 1 + this.slides.length) % this.slides.length);
  }

  onAddToCart(product: ProductResponse): void {
    console.log('Thêm sản phẩm vào giỏ hàng:', product.name);
  }
}
