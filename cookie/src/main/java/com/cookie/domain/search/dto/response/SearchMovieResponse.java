package com.cookie.domain.search.dto.response;

import com.cookie.domain.movie.entity.Movie;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SearchMovieResponse {
    private Long id;
    private String poster;
    private String title;

    public static SearchMovieResponse fromMovie(Movie movie) {

        return new SearchMovieResponse(
                movie.getId(),
                movie.getPoster(),
                movie.getTitle()
        );
    }
}
