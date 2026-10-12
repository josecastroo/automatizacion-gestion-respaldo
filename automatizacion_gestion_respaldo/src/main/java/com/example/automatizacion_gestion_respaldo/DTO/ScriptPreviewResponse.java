package com.example.automatizacion_gestion_respaldo.DTO;

import com.example.automatizacion_gestion_respaldo.domain.BackupType;

public record ScriptPreviewResponse(Long strategyId, BackupType backupType, String script) {}