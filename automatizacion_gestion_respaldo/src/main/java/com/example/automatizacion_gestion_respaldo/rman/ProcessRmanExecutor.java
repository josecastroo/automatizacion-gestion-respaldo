package com.example.automatizacion_gestion_respaldo.rman;

import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ejecutor real: lanza el cliente "rman" del host.
 * La contraseña NUNCA va en el script, en la línea de comandos ni en el log:
 * se descifra en memoria y se envía por la entrada estándar del proceso.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rman.executor.mode", havingValue = "real")
public class ProcessRmanExecutor implements RmanExecutor {

    private static final Pattern HOST = Pattern.compile("^[A-Za-z0-9._-]{1,255}$");
    private static final Pattern SERVICE = Pattern.compile("^[A-Za-z0-9._$#-]{1,128}$");
    private static final Pattern DB_USER = Pattern.compile("^[A-Za-z][A-Za-z0-9_$#]{0,127}$");
    private static final Pattern PIECE = Pattern.compile("piece handle=(.+?)\\s+tag=");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final StringEncryptor encryptor;

    @Value("${rman.executable:rman}")
    private String executable;

    @Value("${rman.logs-dir:./logs/rman}")
    private String logsDir;

    @Value("${rman.scripts-dir:./scripts/rman}")
    private String scriptsDir;

    @Value("${rman.timeout-minutes:240}")
    private long timeoutMinutes;

    public ProcessRmanExecutor(StringEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public ExecutionResult execute(DatabaseTarget target, String scriptContent, String destinationPath) {
        LocalDateTime start = LocalDateTime.now();
        String stamp = start.format(STAMP);
        String password = null;
        int exitCode;
        String outputLog;

        try {
            validateTarget(target);
            password = encryptor.decrypt(target.getEncryptedCredential());
            if (password == null || password.isEmpty() || password.contains("\"")
                    || password.contains("\n") || password.contains("\r")) {
                throw new IllegalArgumentException("La credencial de la base objetivo no es válida para RMAN");
            }

            Path scripts = Path.of(scriptsDir).toAbsolutePath();
            Path logs = Path.of(logsDir).toAbsolutePath();
            Files.createDirectories(scripts);
            Files.createDirectories(logs);

            String prefix = "exec_" + target.getId() + "_" + stamp;
            Path scriptFile = scripts.resolve(prefix + ".rman");
            Path logFile = logs.resolve(prefix + ".log");
            Path consoleFile = logs.resolve(prefix + ".console.txt");
            if (scriptFile.toString().contains("'")) {
                throw new IllegalArgumentException("La ruta de scripts no puede contener comillas simples");
            }

            // RUN { }: si un comando falla, los siguientes (p. ej. DELETE OBSOLETE) no se ejecutan
            Files.writeString(scriptFile, "RUN {\n" + scriptContent + "}\n", StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder(executable, "log=" + logFile);
            pb.redirectErrorStream(true);
            pb.redirectOutput(consoleFile.toFile());
            Process process = pb.start();

            String connect = "CONNECT TARGET " + target.getDbUser() + "/\"" + password + "\"@//"
                    + target.getHost() + ":" + target.getPort() + "/" + target.getServiceName() + ";\n";
            try (Writer w = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                w.write(connect);
                w.write("@'" + scriptFile + "'\n");
                w.write("EXIT;\n");
            }

            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            String timeoutNote = "";
            if (finished) {
                exitCode = process.exitValue();
            } else {
                process.destroyForcibly();
                exitCode = -1;
                timeoutNote = "\nEJECUCIÓN CANCELADA: superó el tiempo máximo de " + timeoutMinutes + " minutos\n";
            }

            outputLog = readLog(logFile, consoleFile) + timeoutNote;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            exitCode = -1;
            outputLog = "Ejecución interrumpida\n";
        } catch (Exception e) {
            exitCode = -1;
            outputLog = "No se pudo ejecutar RMAN: " + e.getMessage() + "\n";
            log.error("Fallo al ejecutar RMAN para la base {}", target.getName(), e);
        }

        outputLog = maskPassword(outputLog, password);

        LocalDateTime end = LocalDateTime.now();
        List<ExecutionResult.GeneratedFile> files = findGeneratedFiles(outputLog);
        long total = files.stream()
                .filter(f -> f.sizeBytes() != null)
                .mapToLong(ExecutionResult.GeneratedFile::sizeBytes)
                .sum();

        return ExecutionResult.builder()
                .outputLog(outputLog)
                .exitCode(exitCode)
                .startTime(start)
                .endTime(end)
                .durationSeconds(Duration.between(start, end).getSeconds())
                .backupLocation(destinationPath)
                .totalSizeBytes(total)
                .files(files)
                .build();
    }

    private void validateTarget(DatabaseTarget t) {
        if (t == null || t.getHost() == null || !HOST.matcher(t.getHost()).matches()
                || t.getPort() == null || t.getPort() < 1 || t.getPort() > 65535
                || t.getServiceName() == null || !SERVICE.matcher(t.getServiceName()).matches()
                || t.getDbUser() == null || !DB_USER.matcher(t.getDbUser()).matches()
                || t.getEncryptedCredential() == null) {
            throw new IllegalArgumentException("Datos de conexión de la base objetivo inválidos o incompletos");
        }
    }

    private String readLog(Path logFile, Path consoleFile) throws IOException {
        if (Files.exists(logFile) && Files.size(logFile) > 0) {
            return Files.readString(logFile, StandardCharsets.UTF_8);
        }
        return Files.exists(consoleFile) ? Files.readString(consoleFile, StandardCharsets.UTF_8) : "";
    }

    /** RMAN ya oculta la contraseña en el log; esto es una segunda barrera. */
    private String maskPassword(String text, String password) {
        if (text == null || password == null || password.isEmpty()) return text;
        return text.replace("\"" + password + "\"", "\"****\"");
    }

    /** Lee las rutas de los "piece handle" del log y comprueba que existan (tamaño nulo = no encontrado). */
    private List<ExecutionResult.GeneratedFile> findGeneratedFiles(String outputLog) {
        List<ExecutionResult.GeneratedFile> result = new ArrayList<>();
        if (outputLog == null) return result;

        Set<String> paths = new LinkedHashSet<>();
        Matcher m = PIECE.matcher(outputLog);
        while (m.find()) {
            paths.add(m.group(1).trim());
        }
        for (String p : paths) {
            Long size = null;
            try {
                Path path = Path.of(p);
                if (Files.exists(path)) {
                    size = Files.size(path);
                }
            } catch (InvalidPathException | IOException ignored) {
                // se registra con tamaño nulo: no se pudo verificar
            }
            result.add(new ExecutionResult.GeneratedFile(p, size));
        }
        return result;
    }
}