package com.framecut.controller;

import com.framecut.service.TmdbService;
import com.framecut.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class UniverseController {

    private final TmdbService tmdbService;
    private final MovieService movieService;

    public record DirectorDto(String name, String profilePath) {}

    private static final java.util.List<DirectorDto> TOP_DIRECTORS = java.util.List.of(
        new DirectorDto("Christopher Nolan", "/xuAIuYSmsUzKlUMBFGVZaWsY3DZ.jpg"),
        new DirectorDto("Martin Scorsese", "/9U9Y5GQuWX3EZy39B8nkk4NY01S.jpg"),
        new DirectorDto("Quentin Tarantino", "/1gjcpAa99FAOWGnrUvHEXXsRs7o.jpg"),
        new DirectorDto("Steven Spielberg", "/tZxcg19YQ3e8fJ0pOs7hjlnmmr6.jpg"),
        new DirectorDto("Denis Villeneuve", "/zdDx9Xs93UIrJFWYApYR28J8M6b.jpg"),
        new DirectorDto("Bong Joon-ho", "/stwnTvZAoD8gEJEDHpDQyLCyDy5.jpg"),
        new DirectorDto("James Cameron", "/9NApeRObQEWnAKeaH96oR185V1K.jpg"),
        new DirectorDto("Greta Gerwig", "/1XddSjQGZq6mDktoFzXg6k2k04h.jpg"),
        new DirectorDto("Jordan Peele", "/kFUKn5g3ebpyZ3CSZZZo2HFWRNQ.jpg"),
        new DirectorDto("David Fincher", "/kR7jH0sQv3m2n826LwH45763k67.jpg"),
        new DirectorDto("Alfred Hitchcock", "/108fiNM6poRieMg7RIqLJRxdAwG.jpg"),
        new DirectorDto("Stanley Kubrick", "/yFT0VyIelI9aegZrsAwOG5iVP4v.jpg"),
        new DirectorDto("Ridley Scott", "/97SO7H0UlS3racqjeW5JTy8c6GM.jpg"),
        new DirectorDto("Wes Anderson", "/s03CeUeC5yAXyB1acqP0zGNo2SC.jpg"),
        new DirectorDto("Francis Ford Coppola", "/IwGgkmW6IoJ9vuNF0T9CU3FYUX.jpg"),
        new DirectorDto("Peter Jackson", "/z83wT98zWnJ7oN5b57f0G48F6uS.jpg"),
        new DirectorDto("Guillermo del Toro", "/gldeyCtKcaqnK1v4Vu9vqayhzUQ.jpg"),
        new DirectorDto("Akira Kurosawa", "/g2iwSho2vJxVz7cbD1FAKAmPD6A.jpg"),
        new DirectorDto("Hayao Miyazaki", "/ouhjt9KugzhWtdEyBPipihB3ic8.jpg"),
        new DirectorDto("Christopher McQuarrie", "/xVbQBRpqzKkllUAvjJbgnAmTwaZ.jpg")
    );

    @GetMapping("/universe")
    public String universePage(@RequestParam(required = false) String director, Model model) {
        model.addAttribute("genres", movieService.getAllGenres());
        if (director == null || director.isBlank()) {
            model.addAttribute("directors", TOP_DIRECTORS);
            return "movies/universe-index";
        }
        model.addAttribute("directorName", director);
        return "movies/universe";
    }

    @GetMapping("/api/universe/director")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDirectorUniverse(@RequestParam String name) {
        Map<String, Object> universe = tmdbService.buildDirectorUniverse(name);
        return ResponseEntity.ok(universe);
    }

    @GetMapping("/api/search/directors")
    @ResponseBody
    public ResponseEntity<java.util.List<Map<String, Object>>> searchDirectors(@RequestParam String q) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(java.util.List.of());
        }
        return ResponseEntity.ok(tmdbService.searchDirectorsLive(q));
    }

    @GetMapping("/api/actor/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getActorDetails(@PathVariable Long id) {
        Map<String, Object> details = tmdbService.getActorDetails(id);
        return ResponseEntity.ok(details);
    }
}
