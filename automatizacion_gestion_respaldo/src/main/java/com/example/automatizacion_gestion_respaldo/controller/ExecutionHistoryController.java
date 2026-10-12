package com.example.automatizacion_gestion_respaldo.controller;

import com.example.automatizacion_gestion_respaldo.DTO.*;
import com.example.automatizacion_gestion_respaldo.Service.ExecutionService;
import com.example.automatizacion_gestion_respaldo.Service.ExportService;
import com.example.automatizacion_gestion_respaldo.domain.ExecutionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Historial de ejecuciones. No hay DELETE a propósito: una ejecución es evidencia y solo se archiva.
 */
@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class ExecutionHistoryController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ExecutionService executionService;
    private final ExportService exportService;

    @GetMapping
    public PageResponse<ExecutionSummaryResponse> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String databaseName,
            @RequestParam(required = false) Long strategyId,
            @RequestParam(required = false) ExecutionStatus status,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "startTime"));
        return PageResponse.from(executionService.search(
                filter(from, to, databaseName, strategyId, status, archived), pageable));
    }

    @GetMapping("/{id}")
    public ExecutionDetailResponse detail(@PathVariable Long id) {
        return executionService.detail(id);
    }

    @PatchMapping("/{id}/archived")
    public ExecutionSummaryResponse setArchived(@PathVariable Long id, @RequestParam boolean archived) {
        return executionService.setArchived(id, archived);
    }

    /** Resumen por período (por defecto, los últimos 30 días). */
    @GetMapping("/summary")
    public SummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        return executionService.summary(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String databaseName,
            @RequestParam(required = false) Long strategyId,
            @RequestParam(required = false) ExecutionStatus status,
            @RequestParam(required = false) Boolean archived) {

        List<ExecutionSummaryResponse> rows = executionService.searchAll(
                filter(from, to, databaseName, strategyId, status, archived));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment("csv"))
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(exportService.toCsv(rows));
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String databaseName,
            @RequestParam(required = false) Long strategyId,
            @RequestParam(required = false) ExecutionStatus status,
            @RequestParam(required = false) Boolean archived) {

        List<ExecutionSummaryResponse> rows = executionService.searchAll(
                filter(from, to, databaseName, strategyId, status, archived));
        byte[] pdf = exportService.toPdf(rows, describe(from, to, databaseName, strategyId, status));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment("pdf"))
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ------------------------------------------------------------ utilidades

    private ExecutionFilter filter(LocalDate from, LocalDate to, String databaseName,
                                   Long strategyId, ExecutionStatus status, Boolean archived) {
        return new ExecutionFilter(
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(), // "hasta" es inclusivo
                databaseName, strategyId, status, archived);
    }

    private String describe(LocalDate from, LocalDate to, String databaseName, Long strategyId, ExecutionStatus status) {
        List<String> parts = new ArrayList<>();
        if (from != null) parts.add("desde " + from);
        if (to != null) parts.add("hasta " + to);
        if (databaseName != null && !databaseName.isBlank()) parts.add("base " + databaseName);
        if (strategyId != null) parts.add("estrategia #" + strategyId);
        if (status != null) parts.add("resultado " + status);
        return parts.isEmpty() ? "" : "Filtros: " + String.join("; ", parts);
    }

    private String attachment(String extension) {
        return "attachment; filename=\"historial_respaldos_" + LocalDate.now() + "." + extension + "\"";
    }
}