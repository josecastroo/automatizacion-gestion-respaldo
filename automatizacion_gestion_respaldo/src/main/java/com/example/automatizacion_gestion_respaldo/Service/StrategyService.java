package com.example.automatizacion_gestion_respaldo.Service;

import com.example.automatizacion_gestion_respaldo.domain.*;
import com.example.automatizacion_gestion_respaldo.DTO.*;
import com.example.automatizacion_gestion_respaldo.Exception.BusinessException;
import com.example.automatizacion_gestion_respaldo.Exception.NotFoundException;
import com.example.automatizacion_gestion_respaldo.rman.RmanScriptBuilder;
import com.example.automatizacion_gestion_respaldo.Repository.DatabaseTargetRepository;
import com.example.automatizacion_gestion_respaldo.Repository.ExecutionRepository;
import com.example.automatizacion_gestion_respaldo.Repository.ScheduleRepository;
import com.example.automatizacion_gestion_respaldo.Repository.StrategyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StrategyService {

    private static final Pattern TABLESPACE_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_$#]{0,29}$");
    private static final Pattern DATAFILE_NUMBER = Pattern.compile("^\\d{1,5}$");

    private final StrategyRepository strategyRepository;
    private final DatabaseTargetRepository databaseTargetRepository;
    private final ScheduleRepository scheduleRepository;
    private final ExecutionRepository executionRepository;
    private final RmanScriptBuilder scriptBuilder;

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public List<StrategyResponse> list() {
        return strategyRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StrategyResponse get(Long id) {
        return toResponse(find(id));
    }

    /** Vista previa del script RMAN, sin ejecutarlo. */
    @Transactional(readOnly = true)
    public String previewScript(Long id, BackupType type) {
        Strategy strategy = find(id);
        StrategyBackupType backupType = findBackupType(strategy, type);
        try {
            return scriptBuilder.buildScript(strategy, backupType);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new BusinessException("No se puede generar el script: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ estrategia

    @Transactional
    public StrategyResponse create(StrategyRequest r) {
        String name = r.name().trim();
        if (strategyRepository.existsByName(name)) {
            throw new BusinessException("Ya existe una estrategia con ese nombre");
        }
        DatabaseTarget target = findTarget(r.databaseTargetId());
        requireActive(target);

        Strategy strategy = new Strategy();
        applyBasics(strategy, r, name, target);
        replaceComponents(strategy, r.components());
        syncBackupTypes(strategy, r.backupTypes());
        validateComposition(strategy);
        return toResponse(strategyRepository.save(strategy));
    }

    @Transactional
    public StrategyResponse update(Long id, StrategyRequest r) {
        Strategy strategy = find(id);
        String name = r.name().trim();
        if (strategyRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException("Ya existe otra estrategia con ese nombre");
        }
        DatabaseTarget target = findTarget(r.databaseTargetId());
        boolean sameTarget = strategy.getDatabaseTarget() != null
                && Objects.equals(strategy.getDatabaseTarget().getId(), target.getId());
        if (!sameTarget) {
            requireActive(target);
        }

        applyBasics(strategy, r, name, target);
        replaceComponents(strategy, r.components());
        syncBackupTypes(strategy, r.backupTypes());
        validateComposition(strategy);
        return toResponse(strategyRepository.save(strategy));
    }

    @Transactional
    public StrategyResponse setActive(Long id, boolean active) {
        Strategy strategy = find(id);
        if (active && !Boolean.TRUE.equals(strategy.getActive())) {
            restartTracking(strategy);
        }
        strategy.setActive(active);
        return toResponse(strategyRepository.save(strategy));
    }

    @Transactional
    public void delete(Long id) {
        Strategy strategy = find(id);
        if (executionRepository.existsByStrategyId(id)) {
            throw new BusinessException(
                    "La estrategia tiene ejecuciones registradas (evidencia); desactívela en lugar de eliminarla");
        }
        strategyRepository.delete(strategy);
    }

    // ------------------------------------------------------------------ programaciones (cuándo)

    @Transactional
    public ScheduleResponse addSchedule(Long strategyId, ScheduleRequest r) {
        Strategy strategy = find(strategyId);
        Schedule schedule = new Schedule();
        applySchedule(schedule, strategy, r);
        strategy.addSchedule(schedule);
        return toScheduleResponse(scheduleRepository.save(schedule));
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long strategyId, Long scheduleId, ScheduleRequest r) {
        Strategy strategy = find(strategyId);
        Schedule schedule = findSchedule(strategy, scheduleId);
        applySchedule(schedule, strategy, r);
        return toScheduleResponse(schedule);
    }

    @Transactional
    public ScheduleResponse setScheduleActive(Long strategyId, Long scheduleId, boolean active) {
        Strategy strategy = find(strategyId);
        Schedule schedule = findSchedule(strategy, scheduleId);
        if (active && !Boolean.TRUE.equals(schedule.getActive())) {
            schedule.setTrackedSince(LocalDateTime.now());
        }
        schedule.setActive(active);
        return toScheduleResponse(schedule);
    }

    @Transactional
    public void deleteSchedule(Long strategyId, Long scheduleId) {
        Strategy strategy = find(strategyId);
        Schedule schedule = findSchedule(strategy, scheduleId);
        if (executionRepository.existsByScheduleId(scheduleId)) {
            throw new BusinessException(
                    "La programación ya generó ejecuciones (evidencia); desactívela en lugar de eliminarla");
        }
        strategy.getSchedules().remove(schedule);
    }

    // ------------------------------------------------------------------ validaciones

    private void applyBasics(Strategy s, StrategyRequest r, String name, DatabaseTarget target) {
        s.setName(name);
        s.setDatabaseTarget(target);
        s.setRetentionDays(r.retentionDays());
        s.setDescription(r.description());
        s.setResponsible(r.responsible());
        s.setDestinationPath(r.destinationPath().trim());
        if (r.active() != null) {
            if (r.active() && !Boolean.TRUE.equals(s.getActive())) {
                restartTracking(s);
            }
            s.setActive(r.active());
        }
    }

    private void replaceComponents(Strategy s, List<ComponentDto> dtos) {
        List<ComponentDto> list = dtos == null ? List.of() : dtos;
        Set<String> seen = new HashSet<>();
        List<StrategyComponent> fresh = new ArrayList<>();

        for (ComponentDto d : list) {
            if (d.componentType() == null) {
                throw new BusinessException("Hay un componente sin tipo");
            }
            String objectName = normalizeObjectName(d);
            if (!seen.add(d.componentType() + ":" + objectName)) {
                throw new BusinessException("Componente repetido: " + d.componentType()
                        + (objectName == null ? "" : " " + objectName));
            }
            StrategyComponent c = new StrategyComponent();
            c.setComponentType(d.componentType());
            c.setObjectName(objectName);
            c.setPriority(d.priority() != null ? d.priority() : s.getDatabaseTarget().getPriority());
            fresh.add(c);
        }
        s.getComponents().clear();
        fresh.forEach(s::addComponent);
    }

    private String normalizeObjectName(ComponentDto d) {
        String raw = d.objectName() == null ? null : d.objectName().trim();
        return switch (d.componentType()) {
            case TABLESPACE -> {
                if (raw == null || !TABLESPACE_NAME.matcher(raw).matches()) {
                    throw new BusinessException(
                            "Nombre de tablespace inválido: use letras, números y _ $ # (máximo 30 caracteres)");
                }
                yield raw.toUpperCase();
            }
            case DATAFILE -> {
                if (raw == null || !DATAFILE_NUMBER.matcher(raw).matches()) {
                    throw new BusinessException("El datafile debe indicarse por su número (solo dígitos)");
                }
                yield raw;
            }
            default -> null; // DB, CONTROLFILE, SPFILE y ARCHIVELOG no llevan nombre
        };
    }

    private void syncBackupTypes(Strategy s, List<BackupTypeDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BusinessException("Debe seleccionar al menos un tipo de respaldo");
        }
        Map<BackupType, BackupTypeDto> requested = new LinkedHashMap<>();
        for (BackupTypeDto d : dtos) {
            if (d.backupType() == null) {
                throw new BusinessException("Hay un tipo de respaldo vacío");
            }
            if (requested.put(d.backupType(), d) != null) {
                throw new BusinessException("Tipo de respaldo repetido: " + d.backupType());
            }
        }

        // Quitar los que ya no están, salvo que tengan programaciones
        for (StrategyBackupType existing : new ArrayList<>(s.getBackupTypes())) {
            if (!requested.containsKey(existing.getBackupType())) {
                boolean inUse = s.getSchedules().stream()
                        .anyMatch(sc -> sc.getBackupType().getBackupType() == existing.getBackupType());
                if (inUse) {
                    throw new BusinessException("No se puede quitar el tipo " + existing.getBackupType()
                            + ": tiene programaciones asociadas. Cambie o desactive esas programaciones primero");
                }
                s.getBackupTypes().remove(existing);
            }
        }

        // Agregar o actualizar
        for (BackupTypeDto d : requested.values()) {
            StrategyBackupType bt = s.getBackupTypes().stream()
                    .filter(b -> b.getBackupType() == d.backupType())
                    .findFirst().orElse(null);
            if (bt == null) {
                bt = new StrategyBackupType();
                bt.setBackupType(d.backupType());
                s.addBackupType(bt);
            }
            bt.setCompressed(Boolean.TRUE.equals(d.compressed()));
            bt.setParameters(d.parameters());
        }
    }

    private void validateComposition(Strategy s) {
        boolean onlyArchivelog = s.getBackupTypes().stream()
                .allMatch(b -> b.getBackupType() == BackupType.ARCHIVELOG);
        if (!onlyArchivelog && s.getComponents().isEmpty()) {
            throw new BusinessException("Debe seleccionar al menos un componente a respaldar (qué respaldar)");
        }
    }

    private void applySchedule(Schedule sc, Strategy strategy, ScheduleRequest r) {
        String cron = r.cronExpression() == null ? "" : r.cronExpression().trim();
        if (!CronExpression.isValidExpression(cron)) {
            throw new BusinessException(
                    "Expresión cron inválida: use 6 campos (segundo minuto hora día mes díaSemana), por ejemplo 0 0 23 * * *");
        }
        if ((r.windowStart() == null) != (r.windowEnd() == null)) {
            throw new BusinessException("La ventana de respaldo necesita hora de inicio y de fin");
        }
        if (r.windowStart() != null && r.windowStart().equals(r.windowEnd())) {
            throw new BusinessException("La hora de inicio y de fin de la ventana no pueden ser iguales");
        }
        StrategyBackupType backupType = findBackupType(strategy, r.backupType());

        sc.setBackupType(backupType);
        sc.setCronExpression(cron);
        sc.setWindowStart(r.windowStart());
        sc.setWindowEnd(r.windowEnd());
        sc.setActive(r.active() == null || r.active());
        sc.setTrackedSince(LocalDateTime.now());
    }

    // ------------------------------------------------------------------ avisos (informativo / advertencia / recomendación)

    private List<Notice> analyze(Strategy s) {
        List<Notice> notices = new ArrayList<>();
        DatabaseTarget t = s.getDatabaseTarget();
        boolean includesArchivelog =
                s.getComponents().stream().anyMatch(c -> c.getComponentType() == ComponentType.ARCHIVELOG)
                        || s.getBackupTypes().stream().anyMatch(b -> b.getBackupType() == BackupType.ARCHIVELOG);

        DatabaseTarget.ArchiveMode mode = t.getArchiveMode();
        if (mode == DatabaseTarget.ArchiveMode.NOARCHIVELOG) {
            notices.add(new Notice(NoticeLevel.ADVERTENCIA,
                    "La base de datos se encuentra en modo NOARCHIVELOG. Las posibilidades de recuperación son más "
                            + "limitadas. Revise la estrategia de respaldo y los requerimientos de recuperación antes de continuar."));
            if (includesArchivelog) {
                notices.add(new Notice(NoticeLevel.ADVERTENCIA,
                        "La estrategia incluye archived redo logs, pero la base está en NOARCHIVELOG: "
                                + "el script no podrá generarse."));
            }
        } else if (mode == DatabaseTarget.ArchiveMode.ARCHIVELOG) {
            if (!includesArchivelog) {
                notices.add(new Notice(NoticeLevel.RECOMENDACION,
                        "La base de datos se encuentra en modo ARCHIVELOG. Considere incorporar el respaldo periódico "
                                + "de los archived redo logs dentro de la estrategia para mejorar las posibilidades de recuperación."));
            }
        } else {
            notices.add(new Notice(NoticeLevel.INFORMATIVO,
                    "Aún no se ha determinado el modo de archivado de la base de datos."));
        }

        if (s.getSchedules().isEmpty()) {
            notices.add(new Notice(NoticeLevel.ADVERTENCIA,
                    "La estrategia no tiene programación: no se ejecutará automáticamente."));
        }
        if (!Boolean.TRUE.equals(s.getActive())) {
            notices.add(new Notice(NoticeLevel.ADVERTENCIA, "La estrategia está inactiva."));
        }
        return notices;
    }

    // ------------------------------------------------------------------ utilidades

    private Strategy find(Long id) {
        return strategyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Estrategia no encontrada: " + id));
    }

    private DatabaseTarget findTarget(Long id) {
        return databaseTargetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Base de datos no encontrada: " + id));
    }

    private void requireActive(DatabaseTarget target) {
        if (!Boolean.TRUE.equals(target.getActive())) {
            throw new BusinessException("La base de datos seleccionada está inactiva");
        }
    }

    /** Reinicia desde cuándo el planificador vigila las programaciones (evita omisiones falsas). */
    private void restartTracking(Strategy s) {
        LocalDateTime now = LocalDateTime.now();
        s.getSchedules().forEach(sc -> sc.setTrackedSince(now));
    }

    private StrategyBackupType findBackupType(Strategy s, BackupType type) {
        return s.getBackupTypes().stream()
                .filter(b -> b.getBackupType() == type)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "El tipo de respaldo " + type + " no pertenece a esta estrategia"));
    }

    private Schedule findSchedule(Strategy s, Long scheduleId) {
        return s.getSchedules().stream()
                .filter(x -> Objects.equals(x.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Programación no encontrada: " + scheduleId));
    }

    private StrategyResponse toResponse(Strategy s) {
        DatabaseTarget t = s.getDatabaseTarget();
        return new StrategyResponse(
                s.getId(), s.getName(), t.getId(), t.getName(),
                s.getRetentionDays(), s.getDescription(), s.getResponsible(),
                s.getDestinationPath(), s.getActive(),
                s.getComponents().stream()
                        .map(c -> new ComponentDto(c.getComponentType(), c.getObjectName(), c.getPriority()))
                        .toList(),
                s.getBackupTypes().stream()
                        .map(b -> new BackupTypeDto(b.getBackupType(), b.getCompressed(), b.getParameters()))
                        .toList(),
                s.getSchedules().stream().map(this::toScheduleResponse).toList(),
                analyze(s));
    }

    private ScheduleResponse toScheduleResponse(Schedule sc) {
        return new ScheduleResponse(sc.getId(), sc.getBackupType().getBackupType(), sc.getCronExpression(),
                sc.getWindowStart(), sc.getWindowEnd(), sc.getActive());
    }
}