package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.model.ClientEntity;
import ro.unibuc.prodeng.repository.ClientRepository;
import ro.unibuc.prodeng.request.CreateClientRequest;
import ro.unibuc.prodeng.request.UpdateClientRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @AfterEach
    void cleanup() {
        clientRepository.deleteAll();
    }

    @Test
    void testGetAllClients_returnsList() throws Exception {
        // Arrange
        clientRepository.save(new ClientEntity(null, "Client 1", "client1@example.com", 0, 0));
        clientRepository.save(new ClientEntity(null, "Client 2", "client2@example.com", 0, 0));

        // Act & Assert
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Client 1")));
    }

    @Test
    void testGetClientById_returnsClient() throws Exception {
        // Arrange
        ClientEntity client = clientRepository.save(new ClientEntity(null, "Client 1", "client1@example.com", 0, 0));

        // Act & Assert
        mockMvc.perform(get("/api/clients/{id}", client.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Client 1")));
    }

    @Test
    void testCreateClient_returnsCreated() throws Exception {
        // Arrange
        CreateClientRequest req = new CreateClientRequest("New Client", "new@example.com");

        // Act & Assert
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Client")));
        
        assertThat(clientRepository.findAll()).hasSize(1);
    }

    @Test
    void testCreateClient_missingEmail_returnsBadRequest() throws Exception {
        // Arrange
        CreateClientRequest req = new CreateClientRequest("New Client", null);

        // Act & Assert
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateClient_returnsUpdated() throws Exception {
        // Arrange
        ClientEntity client = clientRepository.save(new ClientEntity(null, "Client 1", "client1@example.com", 0, 0));
        UpdateClientRequest req = new UpdateClientRequest("Updated Client", "updated@example.com");

        // Act & Assert
        mockMvc.perform(put("/api/clients/{id}", client.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Client")));
        
        ClientEntity updatedClient = clientRepository.findById(client.id()).orElseThrow();
        assertThat(updatedClient.name()).isEqualTo("Updated Client");
    }

    @Test
    void testDeleteClient_returnsNoContent() throws Exception {
        // Arrange
        ClientEntity client = clientRepository.save(new ClientEntity(null, "Client 1", "client1@example.com", 0, 0));

        // Act & Assert
        mockMvc.perform(delete("/api/clients/{id}", client.id()))
                .andExpect(status().isNoContent());
        
        assertThat(clientRepository.findById(client.id())).isEmpty();
    }

    @Test
    void testGetClientById_missingClient_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/clients/{id}", "unknown-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testCreateClient_duplicateEmail_returnsBadRequest() throws Exception {
        clientRepository.save(new ClientEntity(null, "Existing Client", "duplicate@example.com", 0, 0));
        CreateClientRequest req = new CreateClientRequest("New Client", "duplicate@example.com");

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testUpdateClient_missingClient_returnsNotFound() throws Exception {
        UpdateClientRequest req = new UpdateClientRequest("Updated Client", "updated@example.com");

        mockMvc.perform(put("/api/clients/{id}", "unknown-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
