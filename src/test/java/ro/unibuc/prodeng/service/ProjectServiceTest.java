package ro.unibuc.prodeng.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import ro.unibuc.prodeng.model.Project;
import ro.unibuc.prodeng.model.ProjectStatus;
import ro.unibuc.prodeng.repository.ClientRepository;
import ro.unibuc.prodeng.repository.FreelancerRepository;
import ro.unibuc.prodeng.repository.ProjectRepository;
import ro.unibuc.prodeng.request.CreateProjectRequest;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private FreelancerRepository freelancerRepository;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ProjectService projectService;

    // --- getProjectById ---

    @Test
    void testGetProjectById_existing_returnsProject() {
        // Arrange
        Project p = new Project();
        p.setTitle("Test Project");
        when(projectRepository.findById("1")).thenReturn(Optional.of(p));

        // Act
        Project result = projectService.getProjectById("1");

        // Assert
        assertEquals("Test Project", result.getTitle());
    }

    @Test
    void testGetProjectById_notFound_throwsException() {
        // Arrange
        when(projectRepository.findById("1")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> projectService.getProjectById("1"));
    }

    // --- cancelProject ---

    @Test
    void testCancelProject_alreadyCancelled_throwsException() {
        // Arrange
        Project p = new Project();
        p.setStatus("CANCELLED");
        when(projectRepository.findById("1")).thenReturn(Optional.of(p));

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> projectService.cancelProject("1"));
    }

    @Test
    void testCancelProject_valid_savesAndIncrementsClient() {
        // Arrange
        Project p = new Project();
        p.setStatus("OPEN");
        p.setClientId("client-1");
        when(projectRepository.findById("1")).thenReturn(Optional.of(p));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Project result = projectService.cancelProject("1");

        // Assert
        assertEquals(ProjectStatus.CANCELLED, result.getStatus());
        verify(clientService).incrementCancelled("client-1");
    }

    // --- getListedSkills ---

    @Test
    void testGetListedSkills_onlyCountsCompletedProjects() {
        // Arrange
        Project completed = new Project();
        completed.setStatus("COMPLETED");
        completed.setRequiredSkills(Arrays.asList("Java", "Python"));
        completed.setBudget(1000.0);

        Project open = new Project();
        // status stays OPEN (set in constructor)
        open.setRequiredSkills(Arrays.asList("Java"));
        open.setBudget(500.0);

        when(projectRepository.findAll()).thenReturn(Arrays.asList(completed, open));

        // Act
        Map<String, Double> result = projectService.getListedSkills();

        // Assert — open project's Java budget must NOT be counted
        assertEquals(1000.0, result.get("Java"));
        assertEquals(1000.0, result.get("Python"));
        assertEquals(2, result.size());
    }

    @Test
    void testGetListedSkills_accumulatesBudgetAcrossProjects() {
        // Arrange
        Project p1 = new Project();
        p1.setStatus("COMPLETED");
        p1.setRequiredSkills(Arrays.asList("Java"));
        p1.setBudget(500.0);

        Project p2 = new Project();
        p2.setStatus("COMPLETED");
        p2.setRequiredSkills(Arrays.asList("Java", "Docker"));
        p2.setBudget(300.0);

        when(projectRepository.findAll()).thenReturn(Arrays.asList(p1, p2));

        // Act
        Map<String, Double> result = projectService.getListedSkills();

        // Assert
        assertEquals(800.0, result.get("Java"));
        assertEquals(300.0, result.get("Docker"));
    }

    @Test
    void testGetListedSkills_nullSkills_doesNotThrow() {
        // Arrange
        Project p = new Project();
        p.setStatus("COMPLETED");
        // requiredSkills intentionally left null
        p.setBudget(1000.0);

        when(projectRepository.findAll()).thenReturn(List.of(p));

        // Act & Assert
        assertDoesNotThrow(() -> projectService.getListedSkills());
        assertTrue(projectService.getListedSkills().isEmpty());
    }
}