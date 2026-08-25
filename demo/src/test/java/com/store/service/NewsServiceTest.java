package com.store.service;

import com.store.dto.request.news.CreateNewsRequest;
import com.store.dto.request.news.NewsCategoryRequest;
import com.store.dto.request.news.UpdateNewsRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.news.NewsCategoryResponse;
import com.store.dto.response.news.NewsResponse;
import com.store.entity.news.News;
import com.store.entity.news.NewsCategory;
import com.store.entity.news.NewsStatus;
import com.store.entity.user.User;
import com.store.repository.NewsCategoryRepository;
import com.store.repository.NewsRepository;
import com.store.repository.UserRepository;
import com.store.service.impl.NewsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsCategoryRepository newsCategoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NewsServiceImpl newsService;

    private User author;
    private NewsCategory category;
    private News newsArticle;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .userId(1L)
                .fullName("Admin Tech")
                .email("admin@complexus.com")
                .build();

        category = NewsCategory.builder()
                .newsCatId(1)
                .name("Tin Công Nghệ")
                .slug("tin-cong-nghe")
                .status("active")
                .build();

        newsArticle = News.builder()
                .newsId(10L)
                .category(category)
                .title("Ra Mắt Card Đồ Họa RTX 5090 Mới")
                .slug("ra-mat-card-do-hoa-rtx-5090-moi")
                .summary("Chi tiết cấu hình và hiệu năng vượt trội.")
                .content("<p>Nội dung chi tiết...</p>")
                .author(author)
                .viewCount(100)
                .status(NewsStatus.PUBLISHED)
                .build();
    }

    @Test
    @DisplayName("Get public news returns published articles")
    void testGetPublicNews() {
        when(newsRepository.findByStatusOrderByPublishedAtDesc(eq(NewsStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(newsArticle)));

        PageResponse<NewsResponse> response = newsService.getPublicNews(null, 0, 10);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("Ra Mắt Card Đồ Họa RTX 5090 Mới");
    }

    @Test
    @DisplayName("Get public news by slug increments view count and returns article")
    void testGetPublicNewsBySlug() {
        when(newsRepository.findBySlugAndStatus("ra-mat-card-do-hoa-rtx-5090-moi", NewsStatus.PUBLISHED))
                .thenReturn(Optional.of(newsArticle));

        NewsResponse response = newsService.getPublicNewsBySlug("ra-mat-card-do-hoa-rtx-5090-moi");

        assertThat(response).isNotNull();
        assertThat(response.getNewsId()).isEqualTo(10L);
        assertThat(response.getViewCount()).isEqualTo(101); // incremented
        verify(newsRepository).incrementViewCount(10L);
    }

    @Test
    @DisplayName("Create news generates slug automatically if slug not provided")
    void testCreateNews_AutoSlug() {
        CreateNewsRequest request = CreateNewsRequest.builder()
                .newsCatId(1)
                .title("Đánh Giá Chi Tiết CPU Intel Core Ultra")
                .summary("Hiệu năng tối ưu cho game thủ")
                .content("<p>Đánh giá chi tiết</p>")
                .status(NewsStatus.PUBLISHED)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(newsCategoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(newsRepository.existsBySlug(any())).thenReturn(false);

        when(newsRepository.save(any(News.class))).thenAnswer(inv -> {
            News n = inv.getArgument(0);
            n.setNewsId(20L);
            return n;
        });

        NewsResponse response = newsService.createNews(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getSlug()).isEqualTo("danh-gia-chi-tiet-cpu-intel-core-ultra");
        assertThat(response.getStatus()).isEqualTo(NewsStatus.PUBLISHED);
        assertThat(response.getPublishedAt()).isNotNull();
        verify(newsRepository).save(any(News.class));
    }

    @Test
    @DisplayName("Create news category creates category successfully")
    void testCreateCategory() {
        NewsCategoryRequest request = NewsCategoryRequest.builder()
                .name("Thủ Thuật Máy Tính")
                .description("Mẹo hay về phần mềm và phần cứng")
                .build();

        when(newsCategoryRepository.existsBySlug("thu-thuat-may-tinh")).thenReturn(false);
        when(newsCategoryRepository.save(any(NewsCategory.class))).thenAnswer(inv -> {
            NewsCategory c = inv.getArgument(0);
            c.setNewsCatId(2);
            return c;
        });

        NewsCategoryResponse response = newsService.createCategory(request);

        assertThat(response).isNotNull();
        assertThat(response.getNewsCatId()).isEqualTo(2);
        assertThat(response.getSlug()).isEqualTo("thu-thuat-may-tinh");
    }
}
