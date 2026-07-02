package com.framecut.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movies", indexes = {
    @Index(name = "idx_movie_tmdb_id", columnList = "tmdbId", unique = true),
    @Index(name = "idx_movie_title", columnList = "title"),
    @Index(name = "idx_movie_type", columnList = "type"),
    @Index(name = "idx_movie_release_year", columnList = "releaseYear"),
    @Index(name = "idx_movie_vote_average", columnList = "voteAverage")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tmdbId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String overview;

    private String posterPath;
    private String backdropPath;

    private Double voteAverage;
    private Integer voteCount;
    private Double popularity;

    private Integer releaseYear;
    private LocalDate releaseDate;

    private Integer runtime;
    private String originalLanguage;

    @Column(length = 20)
    private String type; // "movie" or "tv"

    private String director;
    @Column(name = "cast_names")
    private String castNames;

    @Column(length = 1000)
    private String genreNames;

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    public String getPosterUrl() {
        if (posterPath != null && !posterPath.isEmpty()) {
            return "https://image.tmdb.org/t/p/w500" + posterPath;
        }
        return "/images/no-poster.jpg";
    }

    public String getBackdropUrl() {
        if (backdropPath != null && !backdropPath.isEmpty()) {
            return "https://image.tmdb.org/t/p/original" + backdropPath;
        }
        return null;
    }

    public String getBackdropUrlW1280() {
        if (backdropPath != null && !backdropPath.isEmpty()) {
            return "https://image.tmdb.org/t/p/w1280" + backdropPath;
        }
        return null;
    }

    public String getFirstTwoGenres() {
        if (genreNames == null || genreNames.isBlank()) {
            return "";
        }
        String[] split = genreNames.split(",");
        if (split.length > 1) {
            return split[0].trim() + ", " + split[1].trim();
        }
        return split[0].trim();
    }

    public double getAverageRating() {
        if (reviews == null || reviews.isEmpty()) return 0.0;
        return reviews.stream()
            .mapToInt(Review::getRating)
            .average()
            .orElse(0.0);
    }
}
