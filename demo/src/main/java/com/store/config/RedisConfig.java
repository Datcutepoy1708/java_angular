package com.store.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.SerializationException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put("products", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        configs.put("productDetail", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        configs.put("categories", defaultConfig.entryTtl(Duration.ofHours(2)));
        configs.put("brands", defaultConfig.entryTtl(Duration.ofHours(2)));
        configs.put("categoryAttributes", defaultConfig.entryTtl(Duration.ofHours(2)));
        configs.put("banners", defaultConfig.entryTtl(Duration.ofHours(1)));
        configs.put("news", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        configs.put("newsDetail", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        configs.put("productRatingSummary", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        configs.put("productReviews", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        configs.put("systemSettings", defaultConfig.entryTtl(Duration.ofHours(24)));
        configs.put("statistics", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        configs.put("suppliers", defaultConfig.entryTtl(Duration.ofHours(2)));
        configs.put("roles", defaultConfig.entryTtl(Duration.ofHours(2)));
        configs.put("permissions", defaultConfig.entryTtl(Duration.ofHours(2)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configs)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {

            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                if (isSerializationOrCompatibilityError(exception)) {
                    // 🔴 LỖI TƯƠNG THÍCH DỮ LIỆU / SERIALIZATION (DTO thay đổi hoặc cache cũ bị lỗi format)
                    // Bắt buộc log ERROR để hệ thống giám sát (ELK, Sentry, Prometheus) bắt được cảnh báo.
                    // Đồng thời xóa key hỏng để lần request tiếp theo ghi dữ liệu mới chuẩn hóa.
                    log.error("[REDIS CACHE DATA CORRUPTION] Deserialization failed for cache '{}', key '{}'. " +
                                    "The cached payload is incompatible with current DTO structure! " +
                                    "Action: Evicting corrupt key from Redis. Falling back to database query. Cause: {}",
                            cache.getName(), key, exception.getMessage(), exception);
                    try {
                        cache.evict(key);
                    } catch (Exception e) {
                        log.error("Failed to evict corrupt cache key '{}' from cache '{}'", key, cache.getName(), e);
                    }
                } else {
                    // 🟡 LỖI HẠ TẦNG (Redis down, connection timeout, network partition)
                    // Chỉ log WARN và fallback an toàn về DB, không spam evict khi hạ tầng mất kết nối
                    log.warn("[REDIS INFRASTRUCTURE UNAVAILABLE] Redis GET failed for cache '{}', key '{}': {}. " +
                            "Falling back to database query.", cache.getName(), key, exception.getMessage());
                }
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                if (isSerializationOrCompatibilityError(exception)) {
                    log.error("[REDIS CACHE SERIALIZATION ERROR] Failed to serialize value for cache '{}', key '{}': {}",
                            cache.getName(), key, exception.getMessage(), exception);
                } else {
                    log.warn("[REDIS INFRASTRUCTURE UNAVAILABLE] Redis PUT failed for cache '{}', key '{}': {}",
                            cache.getName(), key, exception.getMessage());
                }
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("[REDIS INFRASTRUCTURE UNAVAILABLE] Redis EVICT failed for cache '{}', key '{}': {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("[REDIS INFRASTRUCTURE UNAVAILABLE] Redis CLEAR failed for cache '{}': {}",
                        cache.getName(), exception.getMessage());
            }

            /**
             * Phân biệt lỗi tương thích dữ liệu (Serialization / Deserialization / Jackson parse)
             * so với lỗi kết nối hạ tầng (Connection refused / Timeout / Network down).
             */
            private boolean isSerializationOrCompatibilityError(Throwable ex) {
                Throwable cause = ex;
                while (cause != null) {
                    if (cause instanceof SerializationException
                            || cause instanceof JacksonException
                            || cause instanceof ClassCastException
                            || cause instanceof IllegalArgumentException) {
                        return true;
                    }
                    cause = cause.getCause();
                }
                return false;
            }
        };
    }
}
