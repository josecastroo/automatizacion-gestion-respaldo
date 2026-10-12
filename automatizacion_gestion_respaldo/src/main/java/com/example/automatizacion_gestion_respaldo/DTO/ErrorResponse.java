package com.example.automatizacion_gestion_respaldo.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(LocalDateTime timestamp, int status, String message, List<String> details) {}