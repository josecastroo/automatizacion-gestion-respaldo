package com.example.automatizacion_gestion_respaldo.rman;

import com.example.automatizacion_gestion_respaldo.domain.BackupType;
import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import com.example.automatizacion_gestion_respaldo.domain.Strategy;
import com.example.automatizacion_gestion_respaldo.domain.StrategyBackupType;
import com.example.automatizacion_gestion_respaldo.domain.StrategyComponent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class RmanScriptBuilder {

    private static final Pattern TABLESPACE_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_$#]{0,29}$");
    private static final Pattern DATAFILE_NUMBER = Pattern.compile("^\\d{1,5}$");
    private static final Pattern SAFE_PATH = Pattern.compile("^[A-Za-z0-9_\\-./\\\\: ]{1,255}$");

    /** Debe llamarse dentro de una transacción (lee relaciones perezosas). */
    public String buildScript(Strategy strategy, StrategyBackupType backupType) {
        if (strategy == null || backupType == null || backupType.getBackupType() == null) {
            throw new IllegalArgumentException("Se requiere una estrategia y un tipo de respaldo");
        }
        BackupType type = backupType.getBackupType();
        String format = formatClause(strategy.getDestinationPath(), type);
        String compress = Boolean.TRUE.equals(backupType.getCompressed()) ? "AS COMPRESSED BACKUPSET " : "";
        boolean archiveAllowed = strategy.getDatabaseTarget() == null
                || strategy.getDatabaseTarget().getArchiveMode() != DatabaseTarget.ArchiveMode.NOARCHIVELOG;

        StringBuilder script = new StringBuilder();

        // Tipo ARCHIVELOG: solo respalda archived redo logs
        if (type == BackupType.ARCHIVELOG) {
            requireArchiveMode(archiveAllowed);
            script.append(archivelogCommand(compress, format));
            appendRetention(script, strategy.getRetentionDays(), false);
            return script.toString();
        }

        List<StrategyComponent> components = strategy.getComponents();
        if (components == null || components.isEmpty()) {
            throw new IllegalStateException("La estrategia no tiene componentes que respaldar");
        }

        String level = levelClause(type);
        for (StrategyComponent c : components) {
            if (c.getComponentType() == null) {
                throw new IllegalStateException("Hay un componente sin tipo");
            }
            switch (c.getComponentType()) {
                case DB -> {
                    boolean plusArchive = archiveAllowed && (type == BackupType.FULL || type == BackupType.INC_0);
                    script.append(backup(compress, level, "DATABASE", format, plusArchive ? " PLUS ARCHIVELOG" : ""));
                }
                case TABLESPACE -> {
                    String name = c.getObjectName();
                    if (name == null || !TABLESPACE_NAME.matcher(name).matches()) {
                        throw new IllegalArgumentException("Nombre de tablespace inválido");
                    }
                    script.append(backup(compress, level, "TABLESPACE " + name, format, ""));
                }
                case DATAFILE -> {
                    String number = c.getObjectName();
                    if (number == null || !DATAFILE_NUMBER.matcher(number).matches()) {
                        throw new IllegalArgumentException("El datafile debe indicarse por su número");
                    }
                    script.append(backup(compress, level, "DATAFILE " + number, format, ""));
                }
                case CONTROLFILE -> script.append("BACKUP ").append(compress)
                        .append("CURRENT CONTROLFILE ").append(format).append(";\n");
                case SPFILE -> script.append("BACKUP ").append(compress)
                        .append("SPFILE ").append(format).append(";\n");
                case ARCHIVELOG -> {
                    requireArchiveMode(archiveAllowed);
                    script.append(archivelogCommand(compress, format));
                }
            }
        }

        appendRetention(script, strategy.getRetentionDays(), true);
        return script.toString();
    }

    private String backup(String compress, String level, String object, String format, String plusArchive) {
        return "BACKUP " + compress + level + object + " " + format + plusArchive + ";\n";
    }

    private String archivelogCommand(String compress, String format) {
        return "BACKUP " + compress + "ARCHIVELOG ALL NOT BACKED UP 1 TIMES " + format + ";\n";
    }

    private String levelClause(BackupType type) {
        return switch (type) {
            case INC_0 -> "INCREMENTAL LEVEL 0 ";
            case INC_1_DIF -> "INCREMENTAL LEVEL 1 ";
            case INC_1_ACUM -> "INCREMENTAL LEVEL 1 CUMULATIVE ";
            default -> "";
        };
    }

    private void appendRetention(StringBuilder script, Integer days, boolean enabled) {
        if (enabled && days != null && days > 0) {
            script.append("DELETE NOPROMPT OBSOLETE RECOVERY WINDOW OF ").append(days).append(" DAYS;\n");
        }
    }

    private void requireArchiveMode(boolean archiveAllowed) {
        if (!archiveAllowed) {
            throw new IllegalStateException(
                    "La base está en NOARCHIVELOG: no se pueden respaldar archived redo logs");
        }
    }

    private String formatClause(String destination, BackupType type) {
        if (destination == null || !SAFE_PATH.matcher(destination).matches()) {
            throw new IllegalArgumentException("Ruta de destino inválida o vacía");
        }
        String base = destination.replaceAll("[/\\\\]+$", "");
        String sep = destination.contains("\\") ? "\\" : "/";
        return "FORMAT '" + base + sep + "%d_" + type.name() + "_%T_%U.bkp'";
    }
}