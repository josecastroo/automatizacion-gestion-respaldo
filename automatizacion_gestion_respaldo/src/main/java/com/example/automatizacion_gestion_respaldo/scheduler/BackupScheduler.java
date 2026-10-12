package com.example.automatizacion_gestion_respaldo.scheduler;

import com.example.automatizacion_gestion_respaldo.domain.BackupType;
import com.example.automatizacion_gestion_respaldo.domain.ExecutionOrigin;
import com.example.automatizacion_gestion_respaldo.domain.Schedule;
import com.example.automatizacion_gestion_respaldo.Exception.BusinessException;
import com.example.automatizacion_gestion_respaldo.Exception.NotFoundException;
import com.example.automatizacion_gestion_respaldo.Repository.ExecutionRepository;
import com.example.automatizacion_gestion_respaldo.Repository.ScheduleRepository;
import com.example.automatizacion_gestion_respaldo.Service.ExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Decide qué respaldo corresponde en cada momento, lo activa y deja registrado lo que se omitió.
 * Revisa cada minuto las horas de disparo de cada programación desde que se vigila (máximo lookback).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class BackupScheduler {

    private static final int MAX_CRON_STEPS = 10_000;
    private static final int MAX_FIRES_PER_SCHEDULE = 50;

    private final ScheduleRepository scheduleRepository;
    private final ExecutionRepository executionRepository;
    private final ExecutionService executionService;
    private final Executor backupExecutor;
    private final Duration grace;
    private final Duration lookback;

    /** Disparos (programación + hora) enviados al pool que aún no terminan: evita duplicarlos. */
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    public BackupScheduler(ScheduleRepository scheduleRepository,
                           ExecutionRepository executionRepository,
                           ExecutionService executionService,
                           @Qualifier("backupExecutor") Executor backupExecutor,
                           @Value("${scheduler.grace-minutes:10}") long graceMinutes,
                           @Value("${scheduler.lookback-hours:24}") long lookbackHours) {
        this.scheduleRepository = scheduleRepository;
        this.executionRepository = executionRepository;
        this.executionService = executionService;
        this.backupExecutor = backupExecutor;
        this.grace = Duration.ofMinutes(graceMinutes);
        this.lookback = Duration.ofHours(lookbackHours);
    }

    @Scheduled(cron = "0 * * * * *")
    public void tick() {
        evaluate(LocalDateTime.now());
    }

    void evaluate(LocalDateTime now) {
        for (Schedule schedule : scheduleRepository.findAllActiveWithStrategy()) {
            try {
                evaluateSchedule(schedule, now);
            } catch (Exception e) {
                log.error("Error evaluando la programación {}", schedule.getId(), e);
            }
        }
    }

    private void evaluateSchedule(Schedule sc, LocalDateTime now) {
        CronExpression cron;
        try {
            cron = CronExpression.parse(sc.getCronExpression());
        } catch (IllegalArgumentException e) {
            log.warn("La programación {} tiene un cron inválido: {}", sc.getId(), sc.getCronExpression());
            return;
        }

        LocalDateTime floor = now.minus(lookback);
        LocalDateTime tracked = sc.getTrackedSince() != null ? sc.getTrackedSince() : now.minusMinutes(1);
        LocalDateTime since = tracked.isAfter(floor) ? tracked : floor;

        List<LocalDateTime> fires = fireTimes(cron, since, now);
        for (int i = 0; i < fires.size(); i++) {
            LocalDateTime fire = fires.get(i);
            if (isHandled(sc, cron, fire, now)) {
                continue;
            }
            handle(sc, fire, now, i == fires.size() - 1);
        }
    }

    /** Horas de disparo en (since, now], de la más antigua a la más reciente. */
    private List<LocalDateTime> fireTimes(CronExpression cron, LocalDateTime since, LocalDateTime now) {
        Deque<LocalDateTime> last = new ArrayDeque<>();
        LocalDateTime t = cron.next(since);
        int steps = 0;
        while (t != null && !t.isAfter(now) && steps++ < MAX_CRON_STEPS) {
            last.addLast(t);
            if (last.size() > MAX_FIRES_PER_SCHEDULE) {
                last.removeFirst();
            }
            t = cron.next(t);
        }
        return new ArrayList<>(last);
    }

    /** Una hora de disparo está atendida si hay una ejecución (real u omitida) entre esa hora y la siguiente. */
    private boolean isHandled(Schedule sc, CronExpression cron, LocalDateTime fire, LocalDateTime now) {
        LocalDateTime next = cron.next(fire);
        LocalDateTime upper = next != null ? next : now.plusMinutes(1);
        return executionRepository.existsByScheduleIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
                sc.getId(), fire, upper);
    }

    private void handle(Schedule sc, LocalDateTime fire, LocalDateTime now, boolean latest) {
        if (!latest) {
            skip(sc.getId(), fire, "La ejecución programada no se realizó a su hora "
                    + "(la aplicación no estuvo disponible o el planificador estaba detenido)");
            return;
        }
        boolean hasWindow = sc.getWindowStart() != null && sc.getWindowEnd() != null;
        if (!isInWindow(now, sc.getWindowStart(), sc.getWindowEnd())) {
            skip(sc.getId(), fire, "La ejecución no pudo iniciarse dentro de la ventana de respaldo ("
                    + sc.getWindowStart() + " - " + sc.getWindowEnd() + ")");
            return;
        }
        boolean timely = Duration.between(fire, now).compareTo(grace) <= 0;
        if (!timely && !hasWindow) {
            skip(sc.getId(), fire, "La ejecución no se inició a tiempo (más de " + grace.toMinutes()
                    + " minutos de retraso); es probable que la aplicación estuviera detenida");
            return;
        }
        launch(sc, fire);
    }

    private void launch(Schedule sc, LocalDateTime fire) {
        String key = sc.getId() + "|" + fire;
        if (!inFlight.add(key)) {
            return; // ya está en cola o ejecutándose
        }
        Long scheduleId = sc.getId();
        Long strategyId = sc.getStrategy().getId();
        BackupType type = sc.getBackupType().getBackupType();
        try {
            backupExecutor.execute(() -> {
                try {
                    executionService.run(strategyId, type, ExecutionOrigin.PROGRAMADA, scheduleId);
                } catch (BusinessException | NotFoundException e) {
                    skip(scheduleId, fire, "No se pudo iniciar la ejecución programada: " + e.getMessage());
                } catch (Exception e) {
                    log.error("Falló la ejecución programada de la programación {}", scheduleId, e);
                } finally {
                    inFlight.remove(key);
                }
            });
        } catch (RejectedExecutionException e) {
            inFlight.remove(key);
            skip(scheduleId, fire, "No se pudo iniciar la ejecución: la cola de ejecuciones está llena");
        }
    }

    private void skip(Long scheduleId, LocalDateTime fire, String reason) {
        try {
            executionService.registerSkipped(scheduleId, fire, reason);
        } catch (Exception e) {
            log.error("No se pudo registrar la omisión de la programación {}", scheduleId, e);
        }
    }

    /** Ventana de inicio a fin; si inicio es posterior a fin, cruza la medianoche (ej. 23:00 a 04:00). */
    static boolean isInWindow(LocalDateTime now, LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return true;
        }
        LocalTime t = now.toLocalTime();
        return start.isBefore(end)
                ? (!t.isBefore(start) && t.isBefore(end))
                : (!t.isBefore(start) || t.isBefore(end));
    }
}