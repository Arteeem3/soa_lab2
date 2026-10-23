package ru.itmo.soa.starship.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.soa.starship.service.StarshipService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/starship")
@RequiredArgsConstructor
public class StarshipController {

    private final StarshipService starshipService;

    @PostMapping("/{starship-id}/unload/{space-marine-id}")
    public ResponseEntity<Map<String, Object>> unloadMarine(
            @PathVariable("starship-id") Long starshipId,
            @PathVariable("space-marine-id") Integer spaceMarineId
    ) {
        return ResponseEntity.ok(starshipService.unloadMarine(starshipId, spaceMarineId));
    }

    @PostMapping("/{starship-id}/unload-all")
    public ResponseEntity<Map<String, Object>> unloadAll(
            @PathVariable("starship-id") Long starshipId
    ) {
        return ResponseEntity.ok(starshipService.unloadAll(starshipId));
    }
}
