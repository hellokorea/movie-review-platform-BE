package com.cookie.domain.notification.dto.request;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String body;
    private String senderProfileImage;
    private LocalDateTime timestamp;
}
