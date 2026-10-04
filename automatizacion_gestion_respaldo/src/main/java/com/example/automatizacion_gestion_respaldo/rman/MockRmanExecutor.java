package com.example.automatizacion_gestion_respaldo.rman;

import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Ejecutor simulado para desarrollo y pruebas (Dry-run).
 * No requiere una instalación real de Oracle ni RMAN.
 */
@Component
public class MockRmanExecutor implements RmanExecutor {

    @Override
    public ExecutionResult execute(DatabaseTarget target, String scriptContent) {
        System.out.println("--- MOCK RMAN EXECUTION START ---");
        System.out.println("Target: " + target.getName() + " (" + target.getHost() + ")");
        System.out.println("Script:\n" + scriptContent);
        
        long duration = (long) (Math.random() * 120) + 10; // 10 a 130 segundos
        
        try {
            Thread.sleep(100); // Simulando algo de tiempo
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        StringBuilder mockLog = new StringBuilder();
        mockLog.append("Recovery Manager: Release 19.0.0.0.0 - Production\n");
        mockLog.append("connected to target database: ").append(target.getServiceName()).append("\n");
        mockLog.append("executing command: ...\n");
        
        int exitCode = 0;
        
        // Simular un error aleatorio (1 de cada 10 veces)
        if (new Random().nextInt(10) == 0) {
            mockLog.append("RMAN-03002: failure of backup command\n");
            mockLog.append("ORA-19504: failed to create file\n");
            exitCode = 1;
        } else {
            mockLog.append("Finished backup at ").append(java.time.LocalDateTime.now()).append("\n");
        }

        System.out.println("--- MOCK RMAN EXECUTION END ---");

        return ExecutionResult.builder()
                .outputLog(mockLog.toString())
                .exitCode(exitCode)
                .durationSeconds(duration)
                .backupLocation("/u01/app/oracle/fast_recovery_area/" + target.getServiceName())
                .totalSizeBytes(1024L * 1024L * (new Random().nextInt(500) + 100)) // 100MB a 600MB
                .build();
    }
}
