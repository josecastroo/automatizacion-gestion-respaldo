package com.example.automatizacion_gestion_respaldo.Service;

import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import com.example.automatizacion_gestion_respaldo.DTO.DatabaseTargetRequest;
import com.example.automatizacion_gestion_respaldo.DTO.DatabaseTargetResponse;
import com.example.automatizacion_gestion_respaldo.Exception.BusinessException;
import com.example.automatizacion_gestion_respaldo.Exception.NotFoundException;
import com.example.automatizacion_gestion_respaldo.Repository.DatabaseTargetRepository;
import com.example.automatizacion_gestion_respaldo.Repository.StrategyRepository;
import lombok.RequiredArgsConstructor;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseTargetService {

    private final DatabaseTargetRepository repository;
    private final StrategyRepository strategyRepository;
    private final StringEncryptor encryptor;

    @Transactional(readOnly = true)
    public List<DatabaseTargetResponse> list() {
        return repository.findAll().stream().map(DatabaseTargetResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DatabaseTargetResponse get(Long id) {
        return DatabaseTargetResponse.from(find(id));
    }

    @Transactional
    public DatabaseTargetResponse create(DatabaseTargetRequest request) {
        if (repository.existsByName(request.name().trim())) {
            throw new BusinessException("Ya existe una base de datos con ese nombre");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("La contraseña es obligatoria al registrar una base de datos");
        }
        DatabaseTarget target = new DatabaseTarget();
        apply(target, request);
        target.setEncryptedCredential(encryptor.encrypt(request.password()));
        return DatabaseTargetResponse.from(repository.save(target));
    }

    @Transactional
    public DatabaseTargetResponse update(Long id, DatabaseTargetRequest request) {
        DatabaseTarget target = find(id);
        if (repository.existsByNameAndIdNot(request.name().trim(), id)) {
            throw new BusinessException("Ya existe otra base de datos con ese nombre");
        }
        apply(target, request);
        // Si no se envía contraseña, se conserva la actual
        if (request.password() != null && !request.password().isBlank()) {
            target.setEncryptedCredential(encryptor.encrypt(request.password()));
        }
        return DatabaseTargetResponse.from(repository.save(target));
    }

    @Transactional
    public DatabaseTargetResponse setActive(Long id, boolean active) {
        DatabaseTarget target = find(id);
        target.setActive(active);
        return DatabaseTargetResponse.from(repository.save(target));
    }

    /** Para la auditoría ARCHIVELOG/NOARCHIVELOG (la detección automática se hará en otro servicio). */
    @Transactional
    public DatabaseTargetResponse updateArchiveMode(Long id, DatabaseTarget.ArchiveMode mode) {
        DatabaseTarget target = find(id);
        target.setArchiveMode(mode);
        return DatabaseTargetResponse.from(repository.save(target));
    }

    @Transactional
    public void delete(Long id) {
        DatabaseTarget target = find(id);
        if (strategyRepository.existsByDatabaseTargetId(id)) {
            throw new BusinessException(
                    "La base tiene estrategias asociadas; desactívela en lugar de eliminarla");
        }
        repository.delete(target);
    }

    private DatabaseTarget find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Base de datos no encontrada: " + id));
    }

    private void apply(DatabaseTarget t, DatabaseTargetRequest r) {
        t.setName(r.name().trim());
        t.setHost(r.host());
        t.setPort(r.port());
        t.setServiceName(r.serviceName());
        t.setDbUser(r.dbUser());
        t.setPriority(r.priority());
        if (r.active() != null) {
            t.setActive(r.active());
        }
    }
}