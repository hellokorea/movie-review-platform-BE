package com.cookie.domain.user.repository;

import com.cookie.domain.user.entity.BadgeAccumulationPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BadgeAccumulationPointRepository extends JpaRepository<BadgeAccumulationPoint, Long> {
    @Query("SELECT b FROM BadgeAccumulationPoint b WHERE b.user.id = :userId")
    BadgeAccumulationPoint findByUserId(@Param("userId") Long userId);

    @Query("""
    SELECT b.accPoint
    FROM BadgeAccumulationPoint b
    WHERE b.user.id = :userId
    """)
    Long findPointByUser(@Param("userId") Long userId);
}
