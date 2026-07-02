package com.framecut.scheduler;

import com.framecut.repository.MovieRepository;
import com.framecut.service.TmdbService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class TmdbSyncScheduler {

    private final TmdbService tmdbService;
    private final MovieRepository movieRepository;

    @Value("${tmdb.fetch-on-startup}")
    private boolean fetchOnStartup;

    @Value("${tmdb.pages-per-run}")
    private int pagesPerRun;

    private final AtomicInteger moviePageOffset = new AtomicInteger(1);
    private final AtomicInteger tvPageOffset = new AtomicInteger(1);

    @PostConstruct
    public void onStartup() {
        if (fetchOnStartup) {
            log.info("Starting initial TMDB sync in background thread...");
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(5000); // wait for app to fully start
                    initialSync();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            thread.setDaemon(true);
            thread.setName("tmdb-initial-sync");
            thread.start();
        }
    }

    private void initialSync() {
        long existing = movieRepository.count();
        if (existing < 1000) {
            log.info("DB has {} records. Starting bulk fetch...", existing);
            // Fetch first 100 pages of movies and TV (2000 records each = 4000 total first run)
            tmdbService.fetchMovies(1, 100);
            tmdbService.fetchSeries(1, 100);
            log.info("Initial sync complete. Total records: {}", movieRepository.count());
            moviePageOffset.set(101);
            tvPageOffset.set(101);
        } else {
            log.info("DB already has {} records. Skipping bulk fetch.", existing);
            int nextMoviePage = 200;
            int nextTvPage = 200;
            moviePageOffset.set(nextMoviePage);
            tvPageOffset.set(nextTvPage);
        }
    }

    // Runs every 6 hours to fetch more movies
    @Scheduled(fixedDelay = 21600000, initialDelay = 600000)
    public void scheduledSync() {
        int movieStart = moviePageOffset.get();
        if (movieStart > 500) {
            log.info("Movie sync reached TMDB page 500 limit. Skipping background fetch.");
        } else {
            int movieEnd = Math.min(movieStart + pagesPerRun - 1, 500);
            log.info("Scheduled sync: fetching movie pages {}-{}", movieStart, movieEnd);
            tmdbService.fetchMovies(movieStart, movieEnd);
            moviePageOffset.set(movieEnd + 1);
        }

        int tvStart = tvPageOffset.get();
        if (tvStart > 500) {
            log.info("TV sync reached TMDB page 500 limit. Skipping background fetch.");
        } else {
            int tvEnd = Math.min(tvStart + pagesPerRun - 1, 500);
            log.info("Scheduled sync: fetching tv pages {}-{}", tvStart, tvEnd);
            tmdbService.fetchSeries(tvStart, tvEnd);
            tvPageOffset.set(tvEnd + 1);
        }

        log.info("Scheduled sync done. Total records: {}", movieRepository.count());
    }
}
