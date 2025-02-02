package com.cookie.domain.movie.repository;

import com.cookie.domain.movie.entity.MovieActor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieActorRepository extends JpaRepository<MovieActor, Long> {
    @Query("SELECT ma FROM MovieActor ma JOIN FETCH ma.movie m WHERE ma.actor.id = :actorId ORDER BY m.releasedAt DESC")
    List<MovieActor> findAllMoviesByActorId(Long actorId);

    @Query("""
        SELECT ma
        FROM MovieActor ma
        WHERE ma.movie.id = :movieId
    """)
    List<MovieActor> findMovieActorsByMovieId(@Param("movieId") Long movieId);

    @Modifying
    @Query("""
        DELETE FROM MovieActor  ma
        WHERE ma.movie.id = :movieId
    """)
    void deleteByMovieId(@Param("movieId") Long movieId);
}
