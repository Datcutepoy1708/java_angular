import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { NewsService } from './news.service';
import { environment } from '../../../environments/environment';
import { News, NewsCategory } from '../models/news.model';
import { PageResponse } from '../models/discount.model';

describe('NewsService', () => {
  let service: NewsService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        NewsService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(NewsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch public news list', () => {
    const mockPage: PageResponse<News> = {
      content: [{
        newsId: 1,
        title: 'Tin Mới Nhất',
        slug: 'tin-moi-nhat',
        content: '<p>Nội dung</p>',
        viewCount: 50,
        status: 'published',
        createdAt: '2026-08-25'
      }],
      number: 0,
      size: 9,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      empty: false
    };

    service.getPublicNews().subscribe(res => {
      expect(res.data.content.length).toBe(1);
      expect(res.data.content[0].slug).toBe('tin-moi-nhat');
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/news?page=0&size=9`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockPage });
  });

  it('should fetch public news by slug', () => {
    const mockNews: News = {
      newsId: 1,
      title: 'Đánh giá CPU',
      slug: 'danh-gia-cpu',
      content: '<p>Nội dung</p>',
      viewCount: 120,
      status: 'published',
      createdAt: '2026-08-25'
    };

    service.getPublicNewsBySlug('danh-gia-cpu').subscribe(res => {
      expect(res.data.title).toBe('Đánh giá CPU');
      expect(res.data.viewCount).toBe(120);
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/news/danh-gia-cpu`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockNews });
  });

  it('should fetch public categories', () => {
    const mockCats: NewsCategory[] = [{
      newsCatId: 1,
      name: 'Tin Công Nghệ',
      slug: 'tin-cong-nghe',
      sortOrder: 1,
      status: 'active'
    }];

    service.getPublicCategories().subscribe(res => {
      expect(res.data.length).toBe(1);
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/news/categories`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockCats });
  });
});
