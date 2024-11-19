package com.cookie.domain.review.dto.response;

import com.cookie.domain.movie.dto.response.ReviewMovieResponse;
import com.cookie.domain.review.entity.Review;
import com.cookie.domain.user.dto.response.ReviewUserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long reviewId;
    private String content;
    private double movieScore;
    private boolean isHide;
    private boolean isSpoiler;
    private long reviewLike;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ReviewMovieResponse movie;
    private ReviewUserResponse user;

    public static ReviewResponse fromEntity(Review review) {
        ReviewMovieResponse reviewMovie = new ReviewMovieResponse(
                review.getMovie().getPoster(),
                review.getMovie().getTitle()
        );

        ReviewUserResponse reviewUser = new ReviewUserResponse(
                review.getUser().getNickname(),
                review.getUser().getProfileImage()
        );

        return new ReviewResponse(
                review.getId(),
                review.getContent(),
                review.getMovieScore(),
                review.isHide(),
                review.isSpoiler(),
                review.getReviewLike(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                reviewMovie,
                reviewUser
        );
    }

}
