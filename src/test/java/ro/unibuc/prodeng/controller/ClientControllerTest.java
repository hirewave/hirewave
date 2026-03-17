package ro.unibuc.prodeng.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.prodeng.request.CreateClientRequest;
import ro.unibuc.prodeng.request.UpdateClientRequest;
import ro.unibuc.prodeng.response.ClientResponse;
import ro.unibuc.prodeng.service.ClientService;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clientController).build();
    }

    @Test
    void testGetAllClients_returnsList() throws Exception {
        // Arrange
        ClientResponse r1 = new ClientResponse("1", "C1", "c1@ex.com", 1, 0, 1.0);
        when(clientService.getAllClients()).thenReturn(Arrays.asList(r1));

        // Act & Assert
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("C1")));
    }

    @Test
    void testGetClientById_returnsClient() throws Exception {
        // Arrange
        ClientResponse r1 = new ClientResponse("1", "C1", "c1@ex.com", 1, 0, 1.0);
        when(clientService.getClientById("1")).thenReturn(r1);

        // Act & Assert
        mockMvc.perform(get("/api/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("C1")));
    }

    @Test
    void testCreateClient_returnsCreated() throws Exception {
        // Arrange
        CreateClientRequest req = new CreateClientRequest("C1", "c1@ex.com");
        ClientResponse res = new ClientResponse("1", "C1", "c1@ex.com", 0, 0, 0.0);
        when(clientService.createClient(any(CreateClientRequest.class))).thenReturn(res);

        // Act & Assert
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("1")));
    }

    @Test
    void testCreateClient_missingEmail_returnsBadRequest() throws Exception {
        // Arrange
        // Email is missing/null, which should trigger a 400 validation error
        CreateClientRequest req = new CreateClientRequest("C1", null);

        // Act & Assert
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateClient_returnsUpdated() throws Exception {
        // Arrange
        UpdateClientRequest req = new UpdateClientRequest("C1", "c1@ex.com");
        ClientResponse res = new ClientResponse("1", "C1", "c1@ex.com", 0, 0, 0.0);
        when(clientService.updateClient(eq("1"), any(UpdateClientRequest.class))).thenReturn(res);

        // Act & Assert
        mockMvc.perform(put("/api/clients/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("1")));
    }

    @Test
    void testDeleteClient_returnsNoContent() throws Exception {
        // Arrange
        doNothing().when(clientService).deleteClient("1");

        // Act & Assert
        mockMvc.perform(delete("/api/clients/1"))
                .andExpect(status().isNoContent());
    }
}
