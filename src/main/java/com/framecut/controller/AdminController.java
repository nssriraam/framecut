package com.framecut.controller;

import com.framecut.entity.Movie;
import com.framecut.entity.User;
import com.framecut.repository.UserRepository;
import com.framecut.service.MovieService;
import com.framecut.service.TmdbService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final MovieService movieService;
    private final UserRepository userRepository;
    private final TmdbService tmdbService;
    private final com.framecut.repository.SyncStateRepository syncStateRepository;

    @GetMapping
    public String dashboard(Model model, Authentication auth) {
        Map<String, Object> stats = movieService.getStats();
        model.addAttribute("stats", stats);
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("genres", movieService.getAllGenres());
        if (auth != null) {
            userRepository.findByUsername(auth.getName())
                .ifPresent(u -> model.addAttribute("currentUser", u));
        }
        return "admin/dashboard";
    }

    @PostMapping("/movies/{id}/delete")
    public String deleteMovie(@PathVariable Long id, RedirectAttributes ra) {
        movieService.delete(id);
        ra.addFlashAttribute("success", "Movie deleted.");
        return "redirect:/admin";
    }

    @PostMapping("/sync")
    public String triggerSync(RedirectAttributes ra) {
        com.framecut.entity.SyncState state = syncStateRepository.findById(1L)
            .orElse(new com.framecut.entity.SyncState(1L, 300, 300));
        int movieStart = state.getMoviePage();
        int tvStart = state.getTvPage();
        int movieEnd = movieStart + 49;
        int tvEnd = tvStart + 49;
        state.setMoviePage(movieEnd + 1);
        state.setTvPage(tvEnd + 1);
        syncStateRepository.save(state);
        Thread t = new Thread(() -> {
            tmdbService.fetchMovies(movieStart, movieEnd);
            tmdbService.fetchSeries(tvStart, tvEnd);
        });
        t.setDaemon(true);
        t.start();
        ra.addFlashAttribute("success", "Sync triggered: movie pages " + movieStart + "-" + movieEnd + ", tv pages " + tvStart + "-" + tvEnd);
        return "redirect:/admin";
    }

    @PostMapping("/sync/reset")
    public String resetSync(RedirectAttributes ra) {
        syncStateRepository.save(new com.framecut.entity.SyncState(1L, 300, 300));
        ra.addFlashAttribute("success", "Sync reset to page 300.");
        return "redirect:/admin";
    }
}
