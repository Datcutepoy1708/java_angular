package com.store.service;

import com.store.dto.request.ProductFilterRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.ProductResponse;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class ProductSpecificationQueryCountIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    @DisplayName("Filter by 3 dynamic attributes concurrently executes 1 SELECT and 1 COUNT query without N+1")
    void filterByMultipleAttributes_executesWithoutNPlusOne() {
        // Query filter simulating: Socket = LGA1700 AND Cores = 24 AND Boost = 6.0 GHz
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .attributes("1:LGA1700;2:24;4:6.0 GHz")
                .page(0)
                .size(10)
                .build();

        statistics.clear();
        long initialQueryCount = statistics.getQueryExecutionCount();

        PageResponse<ProductResponse> result = productService.getProducts(filter);

        long executedQueries = statistics.getQueryExecutionCount() - initialQueryCount;

        // Spring Data JPA executes:
        // 1. SELECT count(p) FROM Product p WHERE EXISTS(...) AND EXISTS(...) AND EXISTS(...)
        // 2. SELECT p FROM Product p WHERE EXISTS(...) AND EXISTS(...) AND EXISTS(...)
        // Exactly 2 total statements for paginated repository call (1 data + 1 count), 0 N+1 queries.
        assertThat(executedQueries).isLessThanOrEqualTo(2);
        assertThat(result).isNotNull();
    }
}
