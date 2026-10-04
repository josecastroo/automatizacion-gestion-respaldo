package com.example.automatizacion_gestion_respaldo.controller;

import com.example.automatizacion_gestion_respaldo.domain.Execution;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/executions")
@CrossOrigin(origins = "*")
public class ExecutionHistoryController {

    @GetMapping
    public ResponseEntity<List<Execution>> getExecutionHistory(
            @RequestParam(required = false) Long strategyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        // TODO: Implementar filtros dinámicos con JPA Criteria o Spring Data
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Execution> getExecutionDetail(@PathVariable Long id) {
        // TODO: Implementar detalle
        return ResponseEntity.ok(new Execution());
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportToCsv() {
        // TODO: Generar archivo CSV de ejecuciones
        return ResponseEntity.ok(new byte[0]);
    }
    
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPdf() {
        // TODO: Generar reporte PDF con resúmenes
        return ResponseEntity.ok(new byte[0]);
    }
}
