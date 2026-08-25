package com.store.service.impl;

import com.store.dto.request.news.CreateNewsRequest;
import com.store.dto.request.news.NewsCategoryRequest;
import com.store.dto.request.news.NewsFilterRequest;
import com.store.dto.request.news.UpdateNewsRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.news.NewsCategoryResponse;
import com.store.dto.response.news.NewsResponse;
import com.store.entity.news.News;
import com.store.entity.news.NewsCategory;
import com.store.entity.news.NewsStatus;
import com.store.entity.user.User;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.NewsCategoryRepository;
import com.store.repository.NewsRepository;
import com.store.repository.UserRepository;
import com.store.service.NewsService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsCategoryRepository newsCategoryRepository;
    private final UserRepository userRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        slug = slug.replaceAll("đ", "d").replaceAll("Đ", "d");
        slug = NONLATIN.matcher(slug).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-");
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "news", key = "'pub_' + (#categoryId != null ? #categoryId : 'all') + '_p' + #page + '_s' + #size")
    public PageResponse<NewsResponse> getPublicNews(Integer categoryId, int page, int size) {
        log.info("Fetching public news for category: {}, page: {}, size: {}", categoryId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage;

        if (categoryId != null && categoryId > 0) {
            newsPage = newsRepository.findByCategory_NewsCatIdAndStatusOrderByPublishedAtDesc(
                    categoryId, NewsStatus.PUBLISHED, pageable);
        } else {
            newsPage = newsRepository.findByStatusOrderByPublishedAtDesc(NewsStatus.PUBLISHED, pageable);
        }

        return PageResponse.of(newsPage.map(NewsResponse::fromEntity));
    }

    @Override
    @Transactional
    public NewsResponse getPublicNewsBySlug(String slug) {
        log.info("Fetching public news by slug: {}", slug);
        News news = newsRepository.findBySlugAndStatus(slug, NewsStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết tin tức: " + slug));

        // Increment view count asynchronously/safely
        newsRepository.incrementViewCount(news.getNewsId());
        news.setViewCount(news.getViewCount() + 1);

        return NewsResponse.fromEntity(news);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsResponse> getRelatedNews(Long newsId, Integer categoryId) {
        List<News> list;
        if (categoryId != null) {
            list = newsRepository.findTop4ByCategory_NewsCatIdAndNewsIdNotAndStatusOrderByPublishedAtDesc(
                    categoryId, newsId, NewsStatus.PUBLISHED);
        } else {
            list = newsRepository.findTop4ByNewsIdNotAndStatusOrderByPublishedAtDesc(newsId, NewsStatus.PUBLISHED);
        }
        return list.stream().map(NewsResponse::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsCategoryResponse> getPublicCategories() {
        return newsCategoryRepository.findByStatusOrderBySortOrderAsc("active").stream()
                .map(NewsCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NewsResponse> getAdminNews(NewsFilterRequest filter) {
        Sort sort = Sort.by(
                "asc".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC,
                filter.getSortBy() != null ? filter.getSortBy() : "createdAt"
        );
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Specification<News> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("newsCatId"), filter.getCategoryId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String kw = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                Predicate titlePred = cb.like(cb.lower(root.get("title")), kw);
                Predicate summaryPred = cb.like(cb.lower(root.get("summary")), kw);
                predicates.add(cb.or(titlePred, summaryPred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<News> page = newsRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(NewsResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public NewsResponse getNewsById(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết với id: " + newsId));
        return NewsResponse.fromEntity(news);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "news", allEntries = true),
        @CacheEvict(cacheNames = "newsDetail", allEntries = true)
    })
    public NewsResponse createNews(Long authorId, CreateNewsRequest request) {
        log.info("Author {} creating news: {}", authorId, request.getTitle());

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tác giả với id: " + authorId));

        NewsCategory category = null;
        if (request.getNewsCatId() != null) {
            category = newsCategoryRepository.findById(request.getNewsCatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục tin tức: " + request.getNewsCatId()));
        }

        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = toSlug(request.getTitle());
        }
        if (newsRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 10000;
        }

        LocalDateTime publishedAt = null;
        if (request.getStatus() == NewsStatus.PUBLISHED) {
            publishedAt = LocalDateTime.now();
        }

        News news = News.builder()
                .category(category)
                .title(request.getTitle())
                .slug(slug)
                .thumbnailUrl(request.getThumbnailUrl())
                .summary(request.getSummary())
                .content(request.getContent())
                .author(author)
                .viewCount(0)
                .status(request.getStatus())
                .publishedAt(publishedAt)
                .build();

        News saved = newsRepository.save(news);
        return NewsResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "news", allEntries = true),
        @CacheEvict(cacheNames = "newsDetail", allEntries = true)
    })
    public NewsResponse updateNews(Long newsId, UpdateNewsRequest request) {
        log.info("Updating news id: {}", newsId);

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết với id: " + newsId));

        NewsCategory category = null;
        if (request.getNewsCatId() != null) {
            category = newsCategoryRepository.findById(request.getNewsCatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục tin tức: " + request.getNewsCatId()));
        }

        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = toSlug(request.getTitle());
        }
        if (newsRepository.existsBySlugAndNewsIdNot(slug, newsId)) {
            slug = slug + "-" + System.currentTimeMillis() % 10000;
        }

        if (news.getStatus() != NewsStatus.PUBLISHED && request.getStatus() == NewsStatus.PUBLISHED && news.getPublishedAt() == null) {
            news.setPublishedAt(LocalDateTime.now());
        }

        news.setCategory(category);
        news.setTitle(request.getTitle());
        news.setSlug(slug);
        news.setThumbnailUrl(request.getThumbnailUrl());
        news.setSummary(request.getSummary());
        news.setContent(request.getContent());
        news.setStatus(request.getStatus());

        News saved = newsRepository.save(news);
        return NewsResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "news", allEntries = true),
        @CacheEvict(cacheNames = "newsDetail", allEntries = true)
    })
    public void deleteNews(Long newsId) {
        log.info("Deleting news id: {}", newsId);
        if (!newsRepository.existsById(newsId)) {
            throw new ResourceNotFoundException("Không tìm thấy bài viết với id: " + newsId);
        }
        newsRepository.deleteById(newsId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsCategoryResponse> getAdminCategories() {
        return newsCategoryRepository.findAll().stream()
                .map(NewsCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NewsCategoryResponse createCategory(NewsCategoryRequest request) {
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = toSlug(request.getName());
        }
        if (newsCategoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Slug danh mục tin tức đã tồn tại: " + slug);
        }

        NewsCategory cat = NewsCategory.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .status(request.getStatus() != null ? request.getStatus() : "active")
                .build();

        NewsCategory saved = newsCategoryRepository.save(cat);
        return NewsCategoryResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public NewsCategoryResponse updateCategory(Integer catId, NewsCategoryRequest request) {
        NewsCategory cat = newsCategoryRepository.findById(catId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục tin tức với id: " + catId));

        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = toSlug(request.getName());
        }
        if (newsCategoryRepository.existsBySlugAndNewsCatIdNot(slug, catId)) {
            throw new DuplicateResourceException("Slug danh mục tin tức đã tồn tại: " + slug);
        }

        cat.setName(request.getName());
        cat.setSlug(slug);
        cat.setDescription(request.getDescription());
        cat.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        cat.setStatus(request.getStatus() != null ? request.getStatus() : "active");

        NewsCategory saved = newsCategoryRepository.save(cat);
        return NewsCategoryResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Integer catId) {
        if (!newsCategoryRepository.existsById(catId)) {
            throw new ResourceNotFoundException("Không tìm thấy danh mục tin tức với id: " + catId);
        }
        newsCategoryRepository.deleteById(catId);
    }
}
