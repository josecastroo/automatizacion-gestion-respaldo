package com.example.automatizacion_gestion_respaldo.controller;

import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/databases")
@CrossOrigin(origins = "*") // Para desarrollo con React
public class DatabaseController {

    @GetMapping
    public ResponseEntity<List<DatabaseTarget>> getAllDatabases() {
        // TODO: Implementar llamada al servicio
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatabaseTarget> getDatabaseById(@PathVariable Long id) {
        // TODO: Implementar llamada al servicio
        return ResponseEntity.ok(new DatabaseTarget());
    }

    @PostMapping
    public ResponseEntity<DatabaseTarget> createDatabase(@RequestBody DatabaseTarget target) {
        // TODO: Implementar llamada al servicio
        return ResponseEntity.ok(target);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DatabaseTarget> updateDatabase(@PathVariable Long id, @RequestBody DatabaseTarget target) {
        // TODO: Implementar llamada al servicio
        return ResponseEntity.ok(target);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDatabase(@PathVariable Long id) {
        // TODO: Implementar llamada al servicio
        return ResponseEntity.noContent().build();
    }
}
