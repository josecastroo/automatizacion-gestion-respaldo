package com.example.automatizacion_gestion_respaldo.Service;

import com.example.automatizacion_gestion_respaldo.domain.*;
import com.example.automatizacion_gestion_respaldo.DTO.ExecutionDetailResponse;
import com.example.automatizacion_gestion_respaldo.DTO.SummaryResponse;
import com.example.automatizacion_gestion_respaldo.Exception.BusinessException;
import com.example.automatizacion_gestion_respaldo.Repository.ExecutionFileRepository;
import com.example.automatizacion_gestion_respaldo.Repository.ExecutionRepository;
import com.example.automatizacion_gestion_respaldo.Repository.ScheduleRepository;
import com.example.automatizacion_gestion_respaldo.Repository.StatusCount;
import com.example.automatizacion_gestion_respaldo.Repository.StrategyRepository;
import com.example.automatizacion_gestion_respaldo.rman.ExecutionResult;
import com.example.automatizacion_gestion_respaldo.rman.LogAnalyzer;
import com.example.automatizacion_gestion_respaldo.rman.RmanExecutor;
import com.example.automatizacion_gestion_respaldo.rman.RmanScriptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExecutionServiceTest {

    private static final String LOG_OK = "Finished backup\nRecovery Manager complete.\n";

    @Mock StrategyRepository strategyRepository;
    @Mock ScheduleRepository scheduleRepository;
    @Mock ExecutionRepository executionRepository;
    @Mock ExecutionFileRepository executionFileRepository;
    @Mock RmanExecutor executor;
    @Mock PlatformTransactionManager txManager;

    ExecutionService service;
    DatabaseTarget target;
    Execution stored;

    @BeforeEach
    void setUp() {
        service = new ExecutionService(strategyRepository, scheduleRepository, executionRepository,
                executionFileRepository, new RmanScriptBuilder(), executor, new LogAnalyzer(), txManager);

        target = new DatabaseTarget();
        target.setId(1L);
        target.setName("PROD");
        target.setActive(true);
        target.setArchiveMode(DatabaseTarget.ArchiveMode.ARCHIVELOG);

        when(executionRepository.save(any(Execution.class))).thenAnswer(inv -> {
            Execution e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(100L);
            }
            stored = e;
            return e;
        });
        when(executionRepository.findById(100L)).thenAnswer(inv -> Optional.ofNullable(stored));
        when(executionFileRepository.save(any(ExecutionFile.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- ayudas ----------

    private Strategy strategyWith(BackupType type) {
        Strategy s = new Strategy();
        s.setId(5L);
        s.setName("Diaria");
        s.setDatabaseTarget(target);
        s.setActive(true);
        s.setRetentionDays(7);
        s.setDestinationPath("C:/backups");
        StrategyComponent c = new StrategyComponent();
        c.setComponentType(ComponentType.DB);
        s.addComponent(c);
        StrategyBackupType b = new StrategyBackupType();
        b.setBackupType(type);
        b.setCompressed(false);
        s.addBackupType(b);
        when(strategyRepository.findById(5L)).thenReturn(Optional.of(s));
        return s;
    }

    private ExecutionResult result(int exitCode, String log, List<ExecutionResult.GeneratedFile> files) {
        LocalDateTime now = LocalDateTime.now();
        long total = files.stream().mapToLong(f -> f.sizeBytes() == null ? 0 : f.sizeBytes()).sum();
        return ExecutionResult.builder()
                .outputLog(log).exitCode(exitCode)
                .startTime(now.minusSeconds(30)).endTime(now).durationSeconds(30L)
                .backupLocation("C:/backups").totalSizeBytes(total).files(files)
                .build();
    }

    private ExecutionResult okResult() {
        return result(0, LOG_OK, List.of(new ExecutionResult.GeneratedFile("C:/backups/a.bkp", 1000L)));
    }

    private StatusCount count(ExecutionStatus status, long total) {
        return new StatusCount() {
            public ExecutionStatus getStatus() { return status; }
            public Long getTotal() { return total; }
        };
    }

    // ---------- ejecución ----------

    @Test
    void run_exitoso_guardaToda_laEvidencia() {
        strategyWith(BackupType.FULL);
        when(executor.execute(any(), anyString(), anyString())).thenReturn(okResult());

        ExecutionDetailResponse r = service.run(5L, BackupType.FULL, ExecutionOrigin.MANUAL, null);

        assertThat(r.status()).isEqualTo(ExecutionStatus.EXITOSO);
        assertThat(r.origin()).isEqualTo(ExecutionOrigin.MANUAL);
        assertThat(r.strategyName()).isEqualTo("Diaria");
        assertThat(r.databaseName()).isEqualTo("PROD");
        assertThat(r.usedScript()).contains("BACKUP DATABASE");
        assertThat(r.rmanLog()).contains("Recovery Manager complete");
        assertThat(r.files()).hasSize(1);
        assertThat(r.files().get(0).found()).isTrue();
        assertThat(r.durationSeconds()).isEqualTo(30L);
        verify(executionFileRepository, times(1)).save(any(ExecutionFile.class));
    }

    @Test
    void run_conErrorDeRman_quedaRegistradoComoError() {
        strategyWith(BackupType.FULL);
        String log = "RMAN-00569: =============== ERROR MESSAGE STACK FOLLOWS ===============\n"
                + "RMAN-03002: failure of backup command\n"
                + "ORA-19504: failed to create file\n"
                + "Recovery Manager complete.\n";
        when(executor.execute(any(), anyString(), anyString())).thenReturn(result(1, log, List.of()));

        ExecutionDetailResponse r = service.run(5L, BackupType.FULL, ExecutionOrigin.MANUAL, null);

        assertThat(r.status()).isEqualTo(ExecutionStatus.ERROR);
        assertThat(r.identifiedErrors()).contains("RMAN-03002").contains("ORA-19504");
        assertThat(r.rmanLog()).isEqualTo(log);
    }

    @Test
    void run_archivoReportadoQueNoExiste_pasaAAdvertencia() {
        strategyWith(BackupType.FULL);
        when(executor.execute(any(), anyString(), anyString())).thenReturn(result(0, LOG_OK,
                List.of(new ExecutionResult.GeneratedFile("C:/backups/fantasma.bkp", null))));

        ExecutionDetailResponse r = service.run(5L, BackupType.FULL, ExecutionOrigin.MANUAL, null);

        assertThat(r.status()).isEqualTo(ExecutionStatus.ADVERTENCIA);
        assertThat(r.identifiedErrors()).contains("no existe en disco");
        assertThat(r.files().get(0).found()).isFalse();
    }

    @Test
    void run_sinArchivosGenerados_pasaAAdvertencia() {
        strategyWith(BackupType.FULL);
        when(executor.execute(any(), anyString(), anyString())).thenReturn(result(0, LOG_OK, List.of()));

        ExecutionDetailResponse r = service.run(5L, BackupType.FULL, ExecutionOrigin.MANUAL, null);

        assertThat(r.status()).isEqualTo(ExecutionStatus.ADVERTENCIA);
    }

    @Test
    void run_respaldoDeArchivelogSinArchivosNuevos_sigueExitoso() {
        strategyWith(BackupType.ARCHIVELOG);
        when(executor.execute(any(), anyString(), anyString())).thenReturn(result(0, LOG_OK, List.of()));

        ExecutionDetailResponse r = service.run(5L, BackupType.ARCHIVELOG, ExecutionOrigin.PROGRAMADA, null);

        assertThat(r.status()).isEqualTo(ExecutionStatus.EXITOSO);
    }

    @Test
    void run_configuracionIncompleta_registraErrorYNoLlamaAlEjecutor() {
        Strategy s = strategyWith(BackupType.FULL);
        s.getComponents().clear();

        ExecutionDetailResponse r = service.run(5L, BackupType.FULL, ExecutionOrigin.MANUAL, null);

        assertThat(r.status()).isEqualTo(ExecutionStatus.ERROR);
        assertThat(r.rmanLog()).contains("No se pudo generar el script");
        verify(executor, never()).execute(any(), anyString(), anyString());
    }

    @Test
    void run_estrategiaInactiva_seRechazaSinRegistrar() {
        Strategy s = strategyWith(BackupType.FULL);
        s.setActive(false);

        assertThatThrownBy(() -> service.run(5L, BackupType.FULL, ExecutionOrigin.MANUAL, null))
                .isInstanceOf(BusinessException.class);
        verify(executionRepository, never()).save(any(Execution.class));
        verify(executor, never()).execute(any(), anyString(), anyString());
    }

    @Test
    void run_siElEjecutorLanzaExcepcion_registraError() {
        strategyWith(BackupType.FULL);
        when(executor.execute(any(), anyString(), anyString())).thenThrow(new RuntimeException("boom"));

        ExecutionDetailResponse r = service.run(5L, BackupType.FULL, ExecutionOrigin.MANUAL, null);

        assertThat(r.status()).isEqualTo(ExecutionStatus.ERROR);
        assertThat(r.rmanLog()).contains("boom");
    }

    @Test
    void run_noPermiteDosEjecucionesSimultaneasDeLaMismaEstrategia() {
        strategyWith(BackupType.FULL);
        when(executor.execute(any(), anyString(), anyString())).thenAnswer(inv -> {
            // mientras corre el respaldo, un segundo intento debe rechazarse
            assertThatThrownBy(() -> service.run(5L, BackupType.FULL, ExecutionOrigin.MANUAL, null))
                    .isInstanceOf(BusinessException.class);
            return okResult();
        });

        ExecutionDetailResponse r = service.run(5L, BackupType.FULL, ExecutionOrigin.MANUAL, null);

        assertThat(r.status()).isEqualTo(ExecutionStatus.EXITOSO);
    }

    @Test
    void run_liberaElBloqueoAlTerminar() {
        strategyWith(BackupType.FULL);
        when(executor.execute(any(), anyString(), anyString())).thenReturn(okResult());

        service.run(5L, BackupType.FULL, ExecutionOrigin.MANUAL, null);
        ExecutionDetailResponse second = service.run(5L, BackupType.FULL, ExecutionOrigin.MANUAL, null);

        assertThat(second.status()).isEqualTo(ExecutionStatus.EXITOSO);
    }

    // ---------- omitidas, interrumpidas y resumen ----------

    @Test
    void registerSkipped_dejaEvidenciaDeLaOmision() {
        Strategy s = strategyWith(BackupType.FULL);
        Schedule sc = new Schedule();
        sc.setId(9L);
        sc.setBackupType(s.getBackupTypes().get(0));
        sc.setCronExpression("0 0 23 * * *");
        s.addSchedule(sc);
        when(scheduleRepository.findById(9L)).thenReturn(Optional.of(sc));

        ExecutionDetailResponse r = service.registerSkipped(9L, LocalDateTime.now().minusMinutes(5),
                "La aplicación estaba detenida a la hora programada");

        assertThat(r.status()).isEqualTo(ExecutionStatus.OMITIDO);
        assertThat(r.origin()).isEqualTo(ExecutionOrigin.PROGRAMADA);
        assertThat(r.identifiedErrors()).contains("detenida");
        verify(executor, never()).execute(any(), anyString(), anyString());
    }

    @Test
    void recoverInterrupted_marcaLasEnCursoComoError() {
        Execution e = new Execution();
        e.setId(7L);
        e.setStatus(ExecutionStatus.EN_EJECUCION);
        e.setStartTime(LocalDateTime.now().minusMinutes(5));
        when(executionRepository.findByStatus(ExecutionStatus.EN_EJECUCION)).thenReturn(List.of(e));

        service.recoverInterrupted();

        assertThat(e.getStatus()).isEqualTo(ExecutionStatus.ERROR);
        assertThat(e.getIdentifiedErrors()).contains("interrumpida");
        assertThat(e.getEndTime()).isNotNull();
    }

    @Test
    void summary_cuentaPorEstado() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        when(executionRepository.summaryByStatus(from, to)).thenReturn(List.of(
                count(ExecutionStatus.EXITOSO, 8), count(ExecutionStatus.ERROR, 2),
                count(ExecutionStatus.OMITIDO, 1)));

        SummaryResponse s = service.summary(from, to);

        assertThat(s.total()).isEqualTo(11);
        assertThat(s.exitosos()).isEqualTo(8);
        assertThat(s.errores()).isEqualTo(2);
        assertThat(s.omitidos()).isEqualTo(1);
        assertThat(s.advertencias()).isZero();
    }
}