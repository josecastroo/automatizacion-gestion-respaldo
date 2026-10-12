package com.example.automatizacion_gestion_respaldo.DTO;

import java.util.List;

public record StrategyResponse(
        Long id,
        String name,
        Long databaseTargetId,
        String databaseName,
        Integer retentionDays,
        String description,
        String responsible,
        String destinationPath,
        Boolean active,
        List<ComponentDto> components,
        List<BackupTypeDto> backupTypes,
        List<ScheduleResponse> schedules,
        List<Notice> notices
) {}