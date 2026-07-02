package com.framecut.service;

import com.framecut.entity.Movie;
import com.framecut.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TmdbService {

    private final MovieRepository movieRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${tmdb.api-key}")
    private String apiKey;

    @Value("${tmdb.base-url}")
    private String baseUrl;

    @Value("${tmdb.pages-per-run}")
    private int pagesPerRun;

    private final Map<String, Map<String, Object>> creditsCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, String> trailerCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    public String getApiKey() {
        return apiKey;
    }

    public WebClient getClient() {
        return webClientBuilder
            .baseUrl(baseUrl)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
            .build();
    }

    public void fetchMovies(int startPage, int endPage) {
        log.info("Fetching movies from TMDB pages {} to {}", startPage, endPage);
        for (int page = startPage; page <= endPage; page++) {
            try {
                fetchMoviePage(page);
                Thread.sleep(260); // Stay within TMDB rate limit (40 req/sec)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error fetching movie page {}: {}", page, e.getMessage());
            }
        }
    }

    public void fetchSeries(int startPage, int endPage) {
        log.info("Fetching TV series from TMDB pages {} to {}", startPage, endPage);
        for (int page = startPage; page <= endPage; page++) {
            try {
                fetchSeriesPage(page);
                Thread.sleep(260);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error fetching series page {}: {}", page, e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void searchLive(String query, int page) {
        if (query == null || query.isBlank()) return;
        try {
            // Search Movies
            Map<String, Object> movieResponse = getClient().get()
                .uri(uriBuilder -> uriBuilder
                    .path("/search/movie")
                    .queryParam("api_key", apiKey)
                    .queryParam("query", query)
                    .queryParam("page", page)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (movieResponse != null && movieResponse.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) movieResponse.get("results");
                for (Map<String, Object> item : results) saveMovie(item, "movie");
            }

            // Search TV Series
            Map<String, Object> tvResponse = getClient().get()
                .uri(uriBuilder -> uriBuilder
                    .path("/search/tv")
                    .queryParam("api_key", apiKey)
                    .queryParam("query", query)
                    .queryParam("page", page)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (tvResponse != null && tvResponse.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) tvResponse.get("results");
                for (Map<String, Object> item : results) saveMovie(item, "tv");
            }
        } catch (Exception e) {
            log.error("Failed to perform live search for {}: {}", query, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchMoviePage(int page) {
        try {
            Map<String, Object> response = getClient().get()
                .uri(uriBuilder -> uriBuilder
                    .path("/discover/movie")
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "en-US")

                    .queryParam("primary_release_date.gte", "2000-01-01")
                    .queryParam("sort_by", "popularity.desc")
                    .queryParam("vote_count.gte", "50")
                    .queryParam("page", page)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                for (Map<String, Object> item : results) {
                    saveMovie(item, "movie");
                }
                log.info("Saved movie page {} ({} items)", page, results.size());
            }
        } catch (Exception e) {
            log.error("Failed to fetch movie page {}: {}", page, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchSeriesPage(int page) {
        try {
            Map<String, Object> response = getClient().get()
                .uri(uriBuilder -> uriBuilder
                    .path("/discover/tv")
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "en-US")

                    .queryParam("first_air_date.gte", "2000-01-01")
                    .queryParam("sort_by", "popularity.desc")
                    .queryParam("vote_count.gte", "50")
                    .queryParam("page", page)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                for (Map<String, Object> item : results) {
                    saveMovie(item, "tv");
                }
                log.info("Saved series page {} ({} items)", page, results.size());
            }
        } catch (Exception e) {
            log.error("Failed to fetch series page {}: {}", page, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void saveMovie(Map<String, Object> data, String type) {
        try {
            Object tmdbIdObj = data.get("id");
            if (tmdbIdObj == null) return;
            Long tmdbId = ((Number) tmdbIdObj).longValue();

            if (movieRepository.existsByTmdbId(tmdbId)) return;

            String title = type.equals("movie")
                ? (String) data.getOrDefault("title", "")
                : (String) data.getOrDefault("name", "");

            if (title == null || title.isBlank()) return;

            String releaseDateStr = type.equals("movie")
                ? (String) data.get("release_date")
                : (String) data.get("first_air_date");

            LocalDate releaseDate = null;
            Integer releaseYear = null;
            if (releaseDateStr != null && !releaseDateStr.isBlank() && releaseDateStr.length() >= 4) {
                try {
                    releaseDate = LocalDate.parse(releaseDateStr);
                    releaseYear = releaseDate.getYear();
                } catch (Exception ignored) {
                    try {
                        releaseYear = Integer.parseInt(releaseDateStr.substring(0, 4));
                    } catch (Exception ignored2) {}
                }
            }

            // Build genre names string
            List<Integer> genreIds = (List<Integer>) data.get("genre_ids");
            String genreNames = buildGenreNames(genreIds, type);

            Double voteAverage = data.get("vote_average") != null
                ? ((Number) data.get("vote_average")).doubleValue() : 0.0;
            Integer voteCount = data.get("vote_count") != null
                ? ((Number) data.get("vote_count")).intValue() : 0;
            Double popularity = data.get("popularity") != null
                ? ((Number) data.get("popularity")).doubleValue() : 0.0;

            Movie movie = Movie.builder()
                .tmdbId(tmdbId)
                .title(title)
                .overview((String) data.get("overview"))
                .posterPath((String) data.get("poster_path"))
                .backdropPath((String) data.get("backdrop_path"))
                .voteAverage(voteAverage)
                .voteCount(voteCount)
                .popularity(popularity)
                .releaseDate(releaseDate)
                .releaseYear(releaseYear)
                .originalLanguage((String) data.get("original_language"))
                .type(type)
                .genreNames(genreNames)
                .build();

            movieRepository.save(movie);
        } catch (Exception e) {
            log.debug("Error saving movie: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCredits(Long tmdbId, String type) {
        String cacheKey = type + "_" + tmdbId;
        if (creditsCache.containsKey(cacheKey)) {
            return creditsCache.get(cacheKey);
        }
        try {
            Map<String, Object> response = getClient().get()
                .uri(uriBuilder -> uriBuilder
                    .path("/" + type + "/" + tmdbId + "/credits")
                    .queryParam("api_key", apiKey)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            if (response != null) {
                creditsCache.put(cacheKey, response);
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to fetch credits for {} {}: {}", type, tmdbId, e.getMessage());
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMovieCast(Long tmdbId, String type) {
        Map<String, Object> credits = getCredits(tmdbId, type);
        if (credits != null && credits.containsKey("cast")) {
            return (List<Map<String, Object>>) credits.get("cast");
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public String getMovieDirector(Long tmdbId, String type) {
        Map<String, Object> credits = getCredits(tmdbId, type);
        if (credits != null && credits.containsKey("crew")) {
            List<Map<String, Object>> crew = (List<Map<String, Object>>) credits.get("crew");
            for (Map<String, Object> member : crew) {
                if ("Director".equals(member.get("job"))) {
                    return (String) member.get("name");
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public String getMovieTrailer(Long tmdbId, String type) {
        String cacheKey = type + "_" + tmdbId;
        if (trailerCache.containsKey(cacheKey)) {
            return trailerCache.get(cacheKey);
        }
        try {
            Map<String, Object> response = getClient().get()
                .uri(uriBuilder -> uriBuilder
                    .path("/" + type + "/" + tmdbId + "/videos")
                    .queryParam("api_key", apiKey)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> videos = (List<Map<String, Object>>) response.get("results");
                String key = null;
                // Prefer official YouTube trailers
                for (Map<String, Object> v : videos) {
                    if ("YouTube".equals(v.get("site")) && "Trailer".equals(v.get("type"))) {
                        key = (String) v.get("key");
                        break;
                    }
                }
                if (key == null) {
                    // Fallback to any YouTube video (teaser, etc.)
                    for (Map<String, Object> v : videos) {
                        if ("YouTube".equals(v.get("site"))) {
                            key = (String) v.get("key");
                            break;
                        }
                    }
                }
                if (key != null) {
                    trailerCache.put(cacheKey, key);
                    return key;
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch trailer for {} {}: {}", type, tmdbId, e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getActorDetails(Long personId) {
        try {
            Map<String, Object> response = getClient().get()
                .uri(uriBuilder -> uriBuilder
                    .path("/person/" + personId)
                    .queryParam("api_key", apiKey)
                    .queryParam("append_to_response", "movie_credits,tv_credits")
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            return response != null ? response : Map.of();
        } catch (Exception e) {
            log.error("Failed to fetch actor details {}: {}", personId, e.getMessage());
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> buildDirectorUniverse(String directorName) {
        try {
            // 1. Search Person
            Map<String, Object> searchResp = getClient().get()
                .uri(uriBuilder -> uriBuilder.path("/search/person")
                    .queryParam("api_key", apiKey)
                    .queryParam("query", directorName)
                    .build())
                .retrieve().bodyToMono(Map.class).block();

            if (searchResp == null || !searchResp.containsKey("results")) return Map.of();
            List<Map<String, Object>> results = (List<Map<String, Object>>) searchResp.get("results");
            if (results.isEmpty()) return Map.of();
            Map<String, Object> director = results.get(0);
            Long personId = ((Number) director.get("id")).longValue();

            // 2. Get Person Details & Credits
            Map<String, Object> details = getActorDetails(personId);
            
            // 3. Extract Directed Movies
            Map<String, Object> credits = (Map<String, Object>) details.get("movie_credits");
            if (credits == null || !credits.containsKey("crew")) return Map.of();
            
            List<Map<String, Object>> crew = (List<Map<String, Object>>) credits.get("crew");
            List<Map<String, Object>> directedMovies = crew.stream()
                .filter(c -> "Director".equals(c.get("job")))
                .sorted((a, b) -> Double.compare(
                    ((Number) b.getOrDefault("popularity", 0)).doubleValue(),
                    ((Number) a.getOrDefault("popularity", 0)).doubleValue()))
                .limit(15)
                .toList();

            // 4. Fetch Cast for these movies to find frequent collaborators
            Map<Long, Map<String, Object>> actorCounts = new HashMap<>();
            for (Map<String, Object> movie : directedMovies) {
                Long movieId = ((Number) movie.get("id")).longValue();
                List<Map<String, Object>> cast = getMovieCast(movieId, "movie");
                for (Map<String, Object> actor : cast) {
                    Long actorId = ((Number) actor.get("id")).longValue();
                    if (!actorCounts.containsKey(actorId)) {
                        Map<String, Object> mutableActor = new HashMap<>(actor);
                        mutableActor.put("collabCount", 1);
                        actorCounts.put(actorId, mutableActor);
                    } else {
                        Map<String, Object> existing = actorCounts.get(actorId);
                        existing.put("collabCount", (int) existing.get("collabCount") + 1);
                    }
                }
            }

            List<Map<String, Object>> frequentActors = actorCounts.values().stream()
                .filter(a -> (int) a.get("collabCount") >= 2)
                .sorted((a, b) -> Integer.compare((int) b.get("collabCount"), (int) a.get("collabCount")))
                .limit(12)
                .toList();

            return Map.of(
                "director", details,
                "movies", directedMovies,
                "frequentActors", frequentActors
            );
        } catch (Exception e) {
            log.error("Failed to build universe for {}: {}", directorName, e.getMessage());
            return Map.of();
        }
    }

    private String buildGenreNames(List<Integer> genreIds, String type) {
        if (genreIds == null || genreIds.isEmpty()) return "";
        Map<Integer, String> genreMap = type.equals("movie") ? movieGenreMap() : tvGenreMap();
        List<String> names = new ArrayList<>();
        for (Integer id : genreIds) {
            String name = genreMap.get(id);
            if (name != null) names.add(name);
        }
        return String.join(", ", names);
    }

    private Map<Integer, String> movieGenreMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(28, "Action"); map.put(12, "Adventure"); map.put(16, "Animation");
        map.put(35, "Comedy"); map.put(80, "Crime"); map.put(99, "Documentary");
        map.put(18, "Drama"); map.put(10751, "Family"); map.put(14, "Fantasy");
        map.put(36, "History"); map.put(27, "Horror"); map.put(10402, "Music");
        map.put(9648, "Mystery"); map.put(10749, "Romance"); map.put(878, "Science Fiction");
        map.put(10770, "TV Movie"); map.put(53, "Thriller"); map.put(10752, "War");
        map.put(37, "Western");
        return map;
    }

    private Map<Integer, String> tvGenreMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(10759, "Action & Adventure"); map.put(16, "Animation"); map.put(35, "Comedy");
        map.put(80, "Crime"); map.put(99, "Documentary"); map.put(18, "Drama");
        map.put(10751, "Family"); map.put(10762, "Kids"); map.put(9648, "Mystery");
        map.put(10763, "News"); map.put(10764, "Reality"); map.put(10765, "Sci-Fi & Fantasy");
        map.put(10766, "Soap"); map.put(10767, "Talk"); map.put(10768, "War & Politics");
        map.put(37, "Western");
        return map;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchDirectorsLive(String query) {
        if (query == null || query.isBlank()) return List.of();
        try {
            Map<String, Object> response = getClient().get()
                .uri(uriBuilder -> uriBuilder
                    .path("/search/person")
                    .queryParam("api_key", apiKey)
                    .queryParam("query", query)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                return results.stream()
                    .filter(p -> "Directing".equals(p.get("known_for_department")))
                    .map(p -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", p.get("name"));
                        map.put("profilePath", p.get("profile_path"));
                        return map;
                    })
                    .limit(8)
                    .toList();
            }
        } catch (Exception e) {
            log.error("Error searching directors live: {}", e.getMessage());
        }
        return List.of();
    }
}
