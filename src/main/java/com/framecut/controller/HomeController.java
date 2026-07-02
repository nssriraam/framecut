package com.framecut.controller;

import com.framecut.entity.Movie;
import com.framecut.entity.User;
import com.framecut.service.AuthService;
import com.framecut.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final MovieService movieService;
    private final AuthService authService;

    @GetMapping({"/", "/home"})
    public String home(Model model, Authentication auth) {
        List<Movie> heroMovies = movieService.getHeroMovies();
        List<Movie> trending = movieService.getTrending(0, 20).getContent();
        List<Movie> newReleases = movieService.getNewReleases(0, 20).getContent();
        List<Movie> topRated = movieService.getTopRated(0, 20).getContent();
        List<Movie> popularSeries = movieService.getTrendingSeries(0, 20).getContent();
        List<String> genres = movieService.getAllGenres();

        model.addAttribute("heroMovies", heroMovies);
        model.addAttribute("trending", trending);
        model.addAttribute("newReleases", newReleases);
        model.addAttribute("topRated", topRated);
        model.addAttribute("popularSeries", popularSeries);
        model.addAttribute("genres", genres);
        model.addAttribute("stats", movieService.getStats());

        if (auth != null && auth.isAuthenticated()) {
            try {
                User user = authService.getCurrentUser(auth.getName());
                model.addAttribute("currentUser", user);
            } catch (Exception ignored) {}
        }

        return "home";
    }

    @GetMapping("/movies")
    public String moviesPage(Model model,
                              @RequestParam(defaultValue = "0") int page,
                              Authentication auth) {
        Page<Movie> movies = movieService.getAllMovies(page, 20);
        model.addAttribute("movies", movies.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", movies.getTotalPages());
        model.addAttribute("type", "movie");
        model.addAttribute("pageTitle", "Movies");
        model.addAttribute("genres", movieService.getAllGenres());
        addUserToModel(model, auth);
        return "movies/list";
    }

    @GetMapping("/tv")
    public String seriesPage(Model model,
                              @RequestParam(defaultValue = "0") int page,
                              Authentication auth) {
        Page<Movie> series = movieService.getAllSeries(page, 20);
        model.addAttribute("movies", series.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", series.getTotalPages());
        model.addAttribute("type", "tv");
        model.addAttribute("pageTitle", "Series");
        model.addAttribute("genres", movieService.getAllGenres());
        addUserToModel(model, auth);
        return "movies/list";
    }

    @GetMapping("/genres/{genre}")
    public String genrePage(@PathVariable String genre,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false) String type,
                             Model model,
                             Authentication auth) {
        Page<Movie> movies = movieService.getByGenre(genre, type, page, 20);
        model.addAttribute("movies", movies.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", movies.getTotalPages());
        model.addAttribute("genre", genre);
        model.addAttribute("type", type);
        model.addAttribute("pageTitle", genre);
        model.addAttribute("genres", movieService.getAllGenres());
        addUserToModel(model, auth);
        return "movies/genre";
    }

    private void addUserToModel(Model model, Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            try {
                User user = authService.getCurrentUser(auth.getName());
                model.addAttribute("currentUser", user);
            } catch (Exception ignored) {}
        }
    }
}
