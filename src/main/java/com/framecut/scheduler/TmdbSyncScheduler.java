package com.framecut.scheduler;

import com.framecut.entity.SyncState;
import com.framecut.repository.MovieRepository;
import com.framecut.repository.SyncStateRepository;
import com.framecut.service.TmdbService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TmdbSyncScheduler {

    private final TmdbService tmdbService;
    private final MovieRepository movieRepository;
    private final SyncStateRepository syncStateRepository;

    @Value("${tmdb.fetch-on-startup}")
    private boolean fetchOnStartup;

    @Value("${tmdb.pages-per-run}")
    private int pagesPerRun;

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
                } catch (Exception e) {
                    log.error("Error in initial TMDB sync: {}", e.getMessage(), e);
                }
            });
            thread.setDaemon(true);
            thread.setName("tmdb-initial-sync");
            thread.start();
        }
    }

    private void initialSync() {
        long existing = movieRepository.count();
        // Always sync trending & current theater/OTT releases first
        try {
            tmdbService.fetchTrendingAndNewReleases();
        } catch (Exception e) {
            log.warn("Trending fetch on startup notice: {}", e.getMessage());
        }

        if (existing < 1000) {
            log.info("DB has {} records. Starting bulk fetch of first 100 pages...", existing);
            tmdbService.fetchMovies(1, 100);
            tmdbService.fetchSeries(1, 100);
            syncStateRepository.save(new SyncState(1L, 101, 101));
            log.info("Initial bulk sync complete. Total records: {}", movieRepository.count());
        } else {
            log.info("DB already has {} records. Ready for scheduled sync cycles.", existing);
            if (!syncStateRepository.existsById(1L)) {
                syncStateRepository.save(new SyncState(1L, 101, 101));
            }
        }
    }

    // Runs every 6 hours (initial trending sync already runs on startup)
    @Scheduled(fixedDelay = 21600000, initialDelay = 21600000)
    public void scheduledSync() {
        log.info("Running automatic scheduled TMDB sync...");
        try {
            // 1. Fetch latest trending & new releases (captures latest 2026 releases & OTT debuts)
            tmdbService.fetchTrendingAndNewReleases();

            // 2. Fetch progressive catalog pages tracked in database
            SyncState state = syncStateRepository.findById(1L)
                .orElse(new SyncState(1L, 1, 1));

            int movieStart = state.getMoviePage();
            int tvStart = state.getTvPage();

            // Cycle back to 1 when reaching page 500 to keep library refreshed
            if (movieStart > 500) movieStart = 1;
            if (tvStart > 500) tvStart = 1;

            int movieEnd = Math.min(movieStart + pagesPerRun - 1, 500);
            int tvEnd = Math.min(tvStart + pagesPerRun - 1, 500);

            log.info("Scheduled sync: fetching movie pages {}-{}, tv pages {}-{}", movieStart, movieEnd, tvStart, tvEnd);
            tmdbService.fetchMovies(movieStart, movieEnd);
            tmdbService.fetchSeries(tvStart, tvEnd);

            state.setMoviePage(movieEnd >= 500 ? 1 : movieEnd + 1);
            state.setTvPage(tvEnd >= 500 ? 1 : tvEnd + 1);
            syncStateRepository.save(state);

            log.info("Scheduled sync completed. Total records in DB: {}", movieRepository.count());
        } catch (Exception e) {
            log.error("Error during scheduled TMDB sync: {}", e.getMessage(), e);
        }
    }
}
