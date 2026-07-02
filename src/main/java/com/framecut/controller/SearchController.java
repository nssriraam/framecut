package com.framecut.controller;

import com.framecut.entity.Movie;
import com.framecut.entity.User;
import com.framecut.service.AuthService;
import com.framecut.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final MovieService movieService;
    private final AuthService authService;

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                          @RequestParam(required = false) String type,
                          @RequestParam(required = false) String genre,
                          @RequestParam(required = false) Integer yearFrom,
                          @RequestParam(required = false) Integer yearTo,
                          @RequestParam(required = false) Double ratingMin,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "popularity") String sortBy,
                          Model model,
                          Authentication auth) {

        Page<Movie> results = movieService.searchMovies(q, type, genre, yearFrom, yearTo, ratingMin, page, 20, sortBy);

        model.addAttribute("movies", results.getContent());
        model.addAttribute("totalResults", results.getTotalElements());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", results.getTotalPages());
        model.addAttribute("query", q);
        model.addAttribute("type", type);
        model.addAttribute("genre", genre);
        model.addAttribute("yearFrom", yearFrom);
        model.addAttribute("yearTo", yearTo);
        model.addAttribute("ratingMin", ratingMin);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("genres", movieService.getAllGenres());

        if (auth != null && auth.isAuthenticated()) {
            try {
                User user = authService.getCurrentUser(auth.getName());
                model.addAttribute("currentUser", user);
            } catch (Exception ignored) {}
        }

        return "movies/search";
    }

    // Live autocomplete API
    @GetMapping("/api/search/autocomplete")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> autocomplete(@RequestParam String q) {
        if (q == null || q.length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        List<Movie> movies = movieService.autocomplete(q);
        List<Map<String, Object>> result = movies.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("title", m.getTitle());
            map.put("year", m.getReleaseYear());
            map.put("type", m.getType());
            map.put("genre", m.getGenreNames() != null ?
                m.getGenreNames().split(",")[0].trim() : "");
            map.put("rating", m.getVoteAverage());
            map.put("poster", m.getPosterUrl());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // Infinite scroll API
    @GetMapping("/api/movies")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMoviesApi(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "trending") String section) {

        Page<Movie> movies;
        switch (section) {
            case "trending" -> movies = movieService.getTrending(page, size);
            case "new" -> movies = movieService.getNewReleases(page, size);
            case "toprated" -> movies = movieService.getTopRated(page, size);
            case "series" -> movies = movieService.getAllSeries(page, size);
            case "hiddengems" -> movies = movieService.getHiddenGems(page, size);
            default -> {
                if (genre != null && !genre.isBlank()) {
                    movies = movieService.getByGenre(genre, type, page, size);
                } else if ("tv".equals(type)) {
                    movies = movieService.getAllSeries(page, size);
                } else {
                    movies = movieService.getAllMovies(page, size);
                }
            }
        }

        List<Map<String, Object>> items = movies.getContent().stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("title", m.getTitle());
            map.put("year", m.getReleaseYear());
            map.put("type", m.getType());
            map.put("genre", m.getGenreNames());
            map.put("rating", m.getVoteAverage());
            map.put("poster", m.getPosterUrl());
            map.put("backdrop", m.getBackdropUrlW1280());
            map.put("overview", m.getOverview() != null && m.getOverview().length() > 150
                ? m.getOverview().substring(0, 150) + "..." : m.getOverview());
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", items);
        response.put("page", page);
        response.put("totalPages", movies.getTotalPages());
        response.put("hasMore", page < movies.getTotalPages() - 1);

        return ResponseEntity.ok(response);
    }
}
