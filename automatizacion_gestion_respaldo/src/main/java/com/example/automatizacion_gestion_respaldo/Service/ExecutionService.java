package com.example.automatizacion_gestion_respaldo.Service;

import com.example.automatizacion_gestion_respaldo.domain.*;
import com.example.automatizacion_gestion_respaldo.DTO.*;
import com.example.automatizacion_gestion_respaldo.Exception.BusinessException;
import com.example.automatizacion_gestion_respaldo.Exception.NotFoundException;
import com.example.automatizacion_gestion_respaldo.Repository.ExecutionFileRepository;
import com.example.automatizacion_gestion_respaldo.Repository.ExecutionRepository;
import com.example.automatizacion_gestion_respaldo.Repository.ExecutionSpecifications;
import com.example.automatizacion_gestion_respaldo.Repository.ScheduleRepository;
import com.example.automatizacion_gestion_respaldo.Repository.StatusCount;
import com.example.automatizacion_gestion_respaldo.Repository.StrategyRepository;
import com.example.automatizacion_gestion_respaldo.rman.ExecutionResult;
import com.example.automatizacion_gestion_respaldo.rman.LogAnalyzer;
import com.example.automatizacion_gestion_respaldo.rman.RmanExecutor;
import com.example.automatizacion_gestion_respaldo.rman.RmanScriptBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ExecutionService {

    private final StrategyRepository strategyRepository;
    private final ScheduleRepository scheduleRepository;
    private final ExecutionRepository executionRepository;
    private final ExecutionFileRepository executionFileRepository;
    private final RmanScriptBuilder scriptBuilder;
    private final RmanExecutor executor;
    private final LogAnalyzer analyzer;
    private final TransactionTemplate tx;

    /** Estrategias que se están ejecutando ahora mismo (una sola ejecución por estrategia). */
    private final Set<Long> running = ConcurrentHashMap.newKeySet();

    public ExecutionService(StrategyRepository strategyRepository,
                            ScheduleRepository scheduleRepository,
                            ExecutionRepository executionRepository,
                            ExecutionFileRepository executionFileRepository,
                            RmanScriptBuilder scriptBuilder,
                            RmanExecutor executor,
                            LogAnalyzer analyzer,
                            PlatformTransactionManager transactionManager) {
        this.strategyRepository = strategyRepository;
        this.scheduleRepository = scheduleRepository;
        this.executionRepository = executionRepository;
        this.executionFileRepository = executionFileRepository;
        this.scriptBuilder = scriptBuilder;
        this.executor = executor;
        this.analyzer = analyzer;
        this.tx = new TransactionTemplate(transactionManager);
    }

    // =============================================================== ejecutar

    /**
     * Ejecuta un respaldo de la estrategia. No es @Transactional a propósito: la ejecución de RMAN
     * puede durar horas y no debe mantener una transacción abierta. Usa tres fases cortas.
     *
     * @param scheduleId programación que la origina (nulo si es manual)
     */
    public ExecutionDetailResponse run(Long strategyId, BackupType backupType,
                                       ExecutionOrigin origin, Long scheduleId) {
        if (!running.add(strategyId)) {
            throw new BusinessException("Ya hay una ejecución en curso para esta estrategia");
        }
        try {
            Prepared prepared = tx.execute(status -> prepare(strategyId, backupType, origin, scheduleId));
            if (prepared.failed() != null) {
                return prepared.failed();
            }
            ExecutionResult result = executeSafely(prepared);
            return tx.execute(status -> finish(prepared.executionId(), prepared.backupType(), result));
        } finally {
            running.remove(strategyId);
        }
    }

    private record Prepared(Long executionId, DatabaseTarget target, String script,
                            String destination, BackupType backupType, ExecutionDetailResponse failed) {}

    /** Fase 1: valida, crea el registro EN_EJECUCION y genera el script. */
    private Prepared prepare(Long strategyId, BackupType backupType, ExecutionOrigin origin, Long scheduleId) {
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new NotFoundException("Estrategia no encontrada: " + strategyId));
        if (!Boolean.TRUE.equals(strategy.getActive())) {
            throw new BusinessException("La estrategia está inactiva; actívela antes de ejecutarla");
        }
        DatabaseTarget target = strategy.getDatabaseTarget();
        if (!Boolean.TRUE.equals(target.getActive())) {
            throw new BusinessException("La base de datos está inactiva");
        }
        StrategyBackupType strategyBackupType = strategy.getBackupTypes().stream()
                .filter(b -> b.getBackupType() == backupType)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "El tipo de respaldo " + backupType + " no pertenece a esta estrategia"));

        Schedule schedule = null;
        if (scheduleId != null) {
            schedule = strategy.getSchedules().stream()
                    .filter(x -> Objects.equals(x.getId(), scheduleId))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Programación no encontrada: " + scheduleId));
        }

        Execution e = new Execution();
        e.setStrategy(strategy);
        e.setSchedule(schedule);
        e.setStrategyName(strategy.getName());
        e.setDatabaseName(target.getName());
        e.setBackupType(backupType);
        e.setOrigin(origin);
        e.setStartTime(LocalDateTime.now());
        e.setStatus(ExecutionStatus.EN_EJECUCION);
        e.setArchived(false);
        e = executionRepository.save(e);

        String script;
        try {
            script = scriptBuilder.buildScript(strategy, strategyBackupType);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            String message = "No se pudo generar el script RMAN: " + ex.getMessage();
            e.setEndTime(LocalDateTime.now());
            e.setDurationSeconds(0L);
            e.setStatus(ExecutionStatus.ERROR);
            e.setRmanLog(message);
            e.setIdentifiedErrors(message);
            e.setTotalSizeBytes(0L);
            e = executionRepository.save(e);
            return new Prepared(e.getId(), target, null, null, backupType, toDetail(e));
        }

        e.setUsedScript(script); // queda guardado aunque la aplicación se detenga durante la ejecución
        e = executionRepository.save(e);
        return new Prepared(e.getId(), target, script, strategy.getDestinationPath(), backupType, null);
    }

    /** Fase 2: ejecuta RMAN fuera de toda transacción; cualquier fallo se convierte en un resultado de error. */
    private ExecutionResult executeSafely(Prepared p) {
        try {
            return executor.execute(p.target(), p.script(), p.destination());
        } catch (Exception ex) {
            log.error("Fallo inesperado del ejecutor RMAN", ex);
            LocalDateTime now = LocalDateTime.now();
            return ExecutionResult.builder()
                    .outputLog("Fallo inesperado del ejecutor: " + ex.getMessage())
                    .exitCode(-1)
                    .startTime(now)
                    .endTime(now)
                    .durationSeconds(0L)
                    .backupLocation(p.destination())
                    .totalSizeBytes(0L)
                    .build();
        }
    }

    /** Fase 3: analiza el resultado y guarda la evidencia completa. */
    private ExecutionDetailResponse finish(Long executionId, BackupType backupType, ExecutionResult result) {
        Execution e = executionRepository.findById(executionId)
                .orElseThrow(() -> new NotFoundException("Ejecución no encontrada: " + executionId));

        String rmanLog = result.getOutputLog();
        ExecutionStatus status = analyzer.determineStatus(result.getExitCode(), rmanLog);
        List<String> notes = new ArrayList<>();
        String errors = analyzer.extractErrors(rmanLog);
        if (errors != null) {
            notes.add(errors);
        }

        List<ExecutionResult.GeneratedFile> files = result.getFiles() == null ? List.of() : result.getFiles();
        if (status == ExecutionStatus.EXITOSO) {
            List<String> missing = files.stream().filter(f -> f.sizeBytes() == null)
                    .map(ExecutionResult.GeneratedFile::path).toList();
            if (!missing.isEmpty()) {
                status = ExecutionStatus.ADVERTENCIA;
                missing.forEach(path ->
                        notes.add("El archivo de respaldo reportado por RMAN no existe en disco: " + path));
            } else if (files.isEmpty() && backupType != BackupType.ARCHIVELOG) {
                status = ExecutionStatus.ADVERTENCIA;
                notes.add("RMAN terminó sin errores pero no se registraron archivos de respaldo; verifique el destino");
            }
        }
        if (status == ExecutionStatus.ERROR && notes.isEmpty()) {
            notes.add("La ejecución terminó con código de salida " + result.getExitCode());
        }

        // El inicio real es el del registro; solo se adelanta, nunca se mueve al pasado
        if (result.getStartTime() != null && result.getStartTime().isAfter(e.getStartTime())) {
            e.setStartTime(result.getStartTime());
        }
        LocalDateTime end = result.getEndTime() != null ? result.getEndTime() : LocalDateTime.now();
        if (end.isBefore(e.getStartTime())) {
            end = e.getStartTime();
        }
        e.setEndTime(end);
        e.setDurationSeconds(result.getDurationSeconds() != null
                ? result.getDurationSeconds()
                : Duration.between(e.getStartTime(), end).getSeconds());
        e.setStatus(status);
        e.setRmanLog(rmanLog);
        e.setIdentifiedErrors(notes.isEmpty() ? null : String.join("\n", notes));
        e.setBackupLocation(result.getBackupLocation());
        e.setTotalSizeBytes(result.getTotalSizeBytes());

        for (ExecutionResult.GeneratedFile f : files) {
            ExecutionFile file = new ExecutionFile();
            file.setPath(f.path());
            file.setSizeBytes(f.sizeBytes());
            e.addFile(file);
            executionFileRepository.save(file);
        }
        e = executionRepository.save(e);
        return toDetail(e);
    }

    // =============================================================== omitidas e interrumpidas

    /** Registra como OMITIDO un respaldo programado que debía correr y no corrió (no queda en silencio). */
    @Transactional
    public ExecutionDetailResponse registerSkipped(Long scheduleId, LocalDateTime expectedAt, String reason) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Programación no encontrada: " + scheduleId));
        Strategy strategy = schedule.getStrategy();

        Execution e = new Execution();
        e.setStrategy(strategy);
        e.setSchedule(schedule);
        e.setStrategyName(strategy.getName());
        e.setDatabaseName(strategy.getDatabaseTarget().getName());
        e.setBackupType(schedule.getBackupType().getBackupType());
        e.setOrigin(ExecutionOrigin.PROGRAMADA);
        e.setStartTime(expectedAt);
        e.setEndTime(expectedAt);
        e.setDurationSeconds(0L);
        e.setStatus(ExecutionStatus.OMITIDO);
        e.setRmanLog(reason);
        e.setIdentifiedErrors(reason);
        e.setTotalSizeBytes(0L);
        e.setArchived(false);
        return toDetail(executionRepository.save(e));
    }

    /** Si la aplicación se detuvo con ejecuciones en curso, quedan marcadas como ERROR. */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterrupted() {
        tx.executeWithoutResult(status -> {
            for (Execution e : executionRepository.findByStatus(ExecutionStatus.EN_EJECUCION)) {
                LocalDateTime now = LocalDateTime.now();
                e.setStatus(ExecutionStatus.ERROR);
                e.setEndTime(now);
                e.setDurationSeconds(Duration.between(e.getStartTime(), now).getSeconds());
                e.setIdentifiedErrors("La ejecución quedó interrumpida: la aplicación se detuvo antes de que terminara");
                executionRepository.save(e);
                log.warn("Ejecución {} marcada como interrumpida", e.getId());
            }
        });
    }

    // =============================================================== historial

    @Transactional(readOnly = true)
    public Page<ExecutionSummaryResponse> search(ExecutionFilter filter, Pageable pageable) {
        return executionRepository.findAll(spec(filter), pageable).map(this::toSummary);
    }

    /** Sin paginar, de más reciente a más antigua; lo usará la exportación a CSV y PDF. */
    @Transactional(readOnly = true)
    public List<ExecutionSummaryResponse> searchAll(ExecutionFilter filter) {
        return executionRepository.findAll(spec(filter), Sort.by(Sort.Direction.DESC, "startTime"))
                .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ExecutionDetailResponse detail(Long id) {
        return toDetail(find(id));
    }

    @Transactional
    public ExecutionSummaryResponse setArchived(Long id, boolean archived) {
        Execution e = find(id);
        e.setArchived(archived);
        return toSummary(executionRepository.save(e));
    }

    /** Resumen por período: cuántas ejecuciones hubo en cada estado (las archivadas no cuentan). */
    @Transactional(readOnly = true)
    public SummaryResponse summary(LocalDateTime from, LocalDateTime to) {
        Map<ExecutionStatus, Long> counts = new EnumMap<>(ExecutionStatus.class);
        for (StatusCount c : executionRepository.summaryByStatus(from, to)) {
            counts.put(c.getStatus(), c.getTotal());
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new SummaryResponse(from, to, total,
                counts.getOrDefault(ExecutionStatus.EXITOSO, 0L),
                counts.getOrDefault(ExecutionStatus.ADVERTENCIA, 0L),
                counts.getOrDefault(ExecutionStatus.ERROR, 0L),
                counts.getOrDefault(ExecutionStatus.OMITIDO, 0L),
                counts.getOrDefault(ExecutionStatus.EN_EJECUCION, 0L));
    }

    // =============================================================== utilidades

    private Execution find(Long id) {
        return executionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ejecución no encontrada: " + id));
    }

    private org.springframework.data.jpa.domain.Specification<Execution> spec(ExecutionFilter f) {
        ExecutionFilter filter = f == null ? new ExecutionFilter(null, null, null, null, null, null) : f;
        return ExecutionSpecifications.filter(filter.from(), filter.to(), filter.databaseName(),
                filter.strategyId(), filter.status(), filter.archived());
    }

    private ExecutionSummaryResponse toSummary(Execution e) {
        return new ExecutionSummaryResponse(e.getId(), e.getStartTime(), e.getEndTime(), e.getDurationSeconds(),
                e.getStrategyName(), e.getDatabaseName(), e.getBackupType(), e.getOrigin(), e.getStatus(),
                Boolean.TRUE.equals(e.getArchived()));
    }

    private ExecutionDetailResponse toDetail(Execution e) {
        List<ExecutionFileDto> files = e.getFiles().stream()
                .map(f -> new ExecutionFileDto(f.getPath(), f.getSizeBytes(), f.getSizeBytes() != null))
                .toList();
        return new ExecutionDetailResponse(e.getId(), e.getStartTime(), e.getEndTime(), e.getDurationSeconds(),
                e.getStrategyName(), e.getDatabaseName(), e.getBackupType(), e.getOrigin(), e.getStatus(),
                Boolean.TRUE.equals(e.getArchived()), e.getBackupLocation(), e.getTotalSizeBytes(),
                e.getUsedScript(), e.getRmanLog(), e.getIdentifiedErrors(), files);
    }
}