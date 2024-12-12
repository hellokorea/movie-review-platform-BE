package com.cookie.domain.matchup.dto.response;

import com.cookie.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchUpCloseResponse {
    private User user;
    private String movieName;
}
