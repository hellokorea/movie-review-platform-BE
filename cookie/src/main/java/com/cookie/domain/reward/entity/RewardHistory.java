package com.cookie.domain.reward.entity;

import com.cookie.domain.user.entity.User;
import com.cookie.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String movieName;
    private String action;
    private Long actionPoint;

    @Builder
    public RewardHistory(User user, String movieName, String action, Long actionPoint) {
        this.user = user;
        this.movieName = movieName;
        this.action = action;
        this.actionPoint = actionPoint;
    }
}
