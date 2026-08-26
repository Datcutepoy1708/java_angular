package com.store.dto.returnrefund;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnReviewRequest {

    @NotNull(message = "Trạng thái phê duyệt không được để trống")
    private Boolean approved; // true = APPROVED, false = REJECTED

    private String adminNote;
}
