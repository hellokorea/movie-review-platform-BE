package com.cookie.domain.movie.repository;

import com.cookie.domain.movie.entity.MovieImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieImageRepository extends JpaRepository<MovieImage, Long> {

    @Modifying
    @Query("""
        DELETE FROM MovieImage  ma
        WHERE ma.movie.id = :movieId
    """)
    void deleteByMovieId(@Param("movieId") Long movieId);

    @Query("SELECT mi.url FROM MovieImage mi WHERE mi.movie.id = :movieId")
    List<String> findImageUrlsByMovieId(@Param("movieId") Long movieId);

    @Query("""
    SELECT mi.url
    FROM MovieImage mi
    """)
    List<String> findAllTMDBImages();

    @Modifying
    @Query("""
        UPDATE MovieImage mi
        SET mi.url = :cloudFrontUrl
        WHERE mi.url = :TmdbBUrl
    """)
    void updateImageByFileName(@Param("TmdbBUrl") String TmdbBUrl, @Param("cloudFrontUrl") String cloudFrontUrl);
}
