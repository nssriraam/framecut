package com.framecut.service;

import com.framecut.dto.ReviewRequest;
import com.framecut.entity.Movie;
import com.framecut.entity.Review;
import com.framecut.entity.User;
import com.framecut.repository.MovieRepository;
import com.framecut.repository.ReviewRepository;
import com.framecut.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    public Page<Review> getMovieReviews(Long movieId, int page, int size) {
        return reviewRepository.findByMovieId(movieId,
            PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public Page<Review> getUserReviews(Long userId, int page, int size) {
        return reviewRepository.findByUserId(userId,
            PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public boolean hasUserReviewed(Long userId, Long movieId) {
        return reviewRepository.existsByUserIdAndMovieId(userId, movieId);
    }

    @Transactional
    public Review addReview(Long movieId, String username, ReviewRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Movie movie = movieRepository.findById(movieId)
            .orElseThrow(() -> new RuntimeException("Movie not found"));

        if (reviewRepository.existsByUserIdAndMovieId(user.getId(), movieId)) {
            throw new RuntimeException("You have already reviewed this movie");
        }

        Review review = Review.builder()
            .user(user)
            .movie(movie)
            .rating(request.getRating())
            .content(request.getContent())
            .score(request.getScore())
            .summary(request.getSummary())
            .hasSpoilers(request.getHasSpoilers() != null && request.getHasSpoilers())
            .build();

        return reviewRepository.save(review);
    }

    @Transactional
    public Review updateReview(Long reviewId, String username, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review not found"));
        if (!review.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Not authorized");
        }
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setUpdatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, String username) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review not found"));
        if (!review.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Not authorized");
        }
        reviewRepository.delete(review);
    }

    public Review getUserReviewForMovie(Long userId, Long movieId) {
        return reviewRepository.findByUserIdAndMovieId(userId, movieId).orElse(null);
    }

    public Double getAverageRating(Long movieId) {
        Double avg = reviewRepository.findAverageRatingByMovieId(movieId);
        return avg != null ? avg : 0.0;
    }
}
