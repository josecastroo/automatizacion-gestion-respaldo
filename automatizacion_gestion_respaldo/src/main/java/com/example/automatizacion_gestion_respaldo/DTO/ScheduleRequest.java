package com.example.automatizacion_gestion_respaldo.DTO;

import com.example.automatizacion_gestion_respaldo.domain.BackupType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ScheduleRequest(
        @NotNull BackupType backupType,
        @NotBlank String cronExpression,   // 6 campos: seg min hora día mes díaSemana
        LocalTime windowStart,
        LocalTime windowEnd,
        Boolean active
) {}