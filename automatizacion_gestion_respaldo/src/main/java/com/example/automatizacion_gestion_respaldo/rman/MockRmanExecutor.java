package com.example.automatizacion_gestion_respaldo.rman;

import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ejecutor simulado (dry-run). No requiere Oracle ni RMAN.
 * - Base con "ERROR" en el nombre: siempre falla.
 * - Base con "WARN" en el nombre: termina con advertencia.
 * - Resto: falla con probabilidad rman.mock.failure-rate (0 por defecto).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rman.executor.mode", havingValue = "mock", matchIfMissing = true)
public class MockRmanExecutor implements RmanExecutor {

    @Value("${rman.mock.failure-rate:0.0}")
    private double failureRate;

    @Override
    public ExecutionResult execute(DatabaseTarget target, String scriptContent, String destinationPath) {
        log.info("[MOCK RMAN] Objetivo: {} (servicio {})", target.getName(), target.getServiceName());
        log.debug("[MOCK RMAN] Script:\n{}", scriptContent);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        long duration = random.nextLong(10, 131);
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusSeconds(duration);

        String name = target.getName() == null ? "" : target.getName().toUpperCase();
        boolean warn = name.contains("WARN");
        boolean fail = name.contains("ERROR") || (!warn && random.nextDouble() < failureRate);

        String base = destinationPath == null ? "" : destinationPath.replaceAll("[/\\\\]+$", "");
        String sep = base.contains("\\") ? "\\" : "/";

        StringBuilder out = new StringBuilder();
        out.append("Recovery Manager: Release 21.0.0.0.0 - Production (SIMULADO)\n");
        out.append("connected to target database: ").append(target.getServiceName()).append("\n");
        out.append("executing script...\n");

        int exitCode = 0;
        List<ExecutionResult.GeneratedFile> files = new ArrayList<>();
        long total = 0L;

        if (fail) {
            exitCode = 1;
            out.append("RMAN-00571: ===========================================================\n");
            out.append("RMAN-00569: =============== ERROR MESSAGE STACK FOLLOWS ===============\n");
            out.append("RMAN-00571: ===========================================================\n");
            out.append("RMAN-03002: failure of backup command\n");
            out.append("ORA-19504: failed to create file\n");
        } else {
            if (warn) {
                out.append("RMAN-06207: WARNING: 1 objects could not be deleted for DISK channel(s)\n");
            }
            int count = random.nextInt(1, 4);
            for (int i = 1; i <= count; i++) {
                long size = 1024L * 1024L * random.nextLong(50, 300);
                files.add(new ExecutionResult.GeneratedFile(
                        base + sep + target.getServiceName() + "_mock_" + i + ".bkp", size));
                total += size;
            }
            out.append("Finished backup at ").append(end).append("\n");
        }
        out.append("Recovery Manager complete.\n");

        return ExecutionResult.builder()
                .outputLog(out.toString())
                .exitCode(exitCode)
                .startTime(start)
                .endTime(end)
                .durationSeconds(duration)
                .backupLocation(base)
                .totalSizeBytes(total)
                .files(files)
                .build();
    }
}