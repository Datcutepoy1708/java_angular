import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NewsService } from '../../../core/services/news.service';
import { News } from '../../../core/models/news.model';

@Component({
  selector: 'app-news-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './news-detail.component.html',
  styleUrl: './news-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewsDetailComponent implements OnInit {
  private readonly newsService = inject(NewsService);
  private readonly route = inject(ActivatedRoute);

  readonly article = signal<News | null>(null);
  readonly relatedNews = signal<News[]>([]);
  readonly loading = signal<boolean>(true);

  ngOnInit(): void {
    this.route.params.subscribe((params) => {
      const slug = params['slug'];
      if (slug) {
        this.loadArticle(slug);
      }
    });
  }

  loadArticle(slug: string): void {
    this.loading.set(true);
    this.newsService.getPublicNewsBySlug(slug).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const item = res.data;
          this.article.set(item);
          this.loadRelated(item.newsId, item.newsCatId);
        }
        this.loading.set(false);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err) => {
        console.error('Error loading news detail:', err);
        this.loading.set(false);
      }
    });
  }

  loadRelated(newsId: number, catId?: number): void {
    this.newsService.getRelatedNews(newsId, catId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.relatedNews.set(res.data);
        }
      }
    });
  }
}
