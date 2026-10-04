package com.example.automatizacion_gestion_respaldo.rman;

import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;

public interface RmanExecutor {
    
    /**
     * Ejecuta un script RMAN contra una base de datos objetivo.
     * @param target La base de datos objetivo
     * @param scriptContent El contenido del script RMAN a ejecutar
     * @return El resultado de la ejecución incluyendo el log
     */
    ExecutionResult execute(DatabaseTarget target, String scriptContent);
}
