package com.example.automatizacion_gestion_respaldo.DTO;

import java.time.LocalDateTime;

public record SummaryResponse(
        LocalDateTime from,
        LocalDateTime to,
        long total,
        long exitosos,
        long advertencias,
        long errores,
        long omitidos,
        long enEjecucion
) {}