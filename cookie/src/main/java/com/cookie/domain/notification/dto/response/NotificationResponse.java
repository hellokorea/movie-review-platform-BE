package com.cookie.domain.notification.dto.response;

import com.cookie.domain.notification.entity.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String notificationId;
    private String body;
    private String senderProfileImage;
    private LocalDateTime timestamp;
    private Long reviewId;
    private Status status;

}
