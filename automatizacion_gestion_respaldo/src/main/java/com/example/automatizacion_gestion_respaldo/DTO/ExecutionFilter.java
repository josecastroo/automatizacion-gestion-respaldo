package com.example.automatizacion_gestion_respaldo.DTO;

import com.example.automatizacion_gestion_respaldo.domain.ExecutionStatus;

import java.time.LocalDateTime;

/** Todos los campos son opcionales; los nulos no filtran. */
public record ExecutionFilter(
        LocalDateTime from,
        LocalDateTime to,
        String databaseName,
        Long strategyId,
        ExecutionStatus status,
        Boolean archived
) {}