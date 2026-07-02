package com.framecut.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "movie_id"}),
    indexes = {
        @Index(name = "idx_watchlist_user", columnList = "user_id"),
        @Index(name = "idx_watchlist_movie", columnList = "movie_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();
}
