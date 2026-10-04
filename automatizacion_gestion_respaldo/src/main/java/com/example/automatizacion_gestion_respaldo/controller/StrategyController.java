package com.example.automatizacion_gestion_respaldo.controller;

import com.example.automatizacion_gestion_respaldo.domain.Strategy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/strategies")
@CrossOrigin(origins = "*")
public class StrategyController {

    @GetMapping
    public ResponseEntity<List<Strategy>> getAllStrategies() {
        // TODO: Implementar llamada al servicio
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Strategy> getStrategyById(@PathVariable Long id) {
        // TODO: Implementar llamada al servicio
        return ResponseEntity.ok(new Strategy());
    }

    @PostMapping
    public ResponseEntity<Strategy> createStrategy(@RequestBody Strategy strategy) {
        // TODO: Implementar llamada al servicio
        return ResponseEntity.ok(strategy);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Strategy> updateStrategy(@PathVariable Long id, @RequestBody Strategy strategy) {
        // TODO: Implementar llamada al servicio
        return ResponseEntity.ok(strategy);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateStrategy(@PathVariable Long id) {
        // TODO: Cambiar el estado 'active' a true
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateStrategy(@PathVariable Long id) {
        // TODO: Cambiar el estado 'active' a false
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/execute-manual")
    public ResponseEntity<Void> executeManual(@PathVariable Long id) {
        // TODO: Orquestar ejecución manual inmediata
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/preview-script")
    public ResponseEntity<String> previewScript(@PathVariable Long id) {
        // TODO: Usar RmanScriptBuilder para generar previsualización
        return ResponseEntity.ok("BACKUP DATABASE;"); 
    }
}
