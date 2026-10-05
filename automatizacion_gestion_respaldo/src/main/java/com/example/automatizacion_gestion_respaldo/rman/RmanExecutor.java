package com.example.automatizacion_gestion_respaldo.rman;

import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;

public interface RmanExecutor {

    /**
     * Ejecuta un script RMAN contra una base de datos objetivo.
     * @param target base de datos objetivo
     * @param scriptContent contenido del script RMAN
     * @param destinationPath carpeta de destino de los respaldos
     * @return resultado con log, código de salida, tiempos y archivos generados
     */
    ExecutionResult execute(DatabaseTarget target, String scriptContent, String destinationPath);
}