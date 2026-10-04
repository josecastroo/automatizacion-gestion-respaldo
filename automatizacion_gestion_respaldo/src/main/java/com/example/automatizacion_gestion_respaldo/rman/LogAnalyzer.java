package com.example.automatizacion_gestion_respaldo.rman;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LogAnalyzer {

    private static final Pattern RMAN_ERROR_PATTERN = Pattern.compile("(RMAN-\\d{5}.*|ORA-\\d{5}.*)");

    public String determineStatus(int exitCode, String log) {
        if (exitCode != 0) {
            return "ERROR";
        }
        
        if (log != null && log.contains("RMAN-") || log.contains("ORA-")) {
            // A veces el exitCode es 0 pero hay advertencias o errores parciales
            if (log.contains("ERROR")) {
                return "ERROR";
            }
            return "ADVERTENCIA";
        }

        return "EXITOSO";
    }

    public String extractErrors(String log) {
        if (log == null) return null;
        
        List<String> errors = new ArrayList<>();
        Matcher matcher = RMAN_ERROR_PATTERN.matcher(log);
        
        while (matcher.find()) {
            errors.add(matcher.group(1));
        }
        
        return errors.isEmpty() ? null : String.join("\n", errors);
    }
}
