package ro.unibuc.prodeng.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.ClientEntity;
import ro.unibuc.prodeng.repository.ClientRepository;
import ro.unibuc.prodeng.request.CreateClientRequest;
import ro.unibuc.prodeng.request.UpdateClientRequest;
import ro.unibuc.prodeng.response.ClientResponse;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    void testGetAllClients_returnsAllClients() {
        // Arrange
        ClientEntity c1 = new ClientEntity("1", "Client 1", "c1@example.com", 1, 0);
        ClientEntity c2 = new ClientEntity("2", "Client 2", "c2@example.com", 0, 1);
        when(clientRepository.findAll()).thenReturn(Arrays.asList(c1, c2));

        // Act
        List<ClientResponse> responses = clientService.getAllClients();

        // Assert
        assertEquals(2, responses.size());
        assertEquals("Client 1", responses.get(0).name());
        assertEquals(1.0, responses.get(0).reputationScore());
        assertEquals(0.0, responses.get(1).reputationScore());
    }

    @Test
    void testGetClientById_existing_returnsClient() {
        // Arrange
        ClientEntity c = new ClientEntity("1", "Client", "c@example.com", 2, 2);
        when(clientRepository.findById("1")).thenReturn(Optional.of(c));

        // Act
        ClientResponse r = clientService.getClientById("1");

        // Assert
        assertEquals("Client", r.name());
        assertEquals(0.5, r.reputationScore());
    }

    @Test
    void testGetClientById_notFound_throwsException() {
        // Arrange
        when(clientRepository.findById("1")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> clientService.getClientById("1"));
    }

    @ParameterizedTest
    @CsvSource({
        "3, 2, 0.6",    // Scenario 4: 3 / 5
        "10, 0, 1.0",   // Perfect reputation
        "0, 5, 0.0",    // Terrible reputation
        "1, 3, 0.25",   // 1 / 4
        "99, 1, 0.99"   // 99 / 100
    })
    void testReputationCalculation_mixedProjects_returnsCorrectRatio(int completed, int cancelled, double expectedScore) {
        // Arrange
        ClientEntity c = new ClientEntity("1", "Client", "c@example.com", completed, cancelled);
        when(clientRepository.findById("1")).thenReturn(Optional.of(c));

        // Act
        ClientResponse r = clientService.getClientById("1");

        // Assert
        assertEquals(expectedScore, r.reputationScore(), 0.0001);
    }

    @Test
    void testReputationCalculation_noProjects_returnsZeroSafe() {
        // Arrange
        // Edge case: 0 total projects should not throw DivideByZero exception
        ClientEntity c = new ClientEntity("1", "Client", "c@example.com", 0, 0);
        when(clientRepository.findById("1")).thenReturn(Optional.of(c));

        // Act
        ClientResponse r = clientService.getClientById("1");

        // Assert
        assertEquals(0.0, r.reputationScore(), 0.0001);
    }

    @Test
    void testCreateClient_valid_savesClient() {
        // Arrange
        CreateClientRequest req = new CreateClientRequest("New Client", "new@example.com");
        when(clientRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> {
            ClientEntity e = inv.getArgument(0);
            return new ClientEntity("gen-id", e.name(), e.email(), e.completedProjects(), e.cancelledProjects());
        });

        ClientResponse r = clientService.createClient(req);
        assertEquals("gen-id", r.id());
        assertEquals("New Client", r.name());
    }

    @Test
    void testCreateClient_emailTaken_throwsException() {
        // Arrange
        CreateClientRequest req = new CreateClientRequest("New Client", "taken@example.com");
        when(clientRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(new ClientEntity("1", "", "", 0, 0)));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> clientService.createClient(req));
    }
    
    @Test
    void testCreateClient_duplicateKey_throwsException() {
        // Arrange
        CreateClientRequest req = new CreateClientRequest("New Client", "new@example.com");
        when(clientRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(clientRepository.save(any(ClientEntity.class))).thenThrow(new DuplicateKeyException("duplicate"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> clientService.createClient(req));
    }

    @Test
    void testUpdateClient_valid_savesClient() {
        // Arrange
        UpdateClientRequest req = new UpdateClientRequest("Updated", "updated@example.com");
        ClientEntity existing = new ClientEntity("1", "Old", "old@example.com", 1, 1);
        when(clientRepository.findById("1")).thenReturn(Optional.of(existing));
        when(clientRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
        
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ClientResponse r = clientService.updateClient("1", req);

        // Assert
        assertEquals("Updated", r.name());
        assertEquals("updated@example.com", r.email());
        assertEquals(1, r.completedProjects());
    }

    @Test
    void testUpdateClient_emailTaken_throwsException() {
        // Arrange
        UpdateClientRequest req = new UpdateClientRequest("Updated", "taken@example.com");
        ClientEntity existing = new ClientEntity("1", "Old", "old@example.com", 1, 1);
        when(clientRepository.findById("1")).thenReturn(Optional.of(existing));
        when(clientRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(new ClientEntity("2", "", "", 0, 0)));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> clientService.updateClient("1", req));
    }

    @Test
    void testUpdateClient_sameEmail_savesClient() {
        // Arrange
        UpdateClientRequest req = new UpdateClientRequest("Updated", "old@example.com");
        ClientEntity existing = new ClientEntity("1", "Old", "old@example.com", 1, 1);
        when(clientRepository.findById("1")).thenReturn(Optional.of(existing));
        
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ClientResponse r = clientService.updateClient("1", req);

        // Assert
        assertEquals("Updated", r.name());
        assertEquals("old@example.com", r.email());
        assertEquals(1, r.completedProjects());
        verify(clientRepository, never()).findByEmail(anyString());
    }

    @Test
    void testUpdateClient_duplicateKey_throwsException() {
        // Arrange
        UpdateClientRequest req = new UpdateClientRequest("Updated", "updated@example.com");
        ClientEntity existing = new ClientEntity("1", "Old", "old@example.com", 1, 1);
        when(clientRepository.findById("1")).thenReturn(Optional.of(existing));
        when(clientRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(clientRepository.save(any(ClientEntity.class))).thenThrow(new DuplicateKeyException("duplicate"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> clientService.updateClient("1", req));
    }

    @Test
    void testDeleteClient_existing_deletes() {
        // Arrange
        when(clientRepository.existsById("1")).thenReturn(true);

        // Act
        clientService.deleteClient("1");

        // Assert
        verify(clientRepository).deleteById("1");
    }

    @Test
    void testDeleteClient_notFound_throwsException() {
        // Arrange
        when(clientRepository.existsById("1")).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> clientService.deleteClient("1"));
    }

    @Test
    void testIncrementCompleted_existing_increments() {
        // Arrange
        when(clientRepository.existsById("1")).thenReturn(true);

        // Act
        clientService.incrementCompleted("1");

        // Assert
        verify(clientRepository).incrementCompletedProjects("1");
    }

    @Test
    void testIncrementCompleted_notFound_throwsException() {
        // Arrange
        when(clientRepository.existsById("1")).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> clientService.incrementCompleted("1"));
    }

    @Test
    void testIncrementCancelled_existing_increments() {
        // Arrange
        when(clientRepository.existsById("1")).thenReturn(true);

        // Act
        clientService.incrementCancelled("1");

        // Assert
        verify(clientRepository).incrementCancelledProjects("1");
    }

    @Test
    void testIncrementCancelled_notFound_throwsException() {
        // Arrange
        when(clientRepository.existsById("1")).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> clientService.incrementCancelled("1"));
    }
}
