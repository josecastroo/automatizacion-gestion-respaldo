package com.example.automatizacion_gestion_respaldo.DTO;

import com.example.automatizacion_gestion_respaldo.domain.BackupType;
import com.example.automatizacion_gestion_respaldo.domain.ExecutionOrigin;
import com.example.automatizacion_gestion_respaldo.domain.ExecutionStatus;

import java.time.LocalDateTime;

public record ExecutionSummaryResponse(
        Long id,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long durationSeconds,
        String strategyName,
        String databaseName,
        BackupType backupType,
        ExecutionOrigin origin,
        ExecutionStatus status,
        boolean archived
) {}