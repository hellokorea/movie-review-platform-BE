package com.cookie.admin.controller;

import com.cookie.admin.dto.response.*;
import com.cookie.admin.service.movie.AdminMovieCreateService;
import com.cookie.admin.service.movie.AdminMovieModifyService;
import com.cookie.admin.service.movie.AdminMovieReadService;
import com.cookie.admin.service.movie.AdminMovieSearchService;
import com.cookie.global.service.AWSS3CDNService;
import com.cookie.global.util.ApiUtil;
import com.cookie.global.util.ApiUtil.ApiSuccess;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin 영화 관리", description = "Admin 영화 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminMovieController {

    private final com.cookie.admin.service.movie.TMDBService TMDBService;
    private final AdminMovieCreateService adminMovieCreateService;
    private final AdminMovieReadService adminMovieReadService;
    private final AdminMovieModifyService adminMovieModifyService;
    private final AdminMovieSearchService adminMovieSearchService;
    private final AWSS3CDNService awss3CDNService;

    @Operation(summary = "영화 초기 세팅 값 - 4600편", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AdminMovieBaseAddResponse.class)))
    })
    @PostMapping("/movie/base")
    public ApiSuccess<?> defaultMoviesAdd() {
        AdminMovieBaseAddResponse data = adminMovieCreateService.defaultMoviesAdd();
        return ApiUtil.success(data);
    }

    @Operation(summary = "TMDB 영화 검색", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AdminSearchResponse.class)))
    })
    @GetMapping("/movie/tmdb/{movieName}")
    public ApiSuccess<?> getMoviesByName(@PathVariable("movieName") String movieName) {
        List<AdminSearchResponse> data = TMDBService.getMoviesByName(movieName);
        return ApiUtil.success(data);
    }

    @Operation(summary = "TMDB 검색 된 영화 선택", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AdminMovieTMDBDetailResponse.class)))
    })
    @GetMapping("/movie/tmdb/choice/{movieId}")
    public ApiSuccess<?> getMovieInfoById(@PathVariable("movieId") Long movieId) {
        AdminMovieTMDBDetailResponse data = TMDBService.getMovieInfoById(movieId);
        return ApiUtil.success(data);
    }

    @Operation(summary = "영화 데이터 생성", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(type = "string", example = "SUCCESS")))
    })
    @PostMapping("/movie")
    public ApiSuccess<String> createMovie(@RequestBody AdminMovieTMDBDetailResponse movie) {
        adminMovieCreateService.createMovie(movie);
        return ApiUtil.success("SUCCESS");
    }

    @Operation(summary = "영화 카테고리 조회", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AdminMovieCategoryResponse.class)))
    })
    @GetMapping("/movie/{movieId}")
    public ApiSuccess<?> getMovieCategory(@PathVariable("movieId") Long movieId) {
        AdminMovieCategoryResponse data = adminMovieReadService.getMovieCategory(movieId);
        return ApiUtil.success(data);
    }

    @Operation(summary = "영화 데이터 상세 조회", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AdminMovieDetailResponse.class)))
    })
    @GetMapping("/movie/{movieId}/detail")
    public ApiSuccess<?> getMovieDetail(@PathVariable("movieId") Long movieId) {
        AdminMovieDetailResponse data = adminMovieReadService.getMovieDetail(movieId);
        return ApiUtil.success(data);
    }

    @Operation(summary = "영화 카테고리 업데이트",
            parameters = {
            @Parameter(name = "MovieCategories",
                    array = @ArraySchema(
                                    schema = @Schema(implementation = MovieCategories.class, nullable = true)))
            }, responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AdminMovieCategoryResponse.class)))
    })
    @PutMapping("/movie/{movieId}")
    public ApiSuccess<?> updateMovieCategory(@PathVariable("movieId") Long movieId,
                                             @RequestBody List<MovieCategories> categories) {
        AdminMovieCategoryResponse data = adminMovieModifyService.updateMovieCategory(movieId, categories);
        return ApiUtil.success(data);
    }

    @Operation(summary = "영화 데이터 삭제", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Long[].class)))
            })
    @DeleteMapping("/movies")
    public ApiSuccess<?> deleteMovie(@RequestBody List<Long> movieIds) {
        List<Long> ids = adminMovieModifyService.deleteMovie(movieIds);
        return ApiUtil.success(ids);
    }

    @Operation(summary = "영화 검색 Admin", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AdminMoviesResponse.class)))
    })
    @GetMapping("/movies/{movieName}/{pageNumber}")
    public ApiSuccess<?> getMovieBySearch(@PathVariable("movieName") String  movieName,
                                          @PathVariable("pageNumber") Integer pageNumber) {
        AdminMoviesResponse data = adminMovieSearchService.getMoviesByName(movieName, pageNumber);
        return ApiUtil.success(data);
    }

    @Operation(summary = "영화 전체 리스트 Admin", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AdminMoviesResponse.class)))
    })
    @GetMapping("/movies/{pageNumber}")
    public ApiSuccess<?> getMovies(@PathVariable("pageNumber") Integer pageNumber) {
        AdminMoviesResponse data = adminMovieSearchService.getMovies(pageNumber);
        return ApiUtil.success(data);
    }

    // ---------- cdn 전용 - 1회성
    @Hidden
    @PostMapping("/movies/updateUrl/posters")
    public ApiSuccess<?> updateMovieImages() {
        awss3CDNService.updateMovieImages();
        return ApiUtil.success("SUCCESS");
    }

    @Hidden
    @PostMapping("/movies/updateUrl/actors")
    public ApiSuccess<?> updateActorImages() {
        awss3CDNService.updateActorImages();
        return ApiUtil.success("SUCCESS");
    }

    @Hidden
    @PostMapping("/movies/updateUrl/directors")
    public ApiSuccess<?> updateDirectorImages() {
        awss3CDNService.updateDirectorImages();
        return ApiUtil.success("SUCCESS");
    }

//    @Hidden
//    @PostMapping("/movies/updateUrl/images")
//    public ApiSuccess<?> updateMovieExtraImages() {
//        awss3CDNService.updateMovieExtraImages();
//        return ApiUtil.success("SUCCESS");
//    }
}
