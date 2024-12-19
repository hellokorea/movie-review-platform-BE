package com.cookie.domain.review.controller;


import com.cookie.domain.review.dto.request.CreateReviewRequest;
import com.cookie.domain.review.dto.request.ReviewCommentRequest;
import com.cookie.domain.review.dto.request.UpdateReviewRequest;
import com.cookie.domain.review.dto.response.CreateReviewResponse;
import com.cookie.domain.review.dto.response.ReviewDetailResponse;
import com.cookie.domain.review.dto.response.ReviewListResponse;
import com.cookie.domain.review.service.ReviewService;
import com.cookie.domain.user.dto.response.auth.CustomOAuth2User;
import com.cookie.global.util.ApiUtil;
import com.cookie.global.util.ApiUtil.ApiSuccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "리뷰", description = "리뷰 API")
@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @Operation(summary = "리뷰 생성", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(type = "string", example = "SUCCESS")))
    })
    @PostMapping
    public ApiSuccess<?> createReview(@AuthenticationPrincipal CustomOAuth2User customOAuth2User, @RequestBody CreateReviewRequest createReviewRequest) {
        Long userId = customOAuth2User.getId();
        CreateReviewResponse createReviewResponse = reviewService.createReview(userId, createReviewRequest);
        return ApiUtil.success(createReviewResponse);
    }

    @Operation(summary = "리뷰 수정",
            parameters = {
            @Parameter(name = "UpdateReviewRequest",
                    schema = @Schema(implementation = UpdateReviewRequest.class, nullable = true))
            },
            responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(type = "string", example = "SUCCESS")))
    })
    @PutMapping("/{reviewId}")
    public ApiSuccess<?> updateReview(@PathVariable(name = "reviewId") Long reviewId, @RequestBody UpdateReviewRequest updateReviewRequest) {
        reviewService.updateReview(reviewId, updateReviewRequest);
        return ApiUtil.success("SUCCESS");
    }

    @Operation(summary = "영화 리뷰 리스트", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ReviewListResponse.class)))
    })
    @GetMapping
    public ApiSuccess<?> getReviewList(@AuthenticationPrincipal CustomOAuth2User customOAuth2User, Pageable pageable) {
        Long userId = (customOAuth2User != null) ? customOAuth2User.getId() : null;
        log.info("getReviewList() userId: {}", userId);
        ReviewListResponse reviewList = reviewService.getReviewList(userId, pageable);
        return ApiUtil.success(reviewList);
    }

    @Operation(summary = "영화 스포일러 리뷰 리스트", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ReviewListResponse.class)))
    })
    @GetMapping("/spoiler")
    public ApiSuccess<?> getSpoilerReviewList(@AuthenticationPrincipal CustomOAuth2User customOAuth2User,Pageable pageable) {
        Long userId = (customOAuth2User != null) ? customOAuth2User.getId() : null;
        log.info("getSpoilerReviewList() userId: {}", userId);
        ReviewListResponse reviewList = reviewService.getSpoilerReviewList(userId, pageable);
        return ApiUtil.success(reviewList);
    }

    @Operation(summary = "리뷰 삭제", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(type = "string", example = "SUCCESS")))
    })
    @DeleteMapping("/{reviewId}")
    public ApiSuccess<?> deleteReview(@PathVariable(name = "reviewId") Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ApiUtil.success("SUCCESS");
    }

    @Operation(summary = "영화 리뷰 상세 정보", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ReviewDetailResponse.class)))
    })
    @GetMapping("/{reviewId}")
    public ApiSuccess<?> getReviewDetail(@AuthenticationPrincipal CustomOAuth2User customOAuth2User, @PathVariable(name = "reviewId") Long reviewId) {
        Long userId = (customOAuth2User != null) ? customOAuth2User.getId() : null;
        ReviewDetailResponse reviewDetailResponse = reviewService.getReviewDetail(reviewId, userId);
        return ApiUtil.success(reviewDetailResponse);
    }

    @Operation(summary = "리뷰 댓글 생성", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(type = "string", example = "SUCCESS")))
    })
    @PostMapping("/{reviewId}/comments")
    public ApiSuccess<?> createComment(@PathVariable(name = "reviewId") Long reviewId, @AuthenticationPrincipal CustomOAuth2User customOAuth2User, @RequestBody ReviewCommentRequest reviewCommentRequest) {
        Long userId = customOAuth2User.getId();
        reviewService.createComment(reviewId, userId, reviewCommentRequest);
        return ApiUtil.success("SUCCESS");
    }

    @Operation(summary = "리뷰 댓글 수정",
            parameters = {
            @Parameter(name = "ReviewCommentRequest",
                            schema = @Schema(implementation = ReviewCommentRequest.class, nullable = true))
            },
            responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "string", example = "SUCCESS")))
            })
    @PutMapping("/comments/{commentId}")
    public ApiSuccess<?> updateComment(@PathVariable(name = "commentId") Long commentId, @RequestBody ReviewCommentRequest reviewCommentRequest) {
        reviewService.updateComment(commentId, reviewCommentRequest);
        return ApiUtil.success("SUCCESS");
    }

    @Operation(summary = "리뷰 댓글 삭제", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(type = "string", example = "SUCCESS")))
    })
    @DeleteMapping("/comments/{commentId}")
    public ApiSuccess<?> deleteComment(@PathVariable(name = "commentId") Long commentId) {
        reviewService.deleteComment(commentId);
        return ApiUtil.success("SUCCESS");
    }

}
