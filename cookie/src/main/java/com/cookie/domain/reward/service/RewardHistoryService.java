package com.cookie.domain.reward.service;

import com.cookie.domain.reward.entity.RewardHistory;
import com.cookie.domain.reward.repository.RewardHistoryRepository;
import com.cookie.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RewardHistoryService {

    private final RewardHistoryRepository rewardHistoryRepository;

    public void createRewardHistoryByUser(User user, String actionName, Long actionPoint, String movieName) {

        rewardHistoryRepository.save(RewardHistory.builder()
                .user(user)
                .movieName(movieName)
                .action(actionName)
                .actionPoint(actionPoint)
                .build());
    }
}
