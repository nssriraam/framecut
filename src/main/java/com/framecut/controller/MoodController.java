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

@Controller
@RequiredArgsConstructor
public class MoodController {

    private final MovieService movieService;
    private final AuthService authService;

    @GetMapping("/mood")
    public String moodPage(Model model, Authentication auth) {
        model.addAttribute("genres", movieService.getAllGenres());
        if (auth != null && auth.isAuthenticated()) {
            try { model.addAttribute("currentUser", authService.getCurrentUser(auth.getName())); }
            catch (Exception ignored) {}
        }
        return "movies/mood";
    }

    @GetMapping("/mood/{mood}")
    public String moodResults(@PathVariable String mood,
                               @RequestParam(defaultValue = "0") int page,
                               Model model,
                               Authentication auth) {
        Page<Movie> movies = movieService.getByMood(mood, page, 20);
        model.addAttribute("movies", movies.getContent());
        model.addAttribute("mood", mood);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", movies.getTotalPages());
        model.addAttribute("genres", movieService.getAllGenres());
        model.addAttribute("pageTitle", mood);
        if (auth != null && auth.isAuthenticated()) {
            try { model.addAttribute("currentUser", authService.getCurrentUser(auth.getName())); }
            catch (Exception ignored) {}
        }
        return "movies/mood-results";
    }

    @GetMapping("/hidden-gems")
    public String hiddenGems(@RequestParam(defaultValue = "0") int page,
                              Model model,
                              Authentication auth) {
        Page<Movie> movies = movieService.getHiddenGems(page, 20);
        model.addAttribute("movies", movies.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", movies.getTotalPages());
        model.addAttribute("genres", movieService.getAllGenres());
        model.addAttribute("pageTitle", "Hidden Gems");
        model.addAttribute("section", "hiddengems");
        if (auth != null && auth.isAuthenticated()) {
            try { model.addAttribute("currentUser", authService.getCurrentUser(auth.getName())); }
            catch (Exception ignored) {}
        }
        return "movies/list";
    }
}
