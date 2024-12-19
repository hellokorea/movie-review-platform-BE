package com.cookie.domain.review.service;


import com.cookie.domain.movie.dto.response.ReviewMovieResponse;
import com.cookie.domain.movie.entity.Movie;
import com.cookie.domain.movie.repository.MovieRepository;
import com.cookie.domain.notification.service.NotificationService;
import com.cookie.domain.review.dto.request.ReviewCommentRequest;
import com.cookie.domain.review.dto.request.CreateReviewRequest;
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
import com.cookie.domain.user.entity.User;
import com.cookie.domain.user.entity.enums.ActionType;
import com.cookie.domain.user.repository.DailyGenreScoreRepository;
import com.cookie.domain.user.repository.UserRepository;
import com.cookie.domain.user.service.DailyGenreScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
    public CreateReviewResponse createReview(Long userId, CreateReviewRequest createReviewRequest) {
        // 1. User 조회
        log.info("리뷰 작성 요청: userId = {}, movieId = {}", userId, createReviewRequest.getMovieId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자 조회 실패: userId = {}", userId);
                    return new IllegalArgumentException("not found userId: " + userId);
                });
        log.info("사용자 조회 성공: userId = {}, nickname = {}", userId, user.getNickname());

        Long movieId = createReviewRequest.getMovieId();

        // 2. Movie 조회
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> {
                    log.error("영화 조회 실패: movieId = {}", movieId);
                    return new IllegalArgumentException("not found movieId: " + movieId);
                });
        log.info("영화 조회 성공: movieId = {}, title = {}", movieId, movie.getTitle());

        // 3. 이미 리뷰 작성 여부 확인
        if (reviewRepository.findByUserAndMovie(user, movie).isPresent()) {
            log.error("중복 리뷰 작성 시도: userId = {}, movieId = {}", userId, movieId);
            throw new IllegalArgumentException("해당 영화에 이미 리뷰를 등록했습니다.");
        }
        log.info("중복 리뷰 없음: userId = {}, movieId = {}", userId, movieId);

        // 4. 영화 평점이 0일 경우 평점 반영
        if (movie.getScore() == 0.0) {
            log.info("영화 평점이 0.0입니다. 평점을 업데이트합니다: movieScore = {}", createReviewRequest.getMovieScore());
            movie.updateScore((double) createReviewRequest.getMovieScore());
        }

        // 5. 영화 장르 목록 조회
        List<String> genres = movie.getMovieCategories().stream()
                .filter(mc -> "장르".equals(mc.getCategory().getMainCategory()))
                .map(mc -> mc.getCategory().getSubCategory())
                .toList();
        log.info("영화 장르 목록 조회 완료: genres = {}", genres);

        // 6. 영화 점수에 따른 dailyGenreScore 계산 및 저장
        int movieScore = createReviewRequest.getMovieScore();
        int dailyGenreScore = calculateGenreScore(movieScore);
        log.info("영화 점수에 따른 dailyGenreScore 계산 완료: movieScore = {}, dailyGenreScore = {}", movieScore, dailyGenreScore);

        genres.forEach(genre -> {
            log.info("장르별 dailyGenreScore 저장: genre = {}, dailyGenreScore = {}", genre, dailyGenreScore);
            dailyGenreScoreService.saveScore(user, genre, dailyGenreScore, ActionType.MOVIE_LIKE);
        });

        // 7. 리뷰 저장
        Review review = createReviewRequest.toEntity(user, movie);
        log.info("리뷰 엔티티 생성 완료: review = {}", review);
        Review savedReview = reviewRepository.save(review);
        log.info("리뷰 저장 완료: savedReviewId = {}", savedReview.getId());

        // 8. 장르에 맞는 유저들에게 푸시 알림 전송
        for (String genre : genres) {
//            List<String> recipientTokens = userRepository.findTokensByGenreAndExcludeUser(genre, userId);

            List<Object[]> results = userRepository.findTokensByGenreAndExcludeUser(genre, userId);
            Map<Long, String> recipientTokenMap = results.stream()
                    .collect(Collectors.toMap(result -> (Long) result[0], result -> (String) result[1]));

            log.info("푸시 알림 대상 조회: genre = {}, tokens = {}", genre, recipientTokenMap.keySet());

            String title = "Cookie 🍪";
            String body = String.format("%s님이 %s 영화에 리뷰를 등록했어요!.", user.getNickname(), movie.getTitle());
            log.info("푸시 알림 전송: title = {}, body = {}", title, body);
            notificationService.sendPushNotificationToUsers(userId, recipientTokenMap, title, body, savedReview.getId());
        }

        // 9. 리워드 포인트 및 배지 업데이트
        log.info("리워드 포인트 및 배지 업데이트 시작: userId = {}, movieTitle = {}", userId, movie.getTitle());
        rewardPointService.updateBadgePointAndBadgeObtain(user, "review", movie.getTitle());
        log.info("리워드 포인트 및 배지 업데이트 완료: userId = {}", userId);

        return new CreateReviewResponse(savedReview.getId());
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
                                comment.getUser().getProfileImage(),
                                comment.getUser().getMainBadge() != null ? review.getUser().getMainBadge().getBadgeImage() : null),
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

