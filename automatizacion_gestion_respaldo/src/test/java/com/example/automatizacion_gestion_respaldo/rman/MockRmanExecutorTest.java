package com.example.automatizacion_gestion_respaldo.rman;

import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import com.example.automatizacion_gestion_respaldo.domain.ExecutionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockRmanExecutorTest {

    private final MockRmanExecutor executor = new MockRmanExecutor();
    private final LogAnalyzer analyzer = new LogAnalyzer();

    private DatabaseTarget target(String name) {
        DatabaseTarget t = new DatabaseTarget();
        t.setName(name);
        t.setServiceName("XEPDB1");
        return t;
    }

    @Test
    void baseNormal_terminaExitosa_conArchivosGenerados() {
        ExecutionResult r = executor.execute(target("PRODUCCION"), "BACKUP DATABASE;", "C:/backups");

        assertThat(analyzer.determineStatus(r.getExitCode(), r.getOutputLog()))
                .isEqualTo(ExecutionStatus.EXITOSO);
        assertThat(r.getFiles()).isNotEmpty();
        assertThat(r.getTotalSizeBytes()).isPositive();
        assertThat(r.getBackupLocation()).isEqualTo("C:/backups");
    }

    @Test
    void baseConError_enElNombre_siempreFalla() {
        ExecutionResult r = executor.execute(target("PRUEBA_ERROR"), "BACKUP DATABASE;", "C:/backups");

        assertThat(analyzer.determineStatus(r.getExitCode(), r.getOutputLog()))
                .isEqualTo(ExecutionStatus.ERROR);
        assertThat(analyzer.extractErrors(r.getOutputLog())).contains("ORA-19504");
        assertThat(r.getFiles()).isEmpty();
    }

    @Test
    void baseConWarn_enElNombre_terminaConAdvertencia() {
        ExecutionResult r = executor.execute(target("PRUEBA_WARN"), "BACKUP DATABASE;", "C:/backups");

        assertThat(analyzer.determineStatus(r.getExitCode(), r.getOutputLog()))
                .isEqualTo(ExecutionStatus.ADVERTENCIA);
    }
}