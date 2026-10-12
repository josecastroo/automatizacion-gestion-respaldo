package com.example.automatizacion_gestion_respaldo.controller;

import com.example.automatizacion_gestion_respaldo.DTO.DatabaseTargetRequest;
import com.example.automatizacion_gestion_respaldo.DTO.DatabaseTargetResponse;
import com.example.automatizacion_gestion_respaldo.Service.ArchiveModeDetectorService;
import com.example.automatizacion_gestion_respaldo.Service.DatabaseTargetService;
import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/databases")
@RequiredArgsConstructor
public class DatabaseController {

    private final DatabaseTargetService service;
    private final ArchiveModeDetectorService detectorService;

    @GetMapping
    public List<DatabaseTargetResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public DatabaseTargetResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DatabaseTargetResponse create(@Valid @RequestBody DatabaseTargetRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public DatabaseTargetResponse update(@PathVariable Long id, @Valid @RequestBody DatabaseTargetRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public DatabaseTargetResponse setActive(@PathVariable Long id, @RequestParam boolean active) {
        return service.setActive(id, active);
    }

    @PatchMapping("/{id}/archive-mode")
    public DatabaseTargetResponse updateArchiveMode(@PathVariable Long id,
                                                    @RequestParam DatabaseTarget.ArchiveMode mode) {
        return service.updateArchiveMode(id, mode);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/detect-archive-mode")
    public DatabaseTargetResponse detectArchiveMode(@PathVariable Long id) {
        // Ejecuta la detección real
        detectorService.detectAndUpdateMode(id);
        // Retorna la base de datos actualizada usando la variable correcta: "service"
        return service.get(id);
    }
}