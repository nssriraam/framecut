package com.framecut.controller;

import com.framecut.entity.User;
import com.framecut.entity.Watchlist;
import com.framecut.service.AuthService;
import com.framecut.service.MovieService;
import com.framecut.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final AuthService authService;
    private final MovieService movieService;

    // Toggle watchlist via AJAX
    @PostMapping("/api/watchlist/toggle/{movieId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable Long movieId,
                                                       Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).build();
        }
        boolean added = watchlistService.toggle(auth.getName(), movieId);
        Map<String, Object> response = new HashMap<>();
        response.put("added", added);
        response.put("message", added ? "Added to watchlist" : "Removed from watchlist");
        return ResponseEntity.ok(response);
    }

    // Watchlist page
    @GetMapping("/watchlist")
    public String watchlistPage(@RequestParam(defaultValue = "0") int page,
                                 Model model,
                                 Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/auth/login";
        }

        User user = authService.getCurrentUser(auth.getName());
        Page<Watchlist> watchlist = watchlistService.getUserWatchlist(user.getId(), page, 20);

        model.addAttribute("currentUser", user);
        model.addAttribute("watchlist", watchlist.getContent());
        model.addAttribute("totalItems", watchlist.getTotalElements());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", watchlist.getTotalPages());
        model.addAttribute("genres", movieService.getAllGenres());
        return "user/watchlist";
    }

    // Infinite scroll API for watchlist
    @GetMapping("/api/watchlist")
    @ResponseBody
    public ResponseEntity<?> watchlistApi(@RequestParam(defaultValue = "0") int page,
                                           Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).build();
        }
        User user = authService.getCurrentUser(auth.getName());
        Page<Watchlist> items = watchlistService.getUserWatchlist(user.getId(), page, 20);

        List<Map<String, Object>> content = items.getContent().stream().map(w -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", w.getMovie().getId());
            m.put("title", w.getMovie().getTitle());
            m.put("poster", w.getMovie().getPosterUrl());
            m.put("year", w.getMovie().getReleaseYear());
            m.put("rating", w.getMovie().getVoteAverage());
            m.put("genre", w.getMovie().getGenreNames() != null ?
                w.getMovie().getGenreNames().split(",")[0].trim() : "");
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("hasMore", page < items.getTotalPages() - 1);
        return ResponseEntity.ok(response);
    }
}
