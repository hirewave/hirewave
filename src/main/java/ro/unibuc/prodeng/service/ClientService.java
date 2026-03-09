package ro.unibuc.prodeng.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.ClientEntity;
import ro.unibuc.prodeng.repository.ClientRepository;
import ro.unibuc.prodeng.request.CreateClientRequest;
import ro.unibuc.prodeng.request.UpdateClientRequest;
import ro.unibuc.prodeng.response.ClientResponse;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    public List<ClientResponse> getAllClients() {
        return clientRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ClientResponse getClientById(String id) {
        return toResponse(getEntityById(id));
    }

    public ClientEntity getEntityById(String id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client " + id));
    }

    public ClientResponse createClient(CreateClientRequest request) {
        if (clientRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }
        ClientEntity entity = new ClientEntity(null, request.name(), request.email(), 0, 0);
        try {
            return toResponse(clientRepository.save(entity));
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }
    }

    public ClientResponse updateClient(String id, UpdateClientRequest request) {
        ClientEntity existing = getEntityById(id);
        if (!existing.email().equals(request.email())) {
            if (clientRepository.findByEmail(request.email()).isPresent()) {
                throw new IllegalArgumentException("Email already exists: " + request.email());
            }
        }
        ClientEntity updated = new ClientEntity(
                id, request.name(), request.email(),
                existing.completedProjects(), existing.cancelledProjects()
        );
        try {
            return toResponse(clientRepository.save(updated));
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }
    }

    public void deleteClient(String id) {
        if (!clientRepository.existsById(id)) {
            throw new EntityNotFoundException("Client " + id);
        }
        clientRepository.deleteById(id);
    }

    public void incrementCompleted(String clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new EntityNotFoundException("Client " + clientId);
        }
        clientRepository.incrementCompletedProjects(clientId);
    }

    public void incrementCancelled(String clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new EntityNotFoundException("Client " + clientId);
        }
        clientRepository.incrementCancelledProjects(clientId);
    }
    
    private ClientResponse toResponse(ClientEntity entity) {
        int total = entity.completedProjects() + entity.cancelledProjects();
        double reputation = total == 0 ? 0.0 : (double) entity.completedProjects() / total;
        return new ClientResponse(
                entity.id(), entity.name(), entity.email(),
                entity.completedProjects(), entity.cancelledProjects(),
                reputation
        );
    }
}
