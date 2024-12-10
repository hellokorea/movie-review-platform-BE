package com.cookie.domain.reward.service;

import com.cookie.domain.badge.entity.Badge;
import com.cookie.domain.badge.entity.BadgeAction;
import com.cookie.domain.badge.repository.BadgeActionRepository;
import com.cookie.domain.badge.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardCacheService {

    private final BadgeActionRepository badgeActionRepository;
    private final BadgeRepository badgeRepository;

    @Cacheable(value = "pointLivedCache", key = "'action:' + #actionName", cacheManager = "pointLivedCacheManager")
    public Long getActionPointByCache(String actionName) {
        log.info("최초 DB 에서 조회 -> actionId: {} from DB", actionName);
        return badgeActionRepository.findBadgeActionByName(actionName)
                .map(BadgeAction::getActionPoint)
                .orElseThrow(() -> new IllegalArgumentException("해당 action name 이 존재하지 않습니다."));
    }

    @Cacheable(value = "pointLivedCache", key = "'badge:' + #grade", cacheManager = "pointLivedCacheManager")
    public Long getBadgeNeedPointByGrade(String grade) {
        log.info("최초 DB 에서 조회 -> badge: {} from DB", grade);
        return badgeRepository.findBadgeByGrade(grade)
                .map(Badge::getNeedPoint)
                .orElseThrow(() -> new IllegalArgumentException("해당 뱃지 등급이 존재하지 않습니다."));
    }
}
