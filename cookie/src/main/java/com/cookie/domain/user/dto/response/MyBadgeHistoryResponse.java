package com.cookie.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyBadgeHistoryResponse {

    private String movieName;
    private String actionName;
    private Long point;
    private String createdAt;
}
