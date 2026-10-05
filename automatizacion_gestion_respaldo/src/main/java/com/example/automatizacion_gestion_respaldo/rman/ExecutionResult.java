package com.example.automatizacion_gestion_respaldo.rman;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
public class ExecutionResult {
    private String outputLog;
    private int exitCode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationSeconds;
    private String backupLocation;
    private Long totalSizeBytes;

    @Builder.Default
    private List<GeneratedFile> files = new ArrayList<>();

    public record GeneratedFile(String path, Long sizeBytes) {}
}