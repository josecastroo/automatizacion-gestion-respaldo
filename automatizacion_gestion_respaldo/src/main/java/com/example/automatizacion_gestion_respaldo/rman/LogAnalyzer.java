package com.example.automatizacion_gestion_respaldo.rman;

import com.example.automatizacion_gestion_respaldo.domain.ExecutionStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LogAnalyzer {

    private static final Pattern RMAN_ERROR_PATTERN = Pattern.compile("(RMAN-\\d{5}.*|ORA-\\d{5}.*)");
    private static final String ERROR_STACK = "RMAN-00569";
    private static final String COMPLETE_MARKER = "Recovery Manager complete";

    public ExecutionStatus determineStatus(int exitCode, String log) {
        if (exitCode != 0) {
            return ExecutionStatus.ERROR;
        }
        if (log == null || log.isBlank()) {
            return ExecutionStatus.ADVERTENCIA; // sin log no hay evidencia suficiente
        }
        if (log.contains(ERROR_STACK)) {
            return ExecutionStatus.ERROR;
        }
        if (RMAN_ERROR_PATTERN.matcher(log).find() || !log.contains(COMPLETE_MARKER)) {
            return ExecutionStatus.ADVERTENCIA;
        }
        return ExecutionStatus.EXITOSO;
    }

    public String extractErrors(String log) {
        if (log == null) return null;

        List<String> errors = new ArrayList<>();
        Matcher matcher = RMAN_ERROR_PATTERN.matcher(log);
        while (matcher.find()) {
            String line = matcher.group(1).trim();
            if (!line.contains("=====")) { // ignora las líneas decorativas de RMAN
                errors.add(line);
            }
        }
        return errors.isEmpty() ? null : String.join("\n", errors);
    }
}