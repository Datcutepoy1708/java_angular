package com.store.dto.response.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingSummaryResponse {

    private Long productId;
    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> ratingCounts;
    private Map<Integer, Double> starPercentages;
}
