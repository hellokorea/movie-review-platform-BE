package com.cookie.domain.actor.repository;

import com.cookie.domain.actor.dto.response.ActorResponse;
import com.cookie.domain.actor.entity.Actor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActorRepository extends JpaRepository<Actor, Long> {

    @Query("""
            SELECT a
            FROM Actor a
            WHERE a.tmdbCasterId =:tmdbCasterId
           """)
    Optional<Actor> findByTMDBCasterId(@Param("tmdbCasterId") Long tmdbCasterId);

    @Query("""
            SELECT a
            FROM Actor a
            WHERE a.tmdbCasterId IN :actorIds
           """)
    List<Actor> findAllByTmdbCasterIdIn(@Param("actorIds") List<Long> actorIds);

    @Query("SELECT new com.cookie.domain.actor.dto.response.ActorResponse("
            + "a.id,a.name, a.profileImage"
            + ") "
            + "FROM Actor a "
            + "JOIN MovieActor ma ON ma.actor.id = a.id "
            + "WHERE ma.movie.id = :movieId")
    List<ActorResponse> findActorsByMovieId(@Param("movieId") Long movieId);


    @Query("""
        SELECT a.profileImage
        FROM Actor a
    """)
    List<String> findAllTMDBImages();

    @Modifying
    @Query("""
        UPDATE Actor a
        SET a.profileImage = :cloudFrontUrl
        WHERE a.profileImage = :TmdbBUrl
    """)
    void updateImageByFileName(@Param("TmdbBUrl") String TmdbBUrl, @Param("cloudFrontUrl") String cloudFrontUrl);
}


