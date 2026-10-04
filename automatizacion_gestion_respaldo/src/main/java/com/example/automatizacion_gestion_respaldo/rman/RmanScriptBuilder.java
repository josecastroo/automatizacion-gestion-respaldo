package com.example.automatizacion_gestion_respaldo.rman;

import com.example.automatizacion_gestion_respaldo.domain.Strategy;
import com.example.automatizacion_gestion_respaldo.domain.StrategyBackupType;
import com.example.automatizacion_gestion_respaldo.domain.StrategyComponent;
import org.springframework.stereotype.Component;

@Component
public class RmanScriptBuilder {

    public String buildScript(Strategy strategy, StrategyBackupType backupType) {
        StringBuilder script = new StringBuilder();

        // 1. Configuracion de retencion
        if (strategy.getRetentionDays() != null && strategy.getRetentionDays() > 0) {
            script.append(String.format("CONFIGURE RETENTION POLICY TO RECOVERY WINDOW OF %d DAYS;\n", strategy.getRetentionDays()));
        }

        // 2. Tipo de respaldo general (Base de datos completa / Incrementales)
        if (backupType != null) {
            switch (backupType.getBackupType()) {
                case "FULL":
                    script.append("BACKUP DATABASE PLUS ARCHIVELOG;\n");
                    break;
                case "INC_0":
                    script.append("BACKUP INCREMENTAL LEVEL 0 DATABASE PLUS ARCHIVELOG;\n");
                    break;
                case "INC_1_DIF":
                    script.append("BACKUP INCREMENTAL LEVEL 1 DATABASE;\n");
                    break;
                case "INC_1_ACUM":
                    script.append("BACKUP INCREMENTAL LEVEL 1 CUMULATIVE DATABASE;\n");
                    break;
                case "ARCHIVELOG":
                    script.append("BACKUP ARCHIVELOG ALL NOT BACKED UP 1 TIMES;\n");
                    break;
            }
        }

        // 3. Componentes individuales (si los hay)
        if (strategy.getComponents() != null) {
            for (StrategyComponent component : strategy.getComponents()) {
                switch (component.getComponentType()) {
                    case "TABLESPACE":
                        script.append(String.format("BACKUP TABLESPACE %s;\n", component.getObjectName()));
                        break;
                    case "DATAFILE":
                        script.append(String.format("BACKUP DATAFILE %s;\n", component.getObjectName()));
                        break;
                    case "CONTROLFILE":
                        script.append("BACKUP CURRENT CONTROLFILE;\n");
                        break;
                    case "SPFILE":
                        script.append("BACKUP SPFILE;\n");
                        break;
                }
            }
        }

        // 4. Limpieza de obsoletos si hay retención
        if (strategy.getRetentionDays() != null && strategy.getRetentionDays() > 0) {
            script.append("DELETE NOPROMPT OBSOLETE;\n");
        }

        return script.toString();
    }
}
