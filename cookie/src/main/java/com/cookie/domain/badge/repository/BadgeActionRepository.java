package com.cookie.domain.badge.repository;

import com.cookie.domain.badge.entity.BadgeAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BadgeActionRepository extends JpaRepository<BadgeAction, Long> {

    @Query("""
    SELECT ba
    FROM BadgeAction ba
    WHERE ba.name = :actionName
    """)
    Optional<BadgeAction> findBadgeActionByName(@Param("actionName") String actionName);
}
