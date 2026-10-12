package com.example.automatizacion_gestion_respaldo.DTO;

import com.example.automatizacion_gestion_respaldo.domain.BackupType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BackupTypeDto(
        @NotNull BackupType backupType,
        Boolean compressed,
        @Size(max = 255) String parameters
) {}