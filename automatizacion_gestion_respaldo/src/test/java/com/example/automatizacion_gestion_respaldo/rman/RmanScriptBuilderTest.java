package com.example.automatizacion_gestion_respaldo.rman;

import com.example.automatizacion_gestion_respaldo.domain.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RmanScriptBuilderTest {

    private final RmanScriptBuilder builder = new RmanScriptBuilder();

    // ---------- helpers ----------

    private Strategy strategy(DatabaseTarget.ArchiveMode mode, String destination, Integer retention) {
        DatabaseTarget target = new DatabaseTarget();
        target.setName("PRODUCCION");
        target.setArchiveMode(mode);

        Strategy s = new Strategy();
        s.setName("Estrategia de prueba");
        s.setDatabaseTarget(target);
        s.setDestinationPath(destination);
        s.setRetentionDays(retention);
        return s;
    }

    private StrategyComponent component(ComponentType type, String objectName) {
        StrategyComponent c = new StrategyComponent();
        c.setComponentType(type);
        c.setObjectName(objectName);
        return c;
    }

    private StrategyBackupType backupType(BackupType type, boolean compressed) {
        StrategyBackupType b = new StrategyBackupType();
        b.setBackupType(type);
        b.setCompressed(compressed);
        return b;
    }

    // ---------- tipos de respaldo ----------

    @Test
    void full_conBaseCompleta_incluyeDestinoYArchivelog() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/backups", 7);
        s.addComponent(component(ComponentType.DB, null));

        String script = builder.buildScript(s, backupType(BackupType.FULL, false));

        assertThat(script).contains("BACKUP DATABASE");
        assertThat(script).contains("FORMAT 'C:/backups/");
        assertThat(script).contains("PLUS ARCHIVELOG");
    }

    @Test
    void inc1Diferencial_noUsaCumulative() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/backups", 7);
        s.addComponent(component(ComponentType.DB, null));

        String script = builder.buildScript(s, backupType(BackupType.INC_1_DIF, false));

        assertThat(script).contains("INCREMENTAL LEVEL 1 DATABASE");
        assertThat(script).doesNotContain("CUMULATIVE");
    }

    @Test
    void inc1Acumulativo_usaCumulative() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/backups", 7);
        s.addComponent(component(ComponentType.DB, null));

        String script = builder.buildScript(s, backupType(BackupType.INC_1_ACUM, false));

        assertThat(script).contains("INCREMENTAL LEVEL 1 CUMULATIVE DATABASE");
    }

    @Test
    void inc0_usaNivelCero() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/backups", 7);
        s.addComponent(component(ComponentType.DB, null));

        String script = builder.buildScript(s, backupType(BackupType.INC_0, false));

        assertThat(script).contains("INCREMENTAL LEVEL 0 DATABASE");
    }

    @Test
    void conCompresion_agregaCompressedBackupset() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/backups", 7);
        s.addComponent(component(ComponentType.DB, null));

        String script = builder.buildScript(s, backupType(BackupType.FULL, true));

        assertThat(script).contains("AS COMPRESSED BACKUPSET");
    }

    // ---------- el "qué" ----------

    @Test
    void soloTablespace_noRespaldaToda_laBase() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/backups", 7);
        s.addComponent(component(ComponentType.TABLESPACE, "USERS"));

        String script = builder.buildScript(s, backupType(BackupType.FULL, false));

        assertThat(script).contains("BACKUP TABLESPACE USERS");
        assertThat(script).doesNotContain("BACKUP DATABASE");
    }

    @Test
    void datafile_seIndicaPorNumero() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/backups", 7);
        s.addComponent(component(ComponentType.DATAFILE, "4"));

        String script = builder.buildScript(s, backupType(BackupType.FULL, false));

        assertThat(script).contains("BACKUP DATAFILE 4");
    }

    @Test
    void controlfileYSpfile_segeneran() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/backups", 7);
        s.addComponent(component(ComponentType.CONTROLFILE, null));
        s.addComponent(component(ComponentType.SPFILE, null));

        String script = builder.buildScript(s, backupType(BackupType.FULL, false));

        assertThat(script).contains("CURRENT CONTROLFILE");
        assertThat(script).contains("SPFILE");
    }

    // ---------- ARCHIVELOG / NOARCHIVELOG ----------

    @Test
    void noarchivelog_omitePlusArchivelog() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.NOARCHIVELOG, "C:/backups", 7);
        s.addComponent(component(ComponentType.DB, null));

        String script = builder.buildScript(s, backupType(BackupType.FULL, false));

        assertThat(script).doesNotContain("PLUS ARCHIVELOG");
    }

    @Test
    void respaldoDeArchivelog_enNoarchivelog_lanzaError() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.NOARCHIVELOG, "C:/backups", 7);

        assertThatThrownBy(() -> builder.buildScript(s, backupType(BackupType.ARCHIVELOG, false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOARCHIVELOG");
    }

    @Test
    void tipoArchivelog_enArchivelog_respaldaLosLogs() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/backups", 7);

        String script = builder.buildScript(s, backupType(BackupType.ARCHIVELOG, false));

        assertThat(script).contains("BACKUP ARCHIVELOG ALL NOT BACKED UP 1 TIMES");
    }

    // ---------- retención ----------

    @Test
    void conRetencion_agregaDeleteObsolete() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/backups", 7);
        s.addComponent(component(ComponentType.DB, null));

        String script = builder.buildScript(s, backupType(BackupType.FULL, false));

        assertThat(script).contains("DELETE NOPROMPT OBSOLETE RECOVERY WINDOW OF 7 DAYS");
        assertThat(script).doesNotContain("CONFIGURE RETENTION POLICY");
    }

    // ---------- seguridad (inyección) ----------

    @Test
    void tablespaceConInyeccion_sRechaza() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/backups", 7);
        s.addComponent(component(ComponentType.TABLESPACE, "USERS; HOST 'calc'"));

        assertThatThrownBy(() -> builder.buildScript(s, backupType(BackupType.FULL, false)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void datafileQueNoEsNumero_seRechaza() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/backups", 7);
        s.addComponent(component(ComponentType.DATAFILE, "users01.dbf"));

        assertThatThrownBy(() -> builder.buildScript(s, backupType(BackupType.FULL, false)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void destinoConComillas_seRechaza() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/x'; HOST 'calc", 7);
        s.addComponent(component(ComponentType.DB, null));

        assertThatThrownBy(() -> builder.buildScript(s, backupType(BackupType.FULL, false)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- configuración incompleta ----------

    @Test
    void sinComponentes_lanzaError() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, "C:/backups", 7);

        assertThatThrownBy(() -> builder.buildScript(s, backupType(BackupType.FULL, false)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sinDestino_lanzaError() {
        Strategy s = strategy(DatabaseTarget.ArchiveMode.ARCHIVELOG, null, 7);
        s.addComponent(component(ComponentType.DB, null));

        assertThatThrownBy(() -> builder.buildScript(s, backupType(BackupType.FULL, false)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}