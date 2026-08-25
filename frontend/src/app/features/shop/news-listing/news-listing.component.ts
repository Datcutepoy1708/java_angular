import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NewsService } from '../../../core/services/news.service';
import { News, NewsCategory } from '../../../core/models/news.model';

@Component({
  selector: 'app-news-listing',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, DatePipe],
  templateUrl: './news-listing.component.html',
  styleUrl: './news-listing.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewsListingComponent implements OnInit {
  private readonly newsService = inject(NewsService);
  private readonly route = inject(ActivatedRoute);

  readonly categories = signal<NewsCategory[]>([]);
  readonly newsList = signal<News[]>([]);
  readonly selectedCatId = signal<number | null>(null);
  readonly currentPage = signal<number>(0);
  readonly totalPages = signal<number>(1);
  readonly totalElements = signal<number>(0);
  readonly loading = signal<boolean>(true);

  ngOnInit(): void {
    this.loadCategories();
    this.route.queryParams.subscribe((params) => {
      const catId = params['cat'] ? Number(params['cat']) : null;
      this.selectedCatId.set(catId);
      this.currentPage.set(0);
      this.loadNews();
    });
  }

  loadCategories(): void {
    this.newsService.getPublicCategories().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.categories.set(res.data);
        }
      }
    });
  }

  loadNews(): void {
    this.loading.set(true);
    const catId = this.selectedCatId() ?? undefined;
    this.newsService.getPublicNews(catId, this.currentPage(), 9).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.newsList.set(res.data.content);
          this.totalPages.set(res.data.totalPages);
          this.totalElements.set(res.data.totalElements);
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading news list:', err);
        this.loading.set(false);
      }
    });
  }

  selectCategory(catId: number | null): void {
    this.selectedCatId.set(catId);
    this.currentPage.set(0);
    this.loadNews();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadNews();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }
}
