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
import ro.unibuc.prodeng.repository.ProjectRepository;
import ro.unibuc.prodeng.request.CreateProjectRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ClientRepository clientRepository; // Needed because Project needs a valid ClientId

    @AfterEach
    void cleanup() {
        projectRepository.deleteAll();
        clientRepository.deleteAll();
    }

    @Test
    void testCreateProject_returnsCreated() throws Exception {
        // Arrange - Create a client first to satisfy the foreign key requirement
        ClientEntity client = clientRepository.save(new ClientEntity(null, "Test Client", "test@example.com", 0, 0));
        
        CreateProjectRequest req = new CreateProjectRequest();
        req.setTitle("Integration Test Project");
        req.setDescription("Simple description");
        req.setClientId(client.id());
        req.setBudget(1000.0);
        req.setRequiredSkills(List.of("Java"));

        // Act & Assert
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Integration Test Project")));
        
        assertThat(projectRepository.findAll()).hasSize(1);
    }
}