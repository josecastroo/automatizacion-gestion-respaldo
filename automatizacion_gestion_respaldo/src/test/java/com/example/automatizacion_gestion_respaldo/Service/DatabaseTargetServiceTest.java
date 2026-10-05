package com.example.automatizacion_gestion_respaldo.Service;

import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import com.example.automatizacion_gestion_respaldo.domain.Priority;
import com.example.automatizacion_gestion_respaldo.DTO.DatabaseTargetRequest;
import com.example.automatizacion_gestion_respaldo.DTO.DatabaseTargetResponse;
import com.example.automatizacion_gestion_respaldo.Exception.BusinessException;
import com.example.automatizacion_gestion_respaldo.Repository.DatabaseTargetRepository;
import com.example.automatizacion_gestion_respaldo.Repository.StrategyRepository;
import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseTargetServiceTest {

    @Mock DatabaseTargetRepository repository;
    @Mock StrategyRepository strategyRepository;
    @Mock StringEncryptor encryptor;
    @InjectMocks DatabaseTargetService service;

    private DatabaseTargetRequest request(String password) {
        return new DatabaseTargetRequest("PROD", "localhost", 1521, "XE", "sys",
                password, Priority.ALTA, true);
    }

    @Test
    void create_cifraLaContrasena() {
        when(repository.existsByName("PROD")).thenReturn(false);
        when(encryptor.encrypt("secreto")).thenReturn("CIFRADO");
        when(repository.save(any(DatabaseTarget.class))).thenAnswer(i -> i.getArgument(0));

        DatabaseTargetResponse response = service.create(request("secreto"));

        ArgumentCaptor<DatabaseTarget> captor = ArgumentCaptor.forClass(DatabaseTarget.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEncryptedCredential()).isEqualTo("CIFRADO");
        assertThat(response.name()).isEqualTo("PROD");
    }

    @Test
    void laRespuesta_noExponeLaCredencial() {
        assertThat(Arrays.stream(DatabaseTargetResponse.class.getRecordComponents())
                .map(RecordComponent::getName))
                .doesNotContain("password", "encryptedCredential");
    }

    @Test
    void create_conNombreDuplicado_seRechaza() {
        when(repository.existsByName("PROD")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("secreto")))
                .isInstanceOf(BusinessException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_sinContrasena_seRechaza() {
        assertThatThrownBy(() -> service.create(request(null)))
                .isInstanceOf(BusinessException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void update_sinContrasena_conservaLaCredencial() {
        DatabaseTarget existing = new DatabaseTarget();
        existing.setEncryptedCredential("CIFRADO_VIEJO");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameAndIdNot("PROD", 1L)).thenReturn(false);
        when(repository.save(any(DatabaseTarget.class))).thenAnswer(i -> i.getArgument(0));

        service.update(1L, request(null));

        assertThat(existing.getEncryptedCredential()).isEqualTo("CIFRADO_VIEJO");
        verify(encryptor, never()).encrypt(anyString());
    }

    @Test
    void delete_conEstrategiasAsociadas_seRechaza() {
        when(repository.findById(1L)).thenReturn(Optional.of(new DatabaseTarget()));
        when(strategyRepository.existsByDatabaseTargetId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessException.class);
        verify(repository, never()).delete(any());
    }
}