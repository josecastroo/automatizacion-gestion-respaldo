package com.example.automatizacion_gestion_respaldo.rman;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ExecutionResult {
    private String outputLog;
    private int exitCode;
    private Long durationSeconds;
    private String backupLocation;
    private Long totalSizeBytes;
}
