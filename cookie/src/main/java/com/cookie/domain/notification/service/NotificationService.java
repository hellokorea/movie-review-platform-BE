package com.cookie.domain.notification.service;


import com.cookie.domain.notification.dto.request.NotificationRequest;
import com.cookie.domain.notification.dto.response.NotificationResponse;
import com.cookie.domain.notification.entity.FcmToken;
import com.cookie.domain.notification.entity.enums.Status;
import com.cookie.domain.notification.repository.FcmTokenRepository;
import com.cookie.domain.user.entity.User;
import com.cookie.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FirebaseMessaging firebaseMessaging;
    private final RedisTemplate<String, Object> template;
    private final ObjectMapper objectMapper;
    private static final String NOTIFICATION_KEY = "notifications:user:";

    /**
     * 푸쉬알림 전송
     */
    @Async
    public void sendPushNotificationToUsers(Long senderId, List<String> tokens, String title, String body, Long reviewId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("not found userId"));
        String senderProfileImage = sender.getProfileImage();

        for (String token : tokens) {

            FcmToken fcmToken = fcmTokenRepository.findByToken(token)
                    .orElseThrow(() -> new IllegalArgumentException("not found token"));
            Long userId = fcmToken.getUser().getId(); // 받는 사람 id


            Message message = Message.builder()
                    .setWebpushConfig(WebpushConfig.builder()
                            .putHeader("Urgency", "high")
                            .build())
                    .putData("title", title)
                    .putData("body", body)
                    .setToken(token)
                    .build();

            try {
                firebaseMessaging.send(message); // 푸쉬 알림 전송
                log.info("푸쉬 알림 전송 성공: token {}", token);
                saveUserNotificationToRedis(userId, body, senderProfileImage, reviewId); // 알림 저장
            } catch (FirebaseMessagingException e) {
                log.error("푸쉬 알림 전송 실패: token {}, error: {}", token, e.getMessage());
            }
        }
    }


    /*
     * 사용자 별 알림 리스트 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        String key = NOTIFICATION_KEY + userId;
        List<Object> notifications = template.opsForList().range(key, 0, -1);

        if (notifications == null) {
            log.warn("알림이 존재하지 않습니다. userId: {}", userId);
            return new ArrayList<>();
        }

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<NotificationResponse> result = new ArrayList<>();

        for (Object notification : notifications) {
            try {
                NotificationResponse notificationResponse = objectMapper.readValue(notification.toString(), NotificationResponse.class);
                result.add(notificationResponse);
            } catch (Exception e) {
                log.error("Error parsing notification JSON: {}", notification, e);
            }
        }

        return result;
    }


    /*
     * redis 알림 저장
     */
    @Async
    public void saveUserNotificationToRedis(Long userId, String body, String senderProfileImage, Long reviewId) {
        String key = NOTIFICATION_KEY + userId;

        NotificationRequest notificationRequest = NotificationRequest.builder()
                .notificationId(UUID.randomUUID().toString())
                .body(body)
                .senderProfileImage(senderProfileImage)
                .timestamp(LocalDateTime.now())
                .reviewId(reviewId)
                .status(Status.UNREAD)
                .build();

        try {
            template.opsForList().leftPush(key, objectMapper.writeValueAsString(notificationRequest));
            log.info("알림 저장 성공 userId: {}", userId);
        } catch (Exception e) {
            log.error("Error saving notification to Redis: {}", e.getMessage());
        }
    }


    /*
     * 읽음 처리 갱신
     */
    public void markNotificationAsRead(Long userId, String notificationId) {
        String key = NOTIFICATION_KEY + userId;

        List<Object> notifications = template.opsForList().range(key, 0, -1);
        if (notifications == null || notifications.isEmpty()) {
            log.warn("알림이 존재하지 않습니다. userId: {}", userId);
            return;
        }
            Message message = Message.builder()
                    .setWebpushConfig(WebpushConfig.builder()
                            .putHeader("Urgency", "high")
                            .build())
                    .putData("title", title)
                    .putData("body", body)
                    .setToken(token)
                    .build();

        for (int i = 0; i < notifications.size(); i++) {
            try {
                NotificationRequest notificationRequest = objectMapper.readValue(notifications.get(i).toString(), NotificationRequest.class);

                if (notificationRequest.getNotificationId().equals(notificationId)) {
                    notificationRequest.setStatus(Status.READ);
                    template.opsForList().set(key, i, objectMapper.writeValueAsString(notificationRequest));
                    log.info("알림 읽음 처리 완료: userId {}, notificationId {}", userId, notificationId);
                    return;
                }
            } catch (Exception e) {
                log.error("Error parsing or updating notification: {}", notifications.get(i), e);
            }
        }

        log.warn("일치하는 알림을 찾을 수 없습니다 userId: {} and notificationId: {}", userId, notificationId);
    }
}

