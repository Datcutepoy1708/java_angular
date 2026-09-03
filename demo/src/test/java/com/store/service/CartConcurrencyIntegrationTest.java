package com.store.service;

import com.store.dto.request.cart.AddToCartRequest;
import com.store.entity.cart.CartItem;
import com.store.repository.CartItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
class CartConcurrencyIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private com.store.util.TestFixtureHelper fixtureHelper;

    @Autowired
    private com.store.repository.UserRepository userRepository;

    @Autowired
    private com.store.repository.ProductVariantRepository productVariantRepository;

    @Test
    @DisplayName("Stress Test: 20 concurrent threads adding same variant to cart - zero duplicate key errors, exact accumulated total")
    void testConcurrentAddToCart_AtomicUpsert_NoDuplicateKeyError() throws InterruptedException {
        fixtureHelper.ensureBasicFixtures();
        com.store.entity.user.User customer = userRepository.findAll().stream().findFirst().orElseThrow();
        com.store.entity.product.ProductVariant variant = productVariantRepository.findAll().stream().findFirst().orElseThrow();
        Long testUserId = customer.getUserId();
        Long testVariantId = variant.getVariantId();
        int threadCount = 20;
        int qtyPerThread = 2;

        // Clean up before test
        cartService.clearCart(testUserId);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    cartService.addToCart(testUserId, new AddToCartRequest(testVariantId, qtyPerThread));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        // Verification
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failureCount.get()).isZero();

        CartItem item = cartItemRepository.findByUserUserIdAndVariantVariantId(testUserId, testVariantId).orElse(null);
        assertThat(item).isNotNull();
        // Exact sum: 20 threads * 2 qty = 40
        assertThat(item.getQuantity()).isEqualTo(threadCount * qtyPerThread);

        // Clean up
        cartService.clearCart(testUserId);
    }
}
