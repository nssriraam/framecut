package com.framecut.controller;

import com.framecut.dto.ReviewRequest;
import com.framecut.entity.Movie;
import com.framecut.entity.Review;
import com.framecut.entity.User;
import com.framecut.service.AuthService;
import com.framecut.service.TmdbService;
import com.framecut.service.WatchlistService;
import com.framecut.service.MovieService;
import com.framecut.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final ReviewService reviewService;
    private final AuthService authService;
    private final WatchlistService watchlistService;
    private final TmdbService tmdbService;

    @GetMapping("/tmdb/{tmdbId}")
    public String movieByTmdbId(@PathVariable Long tmdbId, RedirectAttributes redirectAttributes) {
        Optional<Movie> movieOpt = movieService.getMovieByTmdbId(tmdbId);
        if (movieOpt.isPresent()) {
            return "redirect:/movies/" + movieOpt.get().getId();
        }
        
        // If not in DB, fetch from TMDB using our live search fallback
        try {
            // First check if it's a movie
            Map<String, Object> response = tmdbService.getClient().get()
                .uri(uriBuilder -> uriBuilder.path("/movie/" + tmdbId)
                    .queryParam("api_key", tmdbService.getApiKey()).build())
                .retrieve().bodyToMono(Map.class).block();
                
            if (response != null && response.containsKey("title")) {
                tmdbService.saveMovie(response, "movie");
                Optional<Movie> savedOpt = movieService.getMovieByTmdbId(tmdbId);
                if (savedOpt.isPresent()) return "redirect:/movies/" + savedOpt.get().getId();
            }
        } catch (Exception ignored) {}
        
        return "redirect:/";
    }

    @GetMapping("/{id}")
    public String movieDetail(@PathVariable Long id,
                               @RequestParam(defaultValue = "0") int reviewPage,
                               Model model,
                               Authentication auth) {
        Optional<Movie> movieOpt = movieService.getMovieById(id);
        if (movieOpt.isEmpty()) return "redirect:/";

        Movie movie = movieOpt.get();
        Page<Review> reviews = reviewService.getMovieReviews(id, reviewPage, 10);
        Double avgRating = reviewService.getAverageRating(id);

        model.addAttribute("movie", movie);
        model.addAttribute("reviews", reviews.getContent());
        model.addAttribute("reviewPage", reviewPage);
        model.addAttribute("totalReviewPages", reviews.getTotalPages());
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("reviewCount", reviews.getTotalElements());
        model.addAttribute("reviewRequest", new ReviewRequest());
        model.addAttribute("genres", movieService.getAllGenres());
        model.addAttribute("similarMovies", movieService.getSimilarMovies(
            id, 
            movie.getGenreNames(), 
            movie.getType(), 
            movie.getOriginalLanguage()));
        model.addAttribute("cast", tmdbService.getMovieCast(movie.getTmdbId(), movie.getType()));
        model.addAttribute("directorName", tmdbService.getMovieDirector(movie.getTmdbId(), movie.getType()));
        model.addAttribute("trailerKey", tmdbService.getMovieTrailer(movie.getTmdbId(), movie.getType()));

        if (auth != null && auth.isAuthenticated()) {
            try {
                User user = authService.getCurrentUser(auth.getName());
                model.addAttribute("currentUser", user);
                boolean hasReviewed = reviewService.hasUserReviewed(user.getId(), id);
                model.addAttribute("hasReviewed", hasReviewed);
                model.addAttribute("isInWatchlist", watchlistService.isInWatchlist(user.getId(), id));
                if (hasReviewed) {
                    Review userReview = reviewService.getUserReviewForMovie(user.getId(), id);
                    model.addAttribute("userReview", userReview);
                }
            } catch (Exception ignored) {}
        }

        return "movies/detail";
    }

    @PostMapping("/{id}/review")
    public String submitReview(@PathVariable Long id,
                                @ModelAttribute ReviewRequest request,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/auth/login";
        }
        try {
            reviewService.addReview(id, auth.getName(), request);
            redirectAttributes.addFlashAttribute("reviewSuccess", "Review submitted!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("reviewError", e.getMessage());
        }
        return "redirect:/movies/" + id;
    }

    @PostMapping("/review/{reviewId}/delete")
    public String deleteReview(@PathVariable Long reviewId,
                                @RequestParam Long movieId,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        if (auth == null) return "redirect:/auth/login";
        try {
            reviewService.deleteReview(reviewId, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Review deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/movies/" + movieId;
    }
}
