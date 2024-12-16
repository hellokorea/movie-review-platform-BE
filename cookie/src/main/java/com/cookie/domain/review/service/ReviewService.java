package com.cookie.domain.review.service;


import com.cookie.domain.movie.dto.response.ReviewMovieResponse;
import com.cookie.domain.movie.dto.response.ReviewOfMovieResponse;
import com.cookie.domain.movie.entity.Movie;
import com.cookie.domain.movie.repository.MovieRepository;
import com.cookie.domain.notification.entity.FcmToken;
import com.cookie.domain.notification.service.NotificationService;
import com.cookie.domain.review.dto.request.ReviewCommentRequest;
import com.cookie.domain.review.dto.request.CreateReviewRequest;
import com.cookie.domain.review.dto.response.PushNotification;
import com.cookie.domain.review.dto.response.ReviewCommentResponse;
import com.cookie.domain.review.dto.response.ReviewDetailResponse;
import com.cookie.domain.review.dto.response.ReviewListResponse;
import com.cookie.domain.review.dto.response.ReviewResponse;
import com.cookie.domain.review.dto.response.*;
import com.cookie.domain.review.dto.request.UpdateReviewRequest;
import com.cookie.domain.review.entity.Review;
import com.cookie.domain.review.entity.ReviewComment;
import com.cookie.domain.review.entity.ReviewLike;
import com.cookie.domain.review.repository.ReviewCommentRepository;
import com.cookie.domain.review.repository.ReviewLikeRepository;
import com.cookie.domain.review.repository.ReviewRepository;
import com.cookie.domain.reward.service.RewardPointService;
import com.cookie.domain.user.dto.response.CommentUserResponse;
import com.cookie.domain.user.dto.response.ReviewUserResponse;
import com.cookie.domain.user.entity.DailyGenreScore;
import com.cookie.domain.user.entity.User;
import com.cookie.domain.user.entity.enums.ActionType;
import com.cookie.domain.user.repository.DailyGenreScoreRepository;
import com.cookie.domain.user.repository.UserRepository;
import com.cookie.domain.user.service.DailyGenreScoreService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final DailyGenreScoreRepository dailyGenreScoreRepository;
    private final DailyGenreScoreService dailyGenreScoreService;
    private final NotificationService notificationService;
    private final RewardPointService rewardPointService;

    @Transactional
    public void createReview(Long userId, CreateReviewRequest createReviewRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("not found userId: " + userId));

        Long movieId = createReviewRequest.getMovieId();
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("not found movieId: " + movieId));

        if (reviewRepository.findByUserAndMovie(user, movie).isPresent()) {
            throw new IllegalArgumentException("해당 영화에 이미 리뷰를 등록했습니다.");
        }

        // 영화 평점이 0.0일 경우 평점 반영
        if (movie.getScore() == 0.0) {
            movie.updateScore((double) createReviewRequest.getMovieScore());
        }

        // 해당 영화 장르 목록 조회
        List<String> genres = movie.getMovieCategories().stream()
                .filter(mc -> "장르".equals(mc.getCategory().getMainCategory()))
                .map(mc -> mc.getCategory().getSubCategory())
                .toList();

        // 영화 점수에 따라 dailyGenreScore 부여 점수 설정
        int movieScore = createReviewRequest.getMovieScore();
        int dailyGenreScore = calculateGenreScore(movieScore);

        genres.forEach(genre -> dailyGenreScoreService.saveScore(user, genre, dailyGenreScore, ActionType.MOVIE_LIKE));

        Review review = createReviewRequest.toEntity(user, movie);
        Review savedReview = reviewRepository.save(review); // DB에 저장 된 리뷰

        // 영화 장르 목록 순회하면서 해당 장르를 좋아하는 유저의 토큰 불러와서 푸쉬알림 전송하기
        for (String genre : genres) {
            List<String> recipientTokens = userRepository.findTokensByGenreAndExcludeUser(genre, userId); // 알림을 받는 사람들의 토큰 목록 (작성자 제외)

            String title ="Cookie 🍪";
            String body = String.format("%s님이 %s 영화에 리뷰를 등록했어요!.", user.getNickname(), movie.getTitle()); // 리뷰 작성자 닉네임, 영화 제목
            notificationService.sendPushNotificationToUsers(userId, recipientTokens, title, body, savedReview.getId());
        }

        rewardPointService.updateBadgePointAndBadgeObtain(user, "review", movie.getTitle());
    }

    private int calculateGenreScore(int movieScore) {
        if (movieScore == 5) {
            return 8;
        } else if (movieScore == 4) {
            return 7;
        } else if (movieScore == 3) {
            return 5;
        } else if (movieScore <= 2) {
            return 0;
        } else {
            return 0; // 기본값
        }
    }


    @Transactional
    public void updateReview(Long reviewId, UpdateReviewRequest updateReviewRequest) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("not found reviewId: " + reviewId));
        log.info("Retrieved review: reviewId = {}", reviewId);

        review.update(updateReviewRequest.getContent(), updateReviewRequest.getMovieScore(), updateReviewRequest.getIsSpoiler());
        log.info("Updated review: reviewId = {}", reviewId);
    }

    @Transactional(readOnly = true)
    public ReviewListResponse getReviewList(Long userId, Pageable pageable) {
        Page<Review> reviewList = reviewRepository.findAllWithMovieAndUser(pageable);
        log.info("Total reviews: {}", reviewList.getTotalElements());

        List<ReviewResponse> reviewResponses = reviewList.stream()
                .map(review -> {
                    boolean likedByUser = userId != null &&
                            review.getReviewLikes().stream()
                                    .anyMatch(like -> like.getUser().getId().equals(userId));
                    return ReviewResponse.fromReview(review, likedByUser,Long.valueOf(review.getReviewComments().size()));
                })
                .toList();

        return new ReviewListResponse(
                reviewResponses,
                reviewList.getTotalElements(),
                reviewList.getTotalPages()
        );

    }

    @Transactional(readOnly = true)
    public ReviewListResponse getSpoilerReviewList(Long userId, Pageable pageable) {
        Page<Review> reviewList = reviewRepository.findAllWithMovieAndUserWithSpoilers(pageable);
        log.info("Total reviews: {}", reviewList.getTotalElements());

        List<ReviewResponse> reviewResponses = reviewList.stream()
                .map(review -> {
                    boolean likedByUser = userId != null &&
                            review.getReviewLikes().stream()
                                    .anyMatch(like -> like.getUser().getId().equals(userId));
                    return ReviewResponse.fromReview(review, likedByUser, Long.valueOf(review.getReviewComments().size()));
                })
                .toList();

        return new ReviewListResponse(
                reviewResponses,
                reviewList.getTotalElements(),
                reviewList.getTotalPages()
        );
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("not found reviewId: " + reviewId));
        log.info("Retrieved review: reviewId = {}", reviewId);

        reviewRepository.delete(review);
        log.info("Deleted review: reviewId = {}", reviewId);
    }

    @Transactional(readOnly = true)
    public ReviewDetailResponse getReviewDetail(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("not found reviewId: " + reviewId));
        log.info("Retrieved review: reviewId = {}", reviewId);

        List<ReviewComment> reviewComments = reviewCommentRepository.findCommentsWithUserByReviewId(reviewId);

        List<ReviewCommentResponse> comments = reviewComments.stream()
                .map(comment -> new ReviewCommentResponse(
                        comment.getId(),
                        new CommentUserResponse(
                                comment.getUser().getId(),
                                comment.getUser().getNickname(),
                                comment.getUser().getProfileImage()),
                        comment.getCreatedAt(),
                        comment.getComment()))
                .toList();

        boolean likedByUser = userId != null && reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);

        return new ReviewDetailResponse(
                review.getContent(),
                review.getMovieScore(),
                review.getReviewLike(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                new ReviewMovieResponse(review.getMovie().getId(), review.getMovie().getTitle(), review.getMovie().getPoster(), review.getMovie().getScore()),
                new ReviewUserResponse(
                        review.getUser().getId(),
                        review.getUser().getNickname(),
                        review.getUser().getProfileImage(),
                        review.getUser().getMainBadge() != null ? review.getUser().getMainBadge().getBadgeImage() : null),

                comments,
                likedByUser
        );

    }

    @Transactional
    public void createComment(Long reviewId, Long userId, ReviewCommentRequest reviewCommentRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("not found userId: " + userId));
        log.info("Retrieved user: userId = {}", userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("not found reviewId: " + reviewId));
        log.info("Retrieved review: reviewId = {}", reviewId);

        ReviewComment comment = ReviewComment.builder()
                .user(user)
                .review(review)
                .comment(reviewCommentRequest.getComment())
                .build();

        reviewCommentRepository.save(comment);
        log.info("Created comment for reviewId: {} by userId: {}", reviewId, userId);

    }

    @Transactional
    public void updateComment(Long commentId, ReviewCommentRequest reviewCommentRequest) {
        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("not found commentId: " + commentId));
        log.info("Retrieved comment: commentId = {}", commentId);

        comment.update(reviewCommentRequest.getComment());
        log.info("Updated comment: commentId = {}", commentId);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("not found commentId: " + commentId));
        log.info("Retrieved comment: commentId = {}", commentId);

        reviewCommentRepository.delete(comment);
        log.info("Deleted comment: commentId = {}", commentId);
    }

    @Transactional(readOnly = true)
    public ReviewPagenationResponse getLikedReviewsByUserId(Long userId, int page, int size) {
        // 페이징 요청 생성
        Pageable pageable = PageRequest.of(page, size);

        // ReviewLike 엔티티를 페이징 처리하여 조회
        Page<ReviewLike> likedReviewsPage = reviewLikeRepository.findAllByUserIdWithReviews(userId, pageable);

        // ReviewLike -> ReviewResponse 변환
        List<ReviewResponse> reviews = likedReviewsPage.getContent().stream()
                .map(reviewLike -> {
                    Review review = reviewLike.getReview();
                    return ReviewResponse.fromReview(review, true,Long.valueOf(review.getReviewComments().size()));
                })
                .toList();

        // ReviewPagenationResponse 생성
        return ReviewPagenationResponse.builder()
                .currentPage(page)
                .reviews(reviews)
                .totalPages(likedReviewsPage.getTotalPages())
                .build();
    }

}

