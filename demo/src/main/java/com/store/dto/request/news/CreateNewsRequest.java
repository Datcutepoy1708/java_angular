package com.store.dto.request.news;

import com.store.entity.news.NewsStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNewsRequest {

    private Integer newsCatId;

    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    @Size(max = 250, message = "Tiêu đề tối đa 250 ký tự")
    private String title;

    private String slug;

    @Size(max = 500, message = "Đường dẫn ảnh đại diện tối đa 500 ký tự")
    private String thumbnailUrl;

    @Size(max = 500, message = "Tóm tắt bài viết tối đa 500 ký tự")
    private String summary;

    @NotBlank(message = "Nội dung bài viết không được để trống")
    private String content;

    @Builder.Default
    private NewsStatus status = NewsStatus.DRAFT;
}
