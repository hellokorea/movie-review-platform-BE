package com.cookie.domain.reward.repository;

import com.cookie.domain.reward.entity.RewardHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RewardHistoryRepository extends JpaRepository<RewardHistory, Long> {

    @Query("""
    SELECT SUM(rh.actionPoint)
    FROM RewardHistory rh
    WHERE rh.user.id = :userId
    """)
    Long findTotalBadgePointsByUser(@Param("userId") Long userId);


    @Query("""
    SELECT rh
    FROM RewardHistory rh
    WHERE rh.user.id = :userId
    AND rh.createdAt BETWEEN :startDate AND :endDate
    ORDER BY rh.createdAt DESC
    """)
    List<RewardHistory> findBadgePointHistories(@Param("userId") Long userId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Modifying
    @Query("""
    DELETE FROM RewardHistory r
    WHERE r.user.id = :userId
    """)
    void deleteByUserId(@Param("userId") Long userId);

}
