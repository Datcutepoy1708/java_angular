package com.store.dto.response.order;

import com.store.entity.order.OrderStatusHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryResponse {

    private Long id;
    private String status;
    private String note;
    private Long changedById;
    private String changedByName;
    private LocalDateTime changedAt;

    public static OrderStatusHistoryResponse fromEntity(OrderStatusHistory history) {
        if (history == null) return null;
        var user = history.getChangedBy();
        return OrderStatusHistoryResponse.builder()
                .id(history.getId())
                .status(history.getStatus())
                .note(history.getNote())
                .changedById(user != null ? user.getUserId() : null)
                .changedByName(user != null ? user.getFullName() : "Hệ thống")
                .changedAt(history.getChangedAt())
                .build();
    }
}
