package ro.unibuc.prodeng.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.prodeng.model.Project;
import ro.unibuc.prodeng.model.ProjectStatus;
import ro.unibuc.prodeng.request.CreateProjectRequest;
import ro.unibuc.prodeng.response.ProjectDescriptionResponse;
import ro.unibuc.prodeng.service.ProjectService;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectController projectController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectController).build();
    }

    // -------------------------------------------------------------------------
    // GET /api/projects
    // -------------------------------------------------------------------------

    @Test
    void testGetAllProjects_returnsList() throws Exception {
        // Arrange
        Project p = new Project();
        p.setTitle("Project A");
        when(projectService.getProjects()).thenReturn(List.of(p));

        // Act & Assert
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Project A")));
    }

    // -------------------------------------------------------------------------
    // GET /api/projects/{id}
    // -------------------------------------------------------------------------

    @Test
    void testGetProjectById_existing_returnsProject() throws Exception {
        // Arrange
        Project p = new Project();
        p.setTitle("My Project");
        when(projectService.getProjectById("1")).thenReturn(p);

        // Act & Assert
        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("My Project")));
    }

    @Test
    void testGetProjectById_notFound_returns404() throws Exception {
        // Arrange
        when(projectService.getProjectById("999"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Project with id not found"));

        // Act & Assert
        mockMvc.perform(get("/api/projects/999"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // GET /api/projects/by-client/{clientId}
    // -------------------------------------------------------------------------

    @Test
    void testGetByClient_returnsList() throws Exception {
        // Arrange
        Project p = new Project();
        p.setTitle("Client Project");
        p.setClientId("client-1");
        when(projectService.getByClientId("client-1")).thenReturn(List.of(p));

        // Act & Assert
        mockMvc.perform(get("/api/projects/by-client/client-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Client Project")));
    }

    // -------------------------------------------------------------------------
    // POST /api/projects
    // -------------------------------------------------------------------------

    @Test
    void testCreateProject_valid_returnsCreated() throws Exception {
        // Arrange
        CreateProjectRequest req = new CreateProjectRequest();
        req.setTitle("New Project");
        req.setClientId("client-1");
        req.setBudget(1000.0);
        req.setRequiredSkills(List.of("Java"));

        Project saved = new Project();
        saved.setTitle("New Project");
        when(projectService.CreateProject(any(CreateProjectRequest.class))).thenReturn(saved);

        // Act & Assert
        mockMvc.perform(post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("New Project")));
    }

    // -------------------------------------------------------------------------
    // PUT /api/projects/{id}
    // -------------------------------------------------------------------------

    @Test
    void testUpdateProject_valid_returnsUpdated() throws Exception {
        // Arrange
        CreateProjectRequest req = new CreateProjectRequest();
        req.setTitle("Updated Title");
        req.setClientId("client-1");
        req.setBudget(2000.0);

        Project updated = new Project();
        updated.setTitle("Updated Title");
        when(projectService.updateProject(eq("1"), any(CreateProjectRequest.class))).thenReturn(updated);

        // Act & Assert
        mockMvc.perform(put("/api/projects/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")));
    }

    @Test
    void testUpdateProject_notFound_returns404() throws Exception {
        // Arrange
        CreateProjectRequest req = new CreateProjectRequest();
        req.setTitle("Title");
        req.setClientId("client-1");
        req.setBudget(100.0);

        when(projectService.updateProject(eq("999"), any(CreateProjectRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Project with id not found"));

        // Act & Assert
        mockMvc.perform(put("/api/projects/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/projects/{id}/complete
    // -------------------------------------------------------------------------

    @Test
    void testCompleteProject_returnsUpdatedProject() throws Exception {
        // Arrange
        Project p = new Project();
        p.setTitle("Done Project");
        p.setStatus("COMPLETED");
        when(projectService.completeProject("1")).thenReturn(p);

        // Act & Assert
        mockMvc.perform(patch("/api/projects/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void testCompleteProject_notFound_returns404() throws Exception {
        // Arrange
        when(projectService.completeProject("999"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Project with id not found"));

        // Act & Assert
        mockMvc.perform(patch("/api/projects/999/complete"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/projects/{id}/cancel
    // -------------------------------------------------------------------------

    @Test
    void testCancelProject_valid_returnsCancelled() throws Exception {
        // Arrange
        Project p = new Project();
        p.setTitle("Cancelled Project");
        p.setStatus("CANCELLED");
        when(projectService.cancelProject("1")).thenReturn(p);

        // Act & Assert
        mockMvc.perform(patch("/api/projects/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void testCancelProject_alreadyCancelled_returns400() throws Exception {
        // Arrange
        when(projectService.cancelProject("1"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project is already cancelled"));

        // Act & Assert
        mockMvc.perform(patch("/api/projects/1/cancel"))
                .andExpect(status().isBadRequest());
    }
// -------------------------------------------------------------------------
    // GET /api/projects/skills/statistics
    // -------------------------------------------------------------------------
    
    @Test
    void testGetSkillStatistics_returnsMap() throws Exception {
        // Arrange
        when(projectService.getListedSkills()).thenReturn(Map.of("Java", 1500.0, "Docker", 300.0));

        // Act & Assert
        // FIX: Replaced "/api/projects/skill_statistics" with the correct endpoint "/api/projects/skills/statistics"
        mockMvc.perform(get("/api/projects/skills/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Java", is(1500.0)))
                .andExpect(jsonPath("$.Docker", is(300.0)));
    }

    // -------------------------------------------------------------------------
    // GET /api/projects/{id}/description
    // -------------------------------------------------------------------------

    @Test
    void testGetDescription_valid_returnsDescription() throws Exception {
        // Arrange
        ProjectDescriptionResponse resp = new ProjectDescriptionResponse("1", "Title", "Some description");
        when(projectService.getProjectDescription("1")).thenReturn(resp);

        // Act & Assert
        // FIX: The JSON path must match the getter of ProjectDescriptionResponse ("projectName" instead of "title")
        mockMvc.perform(get("/api/projects/1/description"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectName", is("Title"))) 
                .andExpect(jsonPath("$.description", is("Some description")));
    }

}