package com.framecut.repository;

import com.framecut.entity.Watchlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
    Optional<Watchlist> findByUserIdAndMovieId(Long userId, Long movieId);
    @Query(value = "SELECT w FROM Watchlist w JOIN FETCH w.movie JOIN FETCH w.user WHERE w.user.id = :userId ORDER BY w.addedAt DESC",
           countQuery = "SELECT COUNT(w) FROM Watchlist w WHERE w.user.id = :userId")
    Page<Watchlist> findByUserIdOrderByAddedAtDesc(@Param("userId") Long userId, Pageable pageable);
    long countByUserId(Long userId);
    void deleteByUserIdAndMovieId(Long userId, Long movieId);
}
