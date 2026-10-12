package com.example.automatizacion_gestion_respaldo.Service;

import com.example.automatizacion_gestion_respaldo.domain.*;
import com.example.automatizacion_gestion_respaldo.DTO.*;
import com.example.automatizacion_gestion_respaldo.Exception.BusinessException;
import com.example.automatizacion_gestion_respaldo.Repository.DatabaseTargetRepository;
import com.example.automatizacion_gestion_respaldo.Repository.ExecutionRepository;
import com.example.automatizacion_gestion_respaldo.Repository.ScheduleRepository;
import com.example.automatizacion_gestion_respaldo.Repository.StrategyRepository;
import com.example.automatizacion_gestion_respaldo.rman.RmanScriptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StrategyServiceTest {

    @Mock StrategyRepository strategyRepository;
    @Mock DatabaseTargetRepository databaseTargetRepository;
    @Mock ScheduleRepository scheduleRepository;
    @Mock ExecutionRepository executionRepository;

    StrategyService service;
    DatabaseTarget target;

    @BeforeEach
    void setUp() {
        service = new StrategyService(strategyRepository, databaseTargetRepository,
                scheduleRepository, executionRepository, new RmanScriptBuilder());
        target = target(true, DatabaseTarget.ArchiveMode.ARCHIVELOG);
        when(databaseTargetRepository.findById(1L)).thenReturn(Optional.of(target));
        when(strategyRepository.save(any(Strategy.class))).thenAnswer(i -> i.getArgument(0));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ---------- ayudas ----------

    private DatabaseTarget target(boolean active, DatabaseTarget.ArchiveMode mode) {
        DatabaseTarget t = new DatabaseTarget();
        t.setId(1L);
        t.setName("PROD");
        t.setActive(active);
        t.setArchiveMode(mode);
        t.setPriority(Priority.ALTA);
        return t;
    }

    private StrategyRequest request(List<ComponentDto> components, List<BackupTypeDto> types) {
        return new StrategyRequest("Diaria", 1L, 7, "desc", "Ale", "C:/backups", true, components, types);
    }

    private ComponentDto comp(ComponentType type, String name) {
        return new ComponentDto(type, name, null);
    }

    private BackupTypeDto type(BackupType type) {
        return new BackupTypeDto(type, false, null);
    }

    private ScheduleRequest schedule(BackupType type, String cron, LocalTime start, LocalTime end) {
        return new ScheduleRequest(type, cron, start, end, true);
    }

    /** Estrategia ya existente (id 5) con un componente DB y los tipos indicados. */
    private Strategy strategyWith(BackupType... types) {
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
        for (BackupType t : types) {
            StrategyBackupType b = new StrategyBackupType();
            b.setBackupType(t);
            b.setCompressed(false);
            s.addBackupType(b);
        }
        when(strategyRepository.findById(5L)).thenReturn(Optional.of(s));
        return s;
    }

    // ---------- crear ----------

    @Test
    void create_guardaComponentesYTipos() {
        StrategyResponse r = service.create(
                request(List.of(comp(ComponentType.TABLESPACE, "users")), List.of(type(BackupType.FULL))));

        assertThat(r.databaseName()).isEqualTo("PROD");
        assertThat(r.components()).hasSize(1);
        assertThat(r.components().get(0).objectName()).isEqualTo("USERS");
        assertThat(r.components().get(0).priority()).isEqualTo(Priority.ALTA); // hereda de la base
        assertThat(r.backupTypes()).extracting(BackupTypeDto::backupType).containsExactly(BackupType.FULL);
    }

    @Test
    void create_conNombreDuplicado_seRechaza() {
        when(strategyRepository.existsByName("Diaria")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                request(List.of(comp(ComponentType.DB, null)), List.of(type(BackupType.FULL)))))
                .isInstanceOf(BusinessException.class);
        verify(strategyRepository, never()).save(any());
    }

    @Test
    void create_conBaseInactiva_seRechaza() {
        target.setActive(false);

        assertThatThrownBy(() -> service.create(
                request(List.of(comp(ComponentType.DB, null)), List.of(type(BackupType.FULL)))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void create_tablespaceConNombreInvalido_seRechaza() {
        assertThatThrownBy(() -> service.create(
                request(List.of(comp(ComponentType.TABLESPACE, "USERS; HOST 'calc'")),
                        List.of(type(BackupType.FULL)))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void create_datafileQueNoEsNumero_seRechaza() {
        assertThatThrownBy(() -> service.create(
                request(List.of(comp(ComponentType.DATAFILE, "users01.dbf")), List.of(type(BackupType.FULL)))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void create_incrementalSinComponentes_seRechaza() {
        assertThatThrownBy(() -> service.create(request(List.of(), List.of(type(BackupType.INC_1_DIF)))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void create_soloArchivelogSinComponentes_esValido() {
        StrategyResponse r = service.create(request(List.of(), List.of(type(BackupType.ARCHIVELOG))));

        assertThat(r.backupTypes()).hasSize(1);
    }

    @Test
    void create_componenteRepetido_seRechaza() {
        assertThatThrownBy(() -> service.create(
                request(List.of(comp(ComponentType.TABLESPACE, "USERS"), comp(ComponentType.TABLESPACE, "users")),
                        List.of(type(BackupType.FULL)))))
                .isInstanceOf(BusinessException.class);
    }

    // ---------- programaciones ----------

    @Test
    void addSchedule_cronInvalido_seRechaza() {
        strategyWith(BackupType.FULL);

        assertThatThrownBy(() -> service.addSchedule(5L, schedule(BackupType.FULL, "no es un cron", null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void addSchedule_conTipoAjenoALaEstrategia_seRechaza() {
        strategyWith(BackupType.FULL);

        assertThatThrownBy(() -> service.addSchedule(5L, schedule(BackupType.INC_0, "0 0 23 * * *", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void addSchedule_ventanaIncompleta_seRechaza() {
        strategyWith(BackupType.FULL);

        assertThatThrownBy(() -> service.addSchedule(5L,
                schedule(BackupType.FULL, "0 0 23 * * *", LocalTime.of(23, 0), null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void addSchedule_ventanaNocturna_esValida() {
        strategyWith(BackupType.FULL);

        ScheduleResponse r = service.addSchedule(5L,
                schedule(BackupType.FULL, "0 0 23 * * *", LocalTime.of(23, 0), LocalTime.of(4, 0)));

        assertThat(r.windowStart()).isEqualTo(LocalTime.of(23, 0));
        assertThat(r.windowEnd()).isEqualTo(LocalTime.of(4, 0));
        assertThat(r.active()).isTrue();
    }

    // ---------- editar y borrar ----------

    @Test
    void update_quitarUnTipoConProgramaciones_seRechaza() {
        Strategy s = strategyWith(BackupType.FULL, BackupType.INC_1_DIF);
        Schedule sc = new Schedule();
        sc.setBackupType(s.getBackupTypes().get(0)); // FULL
        sc.setCronExpression("0 0 23 * * *");
        s.addSchedule(sc);

        assertThatThrownBy(() -> service.update(5L,
                request(List.of(comp(ComponentType.DB, null)), List.of(type(BackupType.INC_1_DIF)))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("programaciones");
    }

    @Test
    void delete_conEjecuciones_seRechaza() {
        strategyWith(BackupType.FULL);
        when(executionRepository.existsByStrategyId(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(BusinessException.class);
        verify(strategyRepository, never()).delete(any());
    }

    // ---------- avisos ----------

    @Test
    void avisos_noarchivelog_advierte() {
        target.setArchiveMode(DatabaseTarget.ArchiveMode.NOARCHIVELOG);
        strategyWith(BackupType.FULL);

        assertThat(service.get(5L).notices()).anyMatch(
                n -> n.level() == NoticeLevel.ADVERTENCIA && n.message().contains("NOARCHIVELOG"));
    }

    @Test
    void avisos_archivelogSinRespaldarLogs_recomienda() {
        strategyWith(BackupType.FULL);

        assertThat(service.get(5L).notices()).anyMatch(
                n -> n.level() == NoticeLevel.RECOMENDACION && n.message().contains("ARCHIVELOG"));
    }

    // ---------- vista previa ----------

    @Test
    void preview_generaElScript() {
        strategyWith(BackupType.FULL);

        assertThat(service.previewScript(5L, BackupType.FULL)).contains("BACKUP DATABASE");
    }

    @Test
    void preview_estrategiaIncompleta_explicaElMotivo() {
        Strategy s = strategyWith(BackupType.FULL);
        s.getComponents().clear();

        assertThatThrownBy(() -> service.previewScript(5L, BackupType.FULL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No se puede generar el script");
    }
}