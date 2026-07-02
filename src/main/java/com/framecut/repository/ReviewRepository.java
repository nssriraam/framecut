package com.framecut.repository;

import com.framecut.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query(value = "SELECT r FROM Review r JOIN FETCH r.movie JOIN FETCH r.user WHERE r.movie.id = :movieId",
           countQuery = "SELECT COUNT(r) FROM Review r WHERE r.movie.id = :movieId")
    Page<Review> findByMovieId(@Param("movieId") Long movieId, Pageable pageable);

    @Query(value = "SELECT r FROM Review r JOIN FETCH r.movie JOIN FETCH r.user WHERE r.user.id = :userId",
           countQuery = "SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId")
    Page<Review> findByUserId(@Param("userId") Long userId, Pageable pageable);

    Optional<Review> findByUserIdAndMovieId(Long userId, Long movieId);
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.movie.id = :movieId")
    Double findAverageRatingByMovieId(@Param("movieId") Long movieId);

    long countByMovieId(Long movieId);
    long countByUserId(Long userId);
}
