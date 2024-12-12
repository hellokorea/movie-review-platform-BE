package com.cookie.domain.user.repository;

import com.cookie.domain.user.entity.MatchUpParticipation;
import com.cookie.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchUpParticipationRepository extends JpaRepository<MatchUpParticipation, Long> {
    boolean existsByUserIdAndMatchUpMovie_Id(Long userId, Long matchUpMovieId);

    @Query("""
    SELECT mup.user
    FROM MatchUpParticipation mup
    WHERE mup.matchUpMovie.id = :winnerMovieId
    """)
    List<User> findWinnerUsers(@Param("winnerMovieId") Long winnerMovieId);
}
