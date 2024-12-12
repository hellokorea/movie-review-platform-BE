package com.cookie.domain.notification.dto.request;


import com.cookie.domain.notification.entity.enums.Status;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class NotificationRequest {
    private String notificationId;
    private String body;
    private String senderProfileImage;
    private LocalDateTime timestamp;
    private Long reviewId;
    @Setter
    private Status status;

    @Builder
    public NotificationRequest(String notificationId, String body, String senderProfileImage, LocalDateTime timestamp, Long reviewId, Status status) {
        this.notificationId = notificationId;
        this.body = body;
        this.senderProfileImage = senderProfileImage;
        this.timestamp = timestamp;
        this.reviewId = reviewId;
        this.status = status;
    }
}
