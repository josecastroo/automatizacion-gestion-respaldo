package com.example.automatizacion_gestion_respaldo.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record StrategyRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long databaseTargetId,
        @NotNull @Min(1) Integer retentionDays,
        @Size(max = 500) String description,
        @Size(max = 100) String responsible,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_\\-./\\\\: ]{1,255}$",
                message = "Ruta de destino inválida") String destinationPath,
        Boolean active,
        @Valid List<ComponentDto> components,
        @Valid List<BackupTypeDto> backupTypes
) {}