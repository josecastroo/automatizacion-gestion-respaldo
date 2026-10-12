package com.example.automatizacion_gestion_respaldo.DTO;

/** found = false significa que RMAN reportó el archivo pero no se encontró en disco. */
public record ExecutionFileDto(String path, Long sizeBytes, boolean found) {}