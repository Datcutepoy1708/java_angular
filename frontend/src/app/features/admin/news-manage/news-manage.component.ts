import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NewsService } from '../../../core/services/news.service';
import { UploadService } from '../../../core/services/upload.service';
import {
  CreateNewsRequest,
  News,
  NewsCategory,
  NewsCategoryRequest,
  NewsFilterParams,
  NewsStatus,
  UpdateNewsRequest,
} from '../../../core/models/news.model';
import { ImageUploadComponent } from '../../../shared/components/image-upload/image-upload.component';

@Component({
  selector: 'app-news-manage',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DatePipe, ImageUploadComponent],
  templateUrl: './news-manage.component.html',
  styleUrl: './news-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewsManageComponent implements OnInit {
  private readonly newsService = inject(NewsService);
  private readonly uploadService = inject(UploadService);
  private readonly fb = inject(FormBuilder);

  readonly newsList = signal<News[]>([]);
  readonly categories = signal<NewsCategory[]>([]);
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly uploadingThumbnail = signal<boolean>(false);
  readonly thumbnailPreview = signal<string | null>(null);

  // Filters & Pagination
  readonly filterCategoryId = signal<number | null>(null);
  readonly filterStatus = signal<NewsStatus | ''>('');
  readonly filterKeyword = signal<string>('');
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(1);

  // Article Modal State
  readonly showArticleModal = signal<boolean>(false);
  readonly isEditing = signal<boolean>(false);
  readonly editingNewsId = signal<number | null>(null);

  // Category Modal State
  readonly showCategoryModal = signal<boolean>(false);
  readonly editingCatId = signal<number | null>(null);

  // Delete State
  readonly showDeleteModal = signal<boolean>(false);
  readonly deletingNewsId = signal<number | null>(null);
  readonly deletingTitle = signal<string>('');

  readonly articleForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(250)]],
    slug: ['', [Validators.maxLength(280)]],
    newsCatId: [null],
    thumbnailUrl: ['', [Validators.maxLength(500)]],
    summary: ['', [Validators.maxLength(500)]],
    content: ['', [Validators.required]],
    status: ['draft', [Validators.required]],
  });

  readonly categoryForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    slug: ['', [Validators.maxLength(180)]],
    description: ['', [Validators.maxLength(255)]],
    sortOrder: [0, [Validators.min(0)]],
    status: ['active', [Validators.required]],
  });

  ngOnInit(): void {
    this.loadCategories();
    this.loadNews();
  }

  loadCategories(): void {
    this.newsService.getAdminCategories().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.categories.set(res.data);
        }
      },
    });
  }

  loadNews(): void {
    this.loading.set(true);
    const filter: NewsFilterParams = {
      categoryId: this.filterCategoryId() ?? undefined,
      status: this.filterStatus() ? (this.filterStatus() as NewsStatus) : undefined,
      keyword: this.filterKeyword().trim() || undefined,
      page: this.currentPage(),
      size: this.pageSize(),
      sortBy: 'createdAt',
      sortDir: 'desc',
    };

    this.newsService.getAdminNews(filter).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.newsList.set(res.data.content);
          this.totalPages.set(res.data.totalPages);
          this.totalElements.set(res.data.totalElements);
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading news:', err);
        this.loading.set(false);
      },
    });
  }

  onSearch(): void {
    this.currentPage.set(0);
    this.loadNews();
  }

  resetFilter(): void {
    this.filterCategoryId.set(null);
    this.filterStatus.set('');
    this.filterKeyword.set('');
    this.currentPage.set(0);
    this.loadNews();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadNews();
    }
  }

  // ── Article Operations ─────────────────────────────────────────

  openCreateArticleModal(): void {
    this.isEditing.set(false);
    this.editingNewsId.set(null);
    this.thumbnailPreview.set(null);
    this.articleForm.reset({
      title: '',
      slug: '',
      newsCatId: this.categories().length > 0 ? this.categories()[0].newsCatId : null,
      thumbnailUrl: '',
      summary: '',
      content: '',
      status: 'draft',
    });
    this.showArticleModal.set(true);
  }

  openEditArticleModal(news: News): void {
    this.isEditing.set(true);
    this.editingNewsId.set(news.newsId);
    this.thumbnailPreview.set(news.thumbnailUrl || null);
    this.articleForm.patchValue({
      title: news.title,
      slug: news.slug,
      newsCatId: news.newsCatId,
      thumbnailUrl: news.thumbnailUrl || '',
      summary: news.summary || '',
      content: news.content,
      status: news.status,
    });
    this.showArticleModal.set(true);
  }

  closeArticleModal(): void {
    this.showArticleModal.set(false);
  }

  handleThumbnailUpload(file: File): void {
    const reader = new FileReader();
    reader.onload = (e) => {
      this.thumbnailPreview.set(e.target?.result as string);
    };
    reader.readAsDataURL(file);

    this.uploadingThumbnail.set(true);
    this.uploadService.uploadImage(file).subscribe({
      next: (url) => {
        this.articleForm.patchValue({ thumbnailUrl: url });
        this.thumbnailPreview.set(url);
        this.uploadingThumbnail.set(false);
      },
      error: (err) => {
        console.error('Thumbnail upload error:', err);
        alert('Tải ảnh đại diện bài viết lên máy chủ thất bại. Bạn có thể nhập link ảnh trực tiếp.');
        this.uploadingThumbnail.set(false);
      },
    });
  }

  onThumbnailUrlChange(url: string): void {
    this.articleForm.patchValue({ thumbnailUrl: url });
    this.thumbnailPreview.set(url || null);
  }

  onThumbnailRemoved(): void {
    this.articleForm.patchValue({ thumbnailUrl: '' });
    this.thumbnailPreview.set(null);
  }

  saveArticle(): void {
    if (this.articleForm.invalid) {
      this.articleForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const val = this.articleForm.value;

    if (this.isEditing() && this.editingNewsId()) {
      const req: UpdateNewsRequest = {
        title: val.title.trim(),
        slug: val.slug ? val.slug.trim() : undefined,
        newsCatId: val.newsCatId ? Number(val.newsCatId) : undefined,
        thumbnailUrl: val.thumbnailUrl ? val.thumbnailUrl.trim() : undefined,
        summary: val.summary ? val.summary.trim() : undefined,
        content: val.content,
        status: val.status,
      };

      this.newsService.updateNews(this.editingNewsId()!, req).subscribe({
        next: () => {
          this.saving.set(false);
          this.closeArticleModal();
          this.loadNews();
        },
        error: (err) => {
          console.error('Error updating article:', err);
          this.saving.set(false);
        },
      });
    } else {
      const req: CreateNewsRequest = {
        title: val.title.trim(),
        slug: val.slug ? val.slug.trim() : undefined,
        newsCatId: val.newsCatId ? Number(val.newsCatId) : undefined,
        thumbnailUrl: val.thumbnailUrl ? val.thumbnailUrl.trim() : undefined,
        summary: val.summary ? val.summary.trim() : undefined,
        content: val.content,
        status: val.status,
      };

      this.newsService.createNews(req).subscribe({
        next: () => {
          this.saving.set(false);
          this.closeArticleModal();
          this.loadNews();
        },
        error: (err) => {
          console.error('Error creating article:', err);
          this.saving.set(false);
        },
      });
    }
  }

  openDeleteModal(news: News): void {
    this.deletingNewsId.set(news.newsId);
    this.deletingTitle.set(news.title);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.deletingNewsId.set(null);
  }

  confirmDelete(): void {
    const id = this.deletingNewsId();
    if (!id) return;

    this.newsService.deleteNews(id).subscribe({
      next: () => {
        this.closeDeleteModal();
        this.loadNews();
      },
      error: (err) => {
        console.error('Error deleting news:', err);
        this.closeDeleteModal();
      },
    });
  }

  // ── Category Operations ────────────────────────────────────────

  openCategoryModal(): void {
    this.editingCatId.set(null);
    this.categoryForm.reset({
      name: '',
      slug: '',
      description: '',
      sortOrder: 0,
      status: 'active',
    });
    this.showCategoryModal.set(true);
  }

  closeCategoryModal(): void {
    this.showCategoryModal.set(false);
  }

  editCategory(cat: NewsCategory): void {
    this.editingCatId.set(cat.newsCatId);
    this.categoryForm.patchValue({
      name: cat.name,
      slug: cat.slug,
      description: cat.description || '',
      sortOrder: cat.sortOrder,
      status: cat.status,
    });
  }

  saveCategory(): void {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }

    const val = this.categoryForm.value;
    const req: NewsCategoryRequest = {
      name: val.name.trim(),
      slug: val.slug ? val.slug.trim() : undefined,
      description: val.description ? val.description.trim() : undefined,
      sortOrder: Number(val.sortOrder) || 0,
      status: val.status,
    };

    if (this.editingCatId()) {
      this.newsService.updateCategory(this.editingCatId()!, req).subscribe({
        next: () => {
          this.editingCatId.set(null);
          this.categoryForm.reset({ name: '', slug: '', description: '', sortOrder: 0, status: 'active' });
          this.loadCategories();
        },
        error: (err) => console.error('Error updating category:', err),
      });
    } else {
      this.newsService.createCategory(req).subscribe({
        next: () => {
          this.categoryForm.reset({ name: '', slug: '', description: '', sortOrder: 0, status: 'active' });
          this.loadCategories();
        },
        error: (err) => console.error('Error creating category:', err),
      });
    }
  }

  deleteCategory(catId: number): void {
    if (confirm('Bạn có chắc chắn muốn xóa danh mục này?')) {
      this.newsService.deleteCategory(catId).subscribe({
        next: () => this.loadCategories(),
        error: (err) => console.error('Error deleting category:', err),
      });
    }
  }
}
