package com.framecut.controller;

import com.framecut.entity.Review;
import com.framecut.entity.User;
import com.framecut.service.AuthService;
import com.framecut.service.MovieService;
import com.framecut.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final ReviewService reviewService;
    private final MovieService movieService;

    @GetMapping("/profile")
    public String profile(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/auth/login";
        User user = authService.getCurrentUser(auth.getName());
        Page<Review> reviews = reviewService.getUserReviews(user.getId(), 0, 10);

        model.addAttribute("currentUser", user);
        model.addAttribute("reviews", reviews.getContent());
        model.addAttribute("totalReviews", reviews.getTotalElements());
        model.addAttribute("genres", movieService.getAllGenres());
        return "user/profile";
    }

    @GetMapping("/my-reviews")
    public String myReviews(@RequestParam(defaultValue = "0") int page,
                             Model model,
                             Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/auth/login";
        User user = authService.getCurrentUser(auth.getName());
        Page<Review> reviews = reviewService.getUserReviews(user.getId(), page, 12);

        model.addAttribute("currentUser", user);
        model.addAttribute("reviews", reviews.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reviews.getTotalPages());
        model.addAttribute("totalReviews", reviews.getTotalElements());
        model.addAttribute("genres", movieService.getAllGenres());
        return "user/reviews";
    }

    // Infinite scroll for my reviews
    @GetMapping("/api/my-reviews")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> myReviewsApi(
            @RequestParam(defaultValue = "0") int page,
            Authentication auth) {
        if (auth == null) return org.springframework.http.ResponseEntity.status(401).build();
        User user = authService.getCurrentUser(auth.getName());
        Page<Review> reviews = reviewService.getUserReviews(user.getId(), page, 12);

        java.util.List<java.util.Map<String, Object>> items = reviews.getContent().stream().map(r -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", r.getId());
            map.put("movieId", r.getMovie().getId());
            map.put("movieTitle", r.getMovie().getTitle());
            map.put("moviePoster", r.getMovie().getPosterUrl());
            map.put("rating", r.getRating());
            map.put("content", r.getContent());
            map.put("createdAt", r.getCreatedAt().toString());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("content", items);
        response.put("hasMore", page < reviews.getTotalPages() - 1);
        return org.springframework.http.ResponseEntity.ok(response);
    }
}
