package com.example.automatizacion_gestion_respaldo.DTO;

import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import com.example.automatizacion_gestion_respaldo.domain.Priority;

public record DatabaseTargetResponse(
        Long id,
        String name,
        String host,
        Integer port,
        String serviceName,
        String dbUser,
        Priority priority,
        DatabaseTarget.ArchiveMode archiveMode,
        Boolean active
) {
    public static DatabaseTargetResponse from(DatabaseTarget t) {
        return new DatabaseTargetResponse(t.getId(), t.getName(), t.getHost(), t.getPort(),
                t.getServiceName(), t.getDbUser(), t.getPriority(), t.getArchiveMode(), t.getActive());
    }
}