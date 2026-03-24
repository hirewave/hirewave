package ro.unibuc.prodeng.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.prodeng.model.Project;
import ro.unibuc.prodeng.request.CreateProjectRequest;
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

    @Test
    void testGetAllProjects_returnsList() throws Exception {
        // Arrange
        Project p1 = new Project();
        p1.setTitle("Project A");
        when(projectService.getProjects()).thenReturn(List.of(p1));

        // Act & Assert
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Project A")));
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
    void testCancelProject_alreadyCancelled_returns400() throws Exception {
        // Arrange
        when(projectService.cancelProject("1"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project is already cancelled"));

        // Act & Assert
        mockMvc.perform(patch("/api/projects/1/cancel"))
                .andExpect(status().isBadRequest());
    }

@Test
void testGetSkillStatistics_returnsMap() throws Exception {
    // Arrange
    Map<String, Double> stats = new HashMap<>();
    stats.put("Java", 1500.0);
    stats.put("Docker", 300.0);
    when(projectService.getListedSkills()).thenReturn(stats);

    // Act & Assert
    mockMvc.perform(get("/api/projects/skills/statistics")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.Java", is(1500.0)))
            .andExpect(jsonPath("$.Docker", is(300.0)));
}
}