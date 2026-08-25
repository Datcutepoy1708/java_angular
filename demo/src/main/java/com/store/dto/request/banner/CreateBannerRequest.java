package com.store.dto.request.banner;

import com.store.entity.banner.BannerPosition;
import com.store.entity.banner.BannerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBannerRequest {

    @Size(max = 200, message = "Tiêu đề banner tối đa 200 ký tự")
    private String title;

    @NotBlank(message = "Đường dẫn ảnh banner không được để trống")
    @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
    private String imageUrl;

    @Size(max = 500, message = "Đường dẫn liên kết tối đa 500 ký tự")
    private String linkUrl;

    @NotNull(message = "Vị trí banner không được để trống")
    private BannerPosition position;

    @Builder.Default
    private Integer sortOrder = 0;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Builder.Default
    private BannerStatus status = BannerStatus.ACTIVE;
}
