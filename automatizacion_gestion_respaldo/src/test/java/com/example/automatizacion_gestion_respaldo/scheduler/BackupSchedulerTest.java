package com.example.automatizacion_gestion_respaldo.scheduler;

import com.example.automatizacion_gestion_respaldo.domain.*;
import com.example.automatizacion_gestion_respaldo.Exception.BusinessException;
import com.example.automatizacion_gestion_respaldo.Repository.ExecutionRepository;
import com.example.automatizacion_gestion_respaldo.Repository.ScheduleRepository;
import com.example.automatizacion_gestion_respaldo.Service.ExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BackupSchedulerTest {

    @Mock ScheduleRepository scheduleRepository;
    @Mock ExecutionRepository executionRepository;
    @Mock ExecutionService executionService;

    // ---------- ayudas ----------

    private void schedule(String cron, LocalTime windowStart, LocalTime windowEnd, LocalDateTime trackedSince) {
        Strategy strategy = new Strategy();
        strategy.setId(5L);
        StrategyBackupType backupType = new StrategyBackupType();
        backupType.setBackupType(BackupType.FULL);

        Schedule sc = new Schedule();
        sc.setId(9L);
        sc.setStrategy(strategy);
        sc.setBackupType(backupType);
        sc.setCronExpression(cron);
        sc.setWindowStart(windowStart);
        sc.setWindowEnd(windowEnd);
        sc.setActive(true);
        sc.setTrackedSince(trackedSince);
        when(scheduleRepository.findAllActiveWithStrategy()).thenReturn(List.of(sc));
    }

    private BackupScheduler scheduler(Executor executor) {
        return new BackupScheduler(scheduleRepository, executionRepository, executionService, executor, 10, 24);
    }

    private static LocalDateTime at(int day, int hour, int minute, int second) {
        return LocalDateTime.of(2026, 10, day, hour, minute, second);
    }

    // ---------- pruebas ----------

    @Test
    void dispara_cuandoLlegaLaHora() {
        schedule("0 0 23 * * *", null, null, at(7, 20, 0, 0));

        scheduler(Runnable::run).evaluate(at(7, 23, 0, 3));

        verify(executionService).run(5L, BackupType.FULL, ExecutionOrigin.PROGRAMADA, 9L);
        verify(executionService, never()).registerSkipped(any(), any(), anyString());
    }

    @Test
    void noRepite_siLaHoraYaFueAtendida() {
        schedule("0 0 23 * * *", null, null, at(7, 20, 0, 0));
        when(executionRepository.existsByScheduleIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
                eq(9L), any(), any())).thenReturn(true);

        scheduler(Runnable::run).evaluate(at(7, 23, 0, 3));

        verify(executionService, never()).run(any(), any(), any(), any());
        verify(executionService, never()).registerSkipped(any(), any(), anyString());
    }

    @Test
    void fueraDeLaVentana_registraOmitido() {
        schedule("0 0 10 * * *", LocalTime.of(23, 0), LocalTime.of(4, 0), at(7, 8, 0, 0));

        scheduler(Runnable::run).evaluate(at(7, 10, 0, 2));

        verify(executionService).registerSkipped(eq(9L), eq(at(7, 10, 0, 0)), contains("ventana"));
        verify(executionService, never()).run(any(), any(), any(), any());
    }

    @Test
    void retrasoDentroDeUnaVentanaNocturna_sigueEjecutando() {
        schedule("0 0 23 * * *", LocalTime.of(23, 0), LocalTime.of(4, 0), at(7, 20, 0, 0));

        scheduler(Runnable::run).evaluate(at(8, 1, 30, 0));

        verify(executionService).run(5L, BackupType.FULL, ExecutionOrigin.PROGRAMADA, 9L);
        verify(executionService, never()).registerSkipped(any(), any(), anyString());
    }

    @Test
    void sinVentana_ySuperadaLaGracia_registraOmitido() {
        schedule("0 0 23 * * *", null, null, at(7, 20, 0, 0));

        scheduler(Runnable::run).evaluate(at(7, 23, 30, 0));

        verify(executionService).registerSkipped(eq(9L), eq(at(7, 23, 0, 0)), contains("a tiempo"));
        verify(executionService, never()).run(any(), any(), any(), any());
    }

    @Test
    void siLaEjecucionEsRechazada_registraOmitido() {
        schedule("0 0 23 * * *", null, null, at(7, 20, 0, 0));
        when(executionService.run(5L, BackupType.FULL, ExecutionOrigin.PROGRAMADA, 9L))
                .thenThrow(new BusinessException("Ya hay una ejecución en curso para esta estrategia"));

        scheduler(Runnable::run).evaluate(at(7, 23, 0, 3));

        verify(executionService).registerSkipped(eq(9L), eq(at(7, 23, 0, 0)), contains("en curso"));
    }

    @Test
    void variasHorasPerdidas_seRegistranYSeEjecutaLaUltima() {
        schedule("0 0 * * * *", null, null, at(7, 20, 30, 0));

        scheduler(Runnable::run).evaluate(at(7, 23, 0, 2));

        verify(executionService).registerSkipped(eq(9L), eq(at(7, 21, 0, 0)), anyString());
        verify(executionService).registerSkipped(eq(9L), eq(at(7, 22, 0, 0)), anyString());
        verify(executionService, times(2)).registerSkipped(eq(9L), any(), anyString());
        verify(executionService, times(1)).run(5L, BackupType.FULL, ExecutionOrigin.PROGRAMADA, 9L);
    }

    @Test
    void noEncolaDosVecesLaMismaHora() {
        schedule("0 0 23 * * *", null, null, at(7, 20, 0, 0));
        List<Runnable> queue = new ArrayList<>();
        BackupScheduler scheduler = scheduler(queue::add); // cola que no ejecuta nada

        scheduler.evaluate(at(7, 23, 0, 3));
        scheduler.evaluate(at(7, 23, 1, 3));

        assertThat(queue).hasSize(1);
    }

    @Test
    void cronInvalido_seIgnoraSinFallar() {
        schedule("esto no es un cron", null, null, at(7, 20, 0, 0));

        assertDoesNotThrow(() -> scheduler(Runnable::run).evaluate(at(7, 23, 0, 3)));

        verify(executionService, never()).run(any(), any(), any(), any());
        verify(executionService, never()).registerSkipped(any(), any(), anyString());
    }
}