package ro.unibuc.prodeng.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

import ro.unibuc.prodeng.model.ClientEntity;
import ro.unibuc.prodeng.model.Project;
import ro.unibuc.prodeng.model.ProjectEntity;
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

    // -------------------------------------------------------------------------
    // CreateProject
    // -------------------------------------------------------------------------

    @Test
    void testCreateProject_validClient_savesAndReturnsProject() {
        // Arrange
        CreateProjectRequest req = mock(CreateProjectRequest.class);
        when(req.getClientId()).thenReturn("client-1");
        when(req.getTitle()).thenReturn("New Project");
        when(req.getDescription()).thenReturn("Desc");
        when(req.getBudget()).thenReturn(500.0);
        when(req.getRequiredSkills()).thenReturn(List.of("Java"));
        when(clientRepository.findById("client-1"))
                .thenReturn(Optional.of(mock(ClientEntity.class)));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Project result = projectService.CreateProject(req);

        // Assert
        assertEquals("New Project", result.getTitle());
        assertEquals(500.0, result.getBudget());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void testCreateProject_clientNotFound_throwsException() {
        // Arrange
        CreateProjectRequest req = mock(CreateProjectRequest.class);
        when(req.getClientId()).thenReturn("missing");
        when(clientRepository.findById("missing")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> projectService.CreateProject(req));
        verify(projectRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getProjects
    // -------------------------------------------------------------------------

    @Test
    void testGetProjects_returnsAll() {
        // Arrange
        Project p1 = new Project();
        p1.setTitle("A");
        Project p2 = new Project();
        p2.setTitle("B");
        when(projectRepository.findAll()).thenReturn(List.of(p1, p2));

        // Act
        List<Project> result = projectService.getProjects();

        // Assert
        assertEquals(2, result.size());
    }

    // -------------------------------------------------------------------------
    // getProjectById
    // -------------------------------------------------------------------------

    @Test
    void testGetProjectById_existing_returnsProject() {
        // Arrange
        Project p = new Project();
        p.setTitle("Test Project");
        when(projectRepository.findById("1")).thenReturn(Optional.of(p));

        // Act & Assert
        assertEquals("Test Project", projectService.getProjectById("1").getTitle());
    }

    @Test
    void testGetProjectById_notFound_throwsException() {
        when(projectRepository.findById("1")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> projectService.getProjectById("1"));
    }

    // -------------------------------------------------------------------------
    // getByClientId
    // -------------------------------------------------------------------------

    @Test
    void testGetByClientId_returnsMatchingProjects() {
        // Arrange
        Project p = new Project();
        p.setClientId("client-1");
        when(projectRepository.findByClientId("client-1")).thenReturn(List.of(p));

        // Act
        List<Project> result = projectService.getByClientId("client-1");

        // Assert
        assertEquals(1, result.size());
        assertEquals("client-1", result.get(0).getClientId());
    }

    // -------------------------------------------------------------------------
    // updateProject
    // -------------------------------------------------------------------------

    @Test
    void testUpdateProject_existing_updatesOnlyProvidedFields() {
        // Arrange
        Project existing = new Project();
        existing.setTitle("Old Title");
        existing.setDescription("Old Desc");
        when(projectRepository.findById("1")).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateProjectRequest req = mock(CreateProjectRequest.class);
        when(req.getTitle()).thenReturn("New Title");
        when(req.getDescription()).thenReturn(null);
        when(req.getBudget()).thenReturn(null);
        when(req.getRequiredSkills()).thenReturn(null);

        // Act
        Project result = projectService.updateProject("1", req);

        // Assert — title updated, description left alone
        assertEquals("New Title", result.getTitle());
        assertEquals("Old Desc", result.getDescription());
    }

    @Test
    void testUpdateProject_notFound_throwsException() {
        when(projectRepository.findById("1")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,
                () -> projectService.updateProject("1", mock(CreateProjectRequest.class)));
    }

    // -------------------------------------------------------------------------
    // completeProject
    // -------------------------------------------------------------------------

    @Test
    void testCompleteProject_valid_setsStatusAndIncrementsClient() {
        // Arrange
        Project p = new Project();
        p.setClientId("client-1");
        when(projectRepository.findById("1")).thenReturn(Optional.of(p));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Project result = projectService.completeProject("1");

        // Assert
        assertEquals(ProjectStatus.COMPLETED, result.getStatus());
        verify(clientService).incrementCompleted("client-1");
    }

    @Test
    void testCompleteProject_notFound_throwsException() {
        when(projectRepository.findById("1")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> projectService.completeProject("1"));
    }

    // -------------------------------------------------------------------------
    // cancelProject
    // -------------------------------------------------------------------------

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
    void testCancelProject_notFound_throwsException() {
        when(projectRepository.findById("1")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> projectService.cancelProject("1"));
    }

    // -------------------------------------------------------------------------
    // markInProgress
    // -------------------------------------------------------------------------

    @Test
    void testMarkInProgress_valid_setsStatusAndFreelancer() {
        // Arrange
        Project p = new Project();
        when(projectRepository.findById("1")).thenReturn(Optional.of(p));

        // Act
        projectService.markInProgress("1", "freelancer-1");

        // Assert
        verify(projectRepository).save(argThat(saved ->
                saved.getStatus() == ProjectStatus.IN_PROGRESS &&
                "freelancer-1".equals(saved.getAwardedFreelancerId())
        ));
    }

    @Test
    void testMarkInProgress_notFound_throwsException() {
        when(projectRepository.findById("1")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,
                () -> projectService.markInProgress("1", "freelancer-1"));
    }

    // -------------------------------------------------------------------------
    // getEntityById
    // -------------------------------------------------------------------------

    @Test
    void testGetEntityById_valid_returnsProjectEntity() {
        // Arrange
        Project p = new Project();
        p.setTitle("My Project");
        p.setStatus("IN_PROGRESS");
        p.setClientId("client-1");
        p.setRequiredSkills(List.of("Java"));
        p.setBudget(1000.0);
        when(projectRepository.findById("1")).thenReturn(Optional.of(p));

        // Act
        ProjectEntity entity = projectService.getEntityById("1");

        // Assert
        assertEquals("My Project", entity.title());
        assertEquals(ProjectStatus.IN_PROGRESS, entity.status());
    }

    @Test
    void testGetEntityById_nullStatus_defaultsToOpen() {
        // Arrange — spy to force getStatus() to return null
        Project p = spy(new Project());
        doReturn(null).when(p).getStatus();
        when(projectRepository.findById("1")).thenReturn(Optional.of(p));

        // Act
        ProjectEntity entity = projectService.getEntityById("1");

        // Assert
        assertEquals(ProjectStatus.OPEN, entity.status());
    }

    @Test
    void testGetEntityById_notFound_throwsException() {
        when(projectRepository.findById("1")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> projectService.getEntityById("1"));
    }

    // -------------------------------------------------------------------------
    // countInProgressByIds
    // -------------------------------------------------------------------------

    @Test
    void testCountInProgressByIds_countsOnlyInProgress() {
        // Arrange
        Project inProgress = new Project();
        inProgress.setStatus("IN_PROGRESS");

        Project completed = new Project();
        completed.setStatus("COMPLETED");

        Project cancelled = new Project();
        cancelled.setStatus("CANCELLED");
        List<String> ids = List.of("p1","p2","p3");
        List<Project> pros = List.of(inProgress,completed,cancelled);
        when(projectRepository.findAllById(ids)).thenReturn(pros);
        /*when(projectRepository.findById("p1")).thenReturn(Optional.of(inProgress));
        when(projectRepository.findById("p2")).thenReturn(Optional.of(completed));
        when(projectRepository.findById("p3")).thenReturn(Optional.empty());
        */
        // Act
        long count = projectService.countInProgressByIds(List.of("p1", "p2", "p3"));

        // Assert
        assertEquals(1, count);
    }



    @Test
    void testGetProjectDescription_notFound_throwsException() {
        when(projectRepository.findById("1")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> projectService.getProjectDescription("1"));
    }

    // -------------------------------------------------------------------------
    // getListedSkills
    // -------------------------------------------------------------------------

    @Test
    void testGetListedSkills_onlyCountsCompletedProjects() {
        // Arrange
        Project completed = new Project();
        completed.setStatus("COMPLETED");
        completed.setRequiredSkills(Arrays.asList("Java", "Python"));
        completed.setBudget(1000.0);

        Project open = new Project();
        // stays OPEN from constructor
        open.setRequiredSkills(Arrays.asList("Java"));
        open.setBudget(500.0);

        when(projectRepository.findAll()).thenReturn(Arrays.asList(completed, open));

        // Act
        Map<String, Double> result = projectService.getListedSkills();

        // Assert
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
        p.setBudget(1000.0);
        // requiredSkills intentionally left null

        when(projectRepository.findAll()).thenReturn(List.of(p));

        // Act & Assert
        assertDoesNotThrow(() -> projectService.getListedSkills());
        assertTrue(projectService.getListedSkills().isEmpty());
    }

    // -------------------------------------------------------------------------
    // countActiveProjectsForFreelancer
    // -------------------------------------------------------------------------

    @Test
    void testCountActiveProjectsForFreelancer_returnsExpectedCount() {
        when(projectRepository.countByAwardedFreelancerIdAndStatus("free-1", ProjectStatus.IN_PROGRESS))
            .thenReturn(3L);
        
        long count = projectService.countActiveProjectsForFreelancer("free-1");
        
        assertEquals(3L, count);
    }

    // -------------------------------------------------------------------------
    // getProjectDescription
    // -------------------------------------------------------------------------

    @Test
    void testGetProjectDescription_valid_returnsResponse() {
        Project p = new Project();
        p.setTitle("Test Title");
        p.setDescription("Test Desc");
        when(projectRepository.findById("1")).thenReturn(Optional.of(p));
        
        ro.unibuc.prodeng.response.ProjectDescriptionResponse res = projectService.getProjectDescription("1");
        
        assertEquals("Test Title", res.getProjectName());
        assertEquals("Test Desc", res.getDescription());
    }
}