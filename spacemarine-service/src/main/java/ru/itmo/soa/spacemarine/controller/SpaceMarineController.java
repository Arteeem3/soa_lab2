package ru.itmo.soa.spacemarine.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.soa.common.dto.SpaceMarineDto;
import ru.itmo.soa.common.dto.SpaceMarineInputDto;
import ru.itmo.soa.common.dto.SpaceMarinesResponse;
import ru.itmo.soa.spacemarine.service.SpaceMarineService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/space-marines")
@RequiredArgsConstructor
public class SpaceMarineController {

    private final SpaceMarineService service;

    @GetMapping("/health/average")
    public ResponseEntity<Double> getAverageHealth() {
        return ResponseEntity.ok(service.getAverageHealth());
    }

    @GetMapping("/search/by-name")
    public ResponseEntity<List<SpaceMarineDto>> searchByName(@RequestParam("substring") String substring) {
        return ResponseEntity.ok(service.searchByName(substring));
    }

    @GetMapping("/health/greater-than")
    public ResponseEntity<List<SpaceMarineDto>> getByHealthGreaterThan(@RequestParam("threshold") Double threshold) {
        return ResponseEntity.ok(service.getByHealthGreaterThan(threshold));
    }

    @GetMapping
    public ResponseEntity<SpaceMarinesResponse> getAll(
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "asc") String sortOrder,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Map<String, String> allParams
    ) {
        Map<String, String> filters = new HashMap<>(allParams != null ? allParams : Collections.emptyMap());
        filters.remove("sortBy");
        filters.remove("sortOrder");
        filters.remove("page");
        filters.remove("pageSize");

        return ResponseEntity.ok(service.getAll(filters, sortBy, sortOrder, page, pageSize));
    }

    @PostMapping
    public ResponseEntity<SpaceMarineDto> create(@RequestBody SpaceMarineInputDto inputDto) {
        SpaceMarineDto created = service.create(inputDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpaceMarineDto> getById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpaceMarineDto> update(
            @PathVariable("id") Integer id,
            @RequestBody SpaceMarineInputDto inputDto
    ) {
        return ResponseEntity.ok(service.update(id, inputDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
