package com.store.config;

import com.store.dto.response.ChatBotRuleResponse;
import com.store.entity.chat.MatchType;
import com.store.entity.chat.RuleActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisConfigSerializationTest {
    private SerializationPair<Object> serialization;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        var manager = new RedisConfig().cacheManager(mock(RedisConnectionFactory.class));
        manager.afterPropertiesSet();
        serialization = (SerializationPair<Object>) manager.getCacheConfigurations()
                .get("chatBotRules").getValueSerializationPair();
    }

    @Test
    void roundTripsImmutableRuleListReturnedByStreamToList() {
        var rules = Stream.of(rule()).toList();
        assertThat(roundTrip(rules)).isEqualTo(rules);
    }

    @Test
    void roundTripsFinalRecordWithEnumsAndTime() {
        var rule = rule();
        assertThat(roundTrip(rule)).isEqualTo(rule);
    }

    @Test
    void roundTripsEmptyListsAndImmutableMaps() {
        assertThat(roundTrip(List.of())).isEqualTo(List.of());
        var settings = Map.of("title", "Hỗ trợ khách hàng", "enabled", true);
        assertThat(roundTrip(settings)).isEqualTo(settings);
    }

    private Object roundTrip(Object value) {
        return serialization.read(serialization.write(value));
    }

    private ChatBotRuleResponse rule() {
        var timestamp = LocalDateTime.of(2026, 9, 29, 12, 0);
        return new ChatBotRuleResponse(1, "Bảo hành", "bảo hành", MatchType.CONTAINS,
                "Thông tin bảo hành", List.of("Gặp nhân viên"), RuleActionType.REPLY,
                1, true, timestamp, timestamp);
    }
}
