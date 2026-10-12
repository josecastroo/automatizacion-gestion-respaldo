package com.example.automatizacion_gestion_respaldo.DTO;

import com.example.automatizacion_gestion_respaldo.domain.BackupType;

import java.time.LocalTime;

public record ScheduleResponse(
        Long id,
        BackupType backupType,
        String cronExpression,
        LocalTime windowStart,
        LocalTime windowEnd,
        Boolean active
) {}