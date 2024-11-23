package com.cookie.admin.controller;

import com.cookie.admin.dto.response.*;
import com.cookie.admin.service.*;
import com.cookie.domain.movie.entity.Movie;
import com.cookie.global.util.ApiUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final TMDBService TMDBService;
    private final AdminMovieCreateService adminMovieCreateService;
    private final AdminMovieReadService adminMovieReadService;
    private final AdminMovieModifyService adminMovieModifyService;
    private final AdminMovieSearchService adminMovieSearchService;

    @PostMapping("/movie/base")
    public ApiUtil.ApiSuccess<?> defaultMoviesAdd() {
        AdminMovieBaseAddResponse data = adminMovieCreateService.defaultMoviesAdd();
        return ApiUtil.success(data);
    }

    @GetMapping("/movie/tmdb/{movieName}")
    public ApiUtil.ApiSuccess<?> getMoviesByName(@PathVariable("movieName") String movieName) {
        List<AdminSearchResponse> data = TMDBService.getMoviesByName(movieName);
        return ApiUtil.success(data);
    }

    @GetMapping("/movie/tmdb/choice/{movieId}")
    public ApiUtil.ApiSuccess<?> getMovieInfoById(@PathVariable("movieId") Long movieId) {
        AdminMovieDetailResponse data = TMDBService.getMovieInfoById(movieId);
        return ApiUtil.success(data);
    }

    @PostMapping("/movie")
    public ApiUtil.ApiSuccess<?> createMovie(@RequestBody AdminMovieDetailResponse movie) {
        Movie data = adminMovieCreateService.createMovie(movie);
        return ApiUtil.success(data);
    }

    @GetMapping("/movie/{movieId}")
    public ApiUtil.ApiSuccess<?> getMovieCategory(@PathVariable("movieId") Long movieId) {
        AdminMovieCategoryResponse data = adminMovieReadService.getMovieCategory(movieId);
        return ApiUtil.success(data);
    }

    @PutMapping("/movie/{movieId}")
    public ApiUtil.ApiSuccess<?> updateMovieCategory(@PathVariable("movieId") Long movieId,
                                                     @RequestBody List<MovieCategories> categories) {
        AdminMovieCategoryResponse data = adminMovieModifyService.updateMovieCategory(movieId, categories);
        return ApiUtil.success(data);
    }

    @DeleteMapping("/movie/{movieId}")
    public ApiUtil.ApiSuccess<?> deleteMovie(@PathVariable("movieId") Long movieId) {
        AdminMovieDeleteResponse data = adminMovieModifyService.deleteMovie(movieId);
        return ApiUtil.success(data);
    }

    @GetMapping("/movies/{movieName}/{pageNumber}")
    public ApiUtil.ApiSuccess<?> getMovieBySearch(@PathVariable("movieName") String  movieName,
                                                  @PathVariable Integer pageNumber) {
        AdminMoviesResponse data = adminMovieSearchService.getMoviesByName(movieName, pageNumber);
        return ApiUtil.success(data);
    }

    @GetMapping("/movies/{pageNumber}")
    public ApiUtil.ApiSuccess<?> getMovies(@PathVariable Integer pageNumber) {
        AdminMoviesResponse data = adminMovieSearchService.getMovies(pageNumber);
        return ApiUtil.success(data);
    }
}
