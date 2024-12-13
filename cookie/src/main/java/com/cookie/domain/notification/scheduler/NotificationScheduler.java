package com.cookie.domain.notification.scheduler;

import com.cookie.domain.notification.dto.request.NotificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {
    private final RedisTemplate<String, Object> template;
    private final ObjectMapper objectMapper;

    private static final String NOTIFICATION_KEY = "notifications:user:";

    @Async
    @Scheduled(cron = "0 0 4 * * *")
    public void syncNotificationsRedisToRds() {
        Set<String> keys = template.keys(NOTIFICATION_KEY + "*");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (String key : keys) {
            List<Object> notifications = template.opsForList().range(key, 0, -1);

            if (notifications == null || notifications.isEmpty()) {
                continue;
            }

            for (Object notification : notifications) {
                try {
                    String jsonString = notification.toString();
                    NotificationRequest notificationRequest = objectMapper.readValue(jsonString, NotificationRequest.class);

                    if (notificationRequest.getTimestamp() == null) {
                        log.warn("Notification with null timestamp: {}", notificationRequest);
                        continue;
                    }

                    LocalDateTime notificationTimestamp = notificationRequest.getTimestamp();

                    if (notificationTimestamp.isBefore(now.minusDays(30))) {
                        template.opsForList().remove(key, 1, jsonString);
                        log.info("만료된 알림을 삭제했습니다.: {}", key);
                    }

                } catch (Exception e) {
                    log.error("Error parsing notification JSON: {}", notification, e);
                }
            }
        }
    }
}