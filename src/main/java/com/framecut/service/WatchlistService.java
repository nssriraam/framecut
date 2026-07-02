package com.framecut.service;

import com.framecut.entity.Movie;
import com.framecut.entity.User;
import com.framecut.entity.Watchlist;
import com.framecut.repository.MovieRepository;
import com.framecut.repository.UserRepository;
import com.framecut.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    public boolean isInWatchlist(Long userId, Long movieId) {
        return watchlistRepository.existsByUserIdAndMovieId(userId, movieId);
    }

    @Transactional
    public boolean toggle(String username, Long movieId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Movie movie = movieRepository.findById(movieId)
            .orElseThrow(() -> new RuntimeException("Movie not found"));

        if (watchlistRepository.existsByUserIdAndMovieId(user.getId(), movieId)) {
            watchlistRepository.deleteByUserIdAndMovieId(user.getId(), movieId);
            return false; // removed
        } else {
            Watchlist entry = Watchlist.builder()
                .user(user)
                .movie(movie)
                .build();
            watchlistRepository.save(entry);
            return true; // added
        }
    }

    public Page<Watchlist> getUserWatchlist(Long userId, int page, int size) {
        return watchlistRepository.findByUserIdOrderByAddedAtDesc(userId, PageRequest.of(page, size));
    }

    public long countByUser(Long userId) {
        return watchlistRepository.countByUserId(userId);
    }
}
