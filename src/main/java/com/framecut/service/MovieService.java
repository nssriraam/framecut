package com.framecut.service;

import com.framecut.entity.Movie;
import com.framecut.repository.MovieRepository;
import com.framecut.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;
    private final TmdbService tmdbService;

    public Page<Movie> getTrending(int page, int size) {
        return movieRepository.findAllTrending(PageRequest.of(page, size));
    }

    public Optional<Movie> getMovieByTmdbId(Long tmdbId) {
        return movieRepository.findByTmdbId(tmdbId);
    }

    public Page<Movie> getTrendingMovies(int page, int size) {
        return movieRepository.findTrending("movie", PageRequest.of(page, size));
    }

    public Page<Movie> getTrendingSeries(int page, int size) {
        return movieRepository.findTrending("tv", PageRequest.of(page, size));
    }

    public Page<Movie> getNewReleases(int page, int size) {
        return movieRepository.findNewReleases(PageRequest.of(page, size));
    }

    public Page<Movie> getTopRated(int page, int size) {
        return movieRepository.findTopRated(PageRequest.of(page, size));
    }

    public Page<Movie> getAllMovies(int page, int size) {
        return movieRepository.findByType("movie", PageRequest.of(page, size, Sort.by("popularity").descending().and(Sort.by("id").descending())));
    }

    public Page<Movie> getAllSeries(int page, int size) {
        return movieRepository.findByType("tv", PageRequest.of(page, size, Sort.by("popularity").descending().and(Sort.by("id").descending())));
    }

    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findById(id);
    }

    public Page<Movie> searchMovies(String query, String type, String genre,
                                     Integer yearFrom, Integer yearTo,
                                     Double ratingMin, int page, int size,
                                     String sortBy) {
        if (query != null && !query.isBlank()) {
            // Live TMDB fetch for the specific page requested (Spring page is 0-indexed, TMDB is 1-indexed)
            tmdbService.searchLive(query, page + 1);
        }
        
        Sort sort = switch (sortBy != null ? sortBy : "popularity") {
            case "rating" -> Sort.by("voteAverage").descending().and(Sort.by("id").descending());
            case "year" -> Sort.by("releaseYear").descending().and(Sort.by("id").descending());
            case "title" -> Sort.by("title").ascending().and(Sort.by("id").descending());
            default -> Sort.by("popularity").descending().and(Sort.by("id").descending());
        };
        Pageable pageable = PageRequest.of(page, size, sort);
        return movieRepository.findWithFilters(
            (query != null && !query.isBlank()) ? query : null,
            (type != null && !type.isBlank()) ? type : null,
            yearFrom, yearTo, ratingMin, pageable
        );
    }

    public List<Movie> autocomplete(String query) {
        if (query != null && !query.isBlank()) {
            tmdbService.searchLive(query, 1);
        }
        return movieRepository.searchForAutocomplete(query, null, PageRequest.of(0, 8));
    }

    public Page<Movie> getByGenre(String genre, String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("popularity").descending().and(Sort.by("id").descending()));
        if (type != null && !type.isBlank()) {
            return movieRepository.findByGenreAndType(genre, type, pageable);
        }
        return movieRepository.findByGenre(genre, pageable);
    }

    public List<String> getAllGenres() {
        Set<String> genres = new TreeSet<>();
        List<String> raw = movieRepository.findAllGenreNames();
        for (String genreStr : raw) {
            if (genreStr != null) {
                Arrays.stream(genreStr.split(","))
                    .map(String::trim)
                    .filter(g -> !g.isEmpty())
                    .forEach(genres::add);
            }
        }
        return new ArrayList<>(genres);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMovies", movieRepository.countByType("movie"));
        stats.put("totalSeries", movieRepository.countByType("tv"));
        stats.put("totalAll", movieRepository.count());
        return stats;
    }

    // Hero backdrop movies - top 5 trending with backdrops
    public List<Movie> getHeroMovies() {
        return movieRepository.findHeroCandidates(PageRequest.of(0, 20))
            .getContent()
            .stream()
            .limit(10)
            .collect(Collectors.toList());
    }


    public List<Movie> getSimilarMovies(Long movieId, String genreNames, String type, String lang) {
        if (genreNames == null || genreNames.isBlank()) return List.of();
        String firstGenre = genreNames.split(",")[0].trim();
        return movieRepository.findSimilarMovies(firstGenre, type, lang, movieId, 6.0,
            PageRequest.of(0, 8));
    }

    public Page<Movie> getHiddenGems(int page, int size) {
        return movieRepository.findHiddenGems(PageRequest.of(page, size));
    }

    public Page<Movie> getByMood(String mood, int page, int size) {
        String genre = switch (mood.toLowerCase()) {
            case "thrilled"  -> "Action";
            case "emotional" -> "Drama";
            case "mindblow"  -> "Science Fiction";
            case "feelgood"  -> "Comedy";
            case "scared"    -> "Horror";
            case "inspired"  -> "History";
            case "nostalgic" -> "Family";
            case "adrenaline"-> "Action";
            case "cozy"      -> "Romance";
            default          -> "Action";
        };
        return movieRepository.findByGenre(genre, PageRequest.of(page, size,
            Sort.by("popularity").descending().and(Sort.by("id").descending())));
    }

    public Movie save(Movie movie) {
        return movieRepository.save(movie);
    }

    public void delete(Long id) {
        movieRepository.deleteById(id);
    }
}
