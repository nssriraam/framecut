package com.framecut.repository;

import com.framecut.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    Optional<Movie> findByTmdbId(Long tmdbId);
    boolean existsByTmdbId(Long tmdbId);

    @Query("SELECT m FROM Movie m WHERE m.type = :type AND m.posterPath IS NOT NULL AND m.posterPath != '' AND m.popularity >= 8.0 AND m.voteCount >= 50")
    Page<Movie> findByType(@Param("type") String type, Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE m.type = :type AND m.posterPath IS NOT NULL AND m.posterPath != '' AND m.popularity >= 8.0 AND m.voteCount >= 50 ORDER BY m.popularity DESC, m.id DESC")
    Page<Movie> findTrending(@Param("type") String type, Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE m.posterPath IS NOT NULL AND m.posterPath != '' AND m.popularity >= 8.0 AND m.voteCount >= 50 ORDER BY m.popularity DESC, m.id DESC")
    Page<Movie> findAllTrending(Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE m.releaseYear >= 2020 AND m.voteAverage >= 7.2 AND m.backdropPath IS NOT NULL AND m.backdropPath != '' AND m.posterPath IS NOT NULL AND m.posterPath != '' ORDER BY m.popularity DESC, m.id DESC")
    Page<Movie> findHeroCandidates(Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE m.posterPath IS NOT NULL AND m.posterPath != '' AND m.popularity >= 8.0 AND m.voteCount >= 30 ORDER BY m.releaseYear DESC, m.releaseDate DESC, m.id DESC")
    Page<Movie> findNewReleases(Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE m.type = :type AND m.posterPath IS NOT NULL AND m.posterPath != '' AND m.popularity >= 8.0 AND m.voteCount >= 30 ORDER BY m.releaseYear DESC, m.releaseDate DESC, m.id DESC")
    Page<Movie> findNewReleasesByType(@Param("type") String type, Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE m.voteCount >= 300 AND m.posterPath IS NOT NULL AND m.posterPath != '' AND m.voteAverage >= 7.0 ORDER BY m.voteAverage DESC, m.id DESC")
    Page<Movie> findTopRated(Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE " +
           "LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.overview) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Movie> searchMovies(@Param("query") String query, Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE " +
           "(LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:type IS NULL OR m.type = :type)")
    List<Movie> searchForAutocomplete(@Param("query") String query, @Param("type") String type, Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Movie> findByTitleContainingIgnoreCase(@Param("query") String query, Pageable pageable);


    @Query("SELECT m FROM Movie m WHERE m.posterPath IS NOT NULL AND m.posterPath != '' AND m.popularity >= 8.0 AND " +
           "LOWER(CONCAT(',', REPLACE(m.genreNames, ', ', ','), ',')) LIKE LOWER(CONCAT('%,', :genre, ',%'))")
    Page<Movie> findByGenre(@Param("genre") String genre, Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE m.posterPath IS NOT NULL AND m.posterPath != '' AND m.popularity >= 8.0 AND " +
           "LOWER(CONCAT(',', REPLACE(m.genreNames, ', ', ','), ',')) LIKE LOWER(CONCAT('%,', :genre, ',%')) AND " +
           "m.type = :type")
    Page<Movie> findByGenreAndType(@Param("genre") String genre, @Param("type") String type, Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE m.posterPath IS NOT NULL AND m.posterPath != '' AND m.popularity >= 5.0 AND " +
           "(:query IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:type IS NULL OR m.type = :type) AND " +
           "(:yearFrom IS NULL OR m.releaseYear >= :yearFrom) AND " +
           "(:yearTo IS NULL OR m.releaseYear <= :yearTo) AND " +
           "(:ratingMin IS NULL OR m.voteAverage >= :ratingMin)")
    Page<Movie> findWithFilters(
        @Param("query") String query,
        @Param("type") String type,
        @Param("yearFrom") Integer yearFrom,
        @Param("yearTo") Integer yearTo,
        @Param("ratingMin") Double ratingMin,
        Pageable pageable);

    @Query("SELECT DISTINCT m.genreNames FROM Movie m WHERE m.genreNames IS NOT NULL")
    List<String> findAllGenreNames();

    long countByType(String type);

    @Query("SELECT m FROM Movie m WHERE " +
           "LOWER(CONCAT(',', REPLACE(m.genreNames, ', ', ','), ',')) LIKE LOWER(CONCAT('%,', :genre, ',%')) AND " +
           "m.type = :type AND " +
           "m.id != :excludeId AND m.voteAverage >= :minRating " +
           "ORDER BY CASE WHEN m.originalLanguage = :lang THEN 1 ELSE 2 END ASC, " +
           "m.popularity DESC, m.id DESC")
    List<Movie> findSimilarMovies(@Param("genre") String genre,
                                   @Param("type") String type,
                                   @Param("lang") String lang,
                                   @Param("excludeId") Long excludeId,
                                   @Param("minRating") Double minRating,
                                   Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE " +
           "m.posterPath IS NOT NULL AND m.posterPath != '' AND m.popularity >= 8.0 AND " +
           "m.voteAverage >= 7.5 AND m.voteCount <= 1000 AND m.voteCount >= 50 " +
           "ORDER BY m.voteAverage DESC, m.id DESC")
    Page<Movie> findHiddenGems(Pageable pageable);
}
