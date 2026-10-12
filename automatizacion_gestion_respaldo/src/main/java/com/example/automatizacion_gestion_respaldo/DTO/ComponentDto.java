package com.example.automatizacion_gestion_respaldo.DTO;

import com.example.automatizacion_gestion_respaldo.domain.ComponentType;
import com.example.automatizacion_gestion_respaldo.domain.Priority;
import jakarta.validation.constraints.NotNull;

public record ComponentDto(
        @NotNull ComponentType componentType,
        String objectName,   // TABLESPACE: nombre. DATAFILE: número. Resto: se ignora.
        Priority priority    // si es nulo, hereda la prioridad de la base de datos
) {}