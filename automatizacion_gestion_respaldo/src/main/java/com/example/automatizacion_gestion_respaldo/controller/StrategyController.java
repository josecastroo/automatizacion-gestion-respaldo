package com.example.automatizacion_gestion_respaldo.controller;

import com.example.automatizacion_gestion_respaldo.DTO.*;
import com.example.automatizacion_gestion_respaldo.Service.ExecutionService;
import com.example.automatizacion_gestion_respaldo.Service.StrategyService;
import com.example.automatizacion_gestion_respaldo.domain.BackupType;
import com.example.automatizacion_gestion_respaldo.domain.ExecutionOrigin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyService strategyService;
    private final ExecutionService executionService;

    // ------------------------------------------------------------ estrategias

    @GetMapping
    public List<StrategyResponse> list() {
        return strategyService.list();
    }

    @GetMapping("/{id}")
    public StrategyResponse get(@PathVariable Long id) {
        return strategyService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StrategyResponse create(@Valid @RequestBody StrategyRequest request) {
        return strategyService.create(request);
    }

    @PutMapping("/{id}")
    public StrategyResponse update(@PathVariable Long id, @Valid @RequestBody StrategyRequest request) {
        return strategyService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public StrategyResponse setActive(@PathVariable Long id, @RequestParam boolean active) {
        return strategyService.setActive(id, active);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        strategyService.delete(id);
    }

    // ------------------------------------------------------------ script y ejecución manual

    /** Vista previa del script RMAN que generaría la estrategia, sin ejecutarlo. */
    @GetMapping("/{id}/script")
    public ScriptPreviewResponse previewScript(@PathVariable Long id, @RequestParam BackupType backupType) {
        return new ScriptPreviewResponse(id, backupType, strategyService.previewScript(id, backupType));
    }

    /** Ejecución manual: la petición responde cuando termina el respaldo, con la evidencia completa. */
    @PostMapping("/{id}/run")
    public ExecutionDetailResponse run(@PathVariable Long id, @RequestParam BackupType backupType) {
        return executionService.run(id, backupType, ExecutionOrigin.MANUAL, null);
    }

    // ------------------------------------------------------------ programaciones (cuándo)

    @PostMapping("/{id}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse addSchedule(@PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        return strategyService.addSchedule(id, request);
    }

    @PutMapping("/{id}/schedules/{scheduleId}")
    public ScheduleResponse updateSchedule(@PathVariable Long id, @PathVariable Long scheduleId,
                                           @Valid @RequestBody ScheduleRequest request) {
        return strategyService.updateSchedule(id, scheduleId, request);
    }

    @PatchMapping("/{id}/schedules/{scheduleId}/active")
    public ScheduleResponse setScheduleActive(@PathVariable Long id, @PathVariable Long scheduleId,
                                              @RequestParam boolean active) {
        return strategyService.setScheduleActive(id, scheduleId, active);
    }

    @DeleteMapping("/{id}/schedules/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSchedule(@PathVariable Long id, @PathVariable Long scheduleId) {
        strategyService.deleteSchedule(id, scheduleId);
    }
}