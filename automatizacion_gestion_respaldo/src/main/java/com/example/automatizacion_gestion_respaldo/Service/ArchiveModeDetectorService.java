package com.example.automatizacion_gestion_respaldo.Service;

import com.example.automatizacion_gestion_respaldo.Exception.BusinessException;
import com.example.automatizacion_gestion_respaldo.Repository.DatabaseTargetRepository;
import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import lombok.RequiredArgsConstructor;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Service
@RequiredArgsConstructor
public class ArchiveModeDetectorService {

    private final DatabaseTargetRepository repository;
    private final StringEncryptor encryptor;

    @Transactional
    public DatabaseTarget.ArchiveMode detectAndUpdateMode(Long databaseId) {
        DatabaseTarget target = repository.findById(databaseId)
                .orElseThrow(() -> new BusinessException("Base de datos no encontrada: " + databaseId));

        // 1. Desencriptar la contraseña para poder conectarnos
        String password = encryptor.decrypt(target.getEncryptedCredential());

        // 2. Armar la cadena de conexión JDBC para Oracle
        String jdbcUrl = String.format("jdbc:oracle:thin:@//%s:%d/%s",
                target.getHost(), target.getPort(), target.getServiceName());

        // 3. Conectarse y consultar la vista v$database
        try (Connection conn = DriverManager.getConnection(jdbcUrl, target.getDbUser(), password);
             PreparedStatement stmt = conn.prepareStatement("SELECT log_mode FROM v$database");
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String logMode = rs.getString("log_mode");

                DatabaseTarget.ArchiveMode mode = "ARCHIVELOG".equalsIgnoreCase(logMode)
                        ? DatabaseTarget.ArchiveMode.ARCHIVELOG
                        : DatabaseTarget.ArchiveMode.NOARCHIVELOG;

                target.setArchiveMode(mode);
                repository.save(target);
                return mode;
            }

        } catch (Exception e) {
            // Si falla la conexión o el usuario no tiene permisos sobre v$database
            target.setArchiveMode(DatabaseTarget.ArchiveMode.DESCONOCIDO);
            repository.save(target);
            throw new BusinessException("No se pudo detectar el modo de archivado. Verifique la conexión y los permisos de sysdba. Error: " + e.getMessage());
        }

        return DatabaseTarget.ArchiveMode.DESCONOCIDO;
    }
}