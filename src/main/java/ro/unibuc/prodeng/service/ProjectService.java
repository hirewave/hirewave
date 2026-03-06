package ro.unibuc.prodeng.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.ClientEntity;
import ro.unibuc.prodeng.model.FreelancerEntity;
import ro.unibuc.prodeng.model.ProjectEntity;
import ro.unibuc.prodeng.model.ProjectStatus;
import ro.unibuc.prodeng.repository.ProjectRepository;
import ro.unibuc.prodeng.request.CreateProjectRequest;
import ro.unibuc.prodeng.request.RateFreelancerRequest;
import ro.unibuc.prodeng.request.UpdateProjectRequest;
import ro.unibuc.prodeng.response.ProjectResponse;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private FreelancerService freelancerService;

    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse getProjectById(String id) {
        return toResponse(getEntityById(id));
    }

    public ProjectEntity getEntityById(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project " + id));
    }

    public List<ProjectResponse> getProjectsByStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse createProject(CreateProjectRequest request) {
        // Validate client exists
        clientService.getEntityById(request.clientId());
        ProjectEntity entity = new ProjectEntity(
                null, request.title(), request.description(),
                request.requiredSkills(), request.budget(),
                ProjectStatus.OPEN, request.clientId(), null
        );
        return toResponse(projectRepository.save(entity));
    }

    public ProjectResponse updateProject(String id, UpdateProjectRequest request) {
        ProjectEntity existing = getEntityById(id);
        if (existing.status() != ProjectStatus.OPEN) {
            throw new IllegalArgumentException("Can only edit projects with OPEN status");
        }
        ProjectEntity updated = new ProjectEntity(
                id, request.title(), request.description(),
                request.requiredSkills(), request.budget(),
                existing.status(), existing.clientId(), existing.awardedFreelancerId()
        );
        return toResponse(projectRepository.save(updated));
    }

    public ProjectResponse cancelProject(String id) {
        ProjectEntity existing = getEntityById(id);
        if (existing.status() != ProjectStatus.OPEN) {
            throw new IllegalArgumentException("Can only cancel projects with OPEN status");
        }
        ProjectEntity updated = new ProjectEntity(
                id, existing.title(), existing.description(),
                existing.requiredSkills(), existing.budget(),
                ProjectStatus.CANCELLED, existing.clientId(), existing.awardedFreelancerId()
        );
        ProjectEntity saved = projectRepository.save(updated);
        clientService.incrementCancelled(existing.clientId());
        return toResponse(saved);
    }

    public ProjectResponse completeProject(String id) {
        ProjectEntity existing = getEntityById(id);
        if (existing.status() != ProjectStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Can only complete projects with IN_PROGRESS status");
        }
        ProjectEntity updated = new ProjectEntity(
                id, existing.title(), existing.description(),
                existing.requiredSkills(), existing.budget(),
                ProjectStatus.COMPLETED, existing.clientId(), existing.awardedFreelancerId()
        );
        ProjectEntity saved = projectRepository.save(updated);
        clientService.incrementCompleted(existing.clientId());
        return toResponse(saved);
    }

    public ProjectResponse startProject(String id, String freelancerId) {
        ProjectEntity existing = getEntityById(id);
        if (existing.status() != ProjectStatus.OPEN) {
            throw new IllegalArgumentException("Can only start projects with OPEN status");
        }
        ProjectEntity updated = new ProjectEntity(
                id, existing.title(), existing.description(),
                existing.requiredSkills(), existing.budget(),
                ProjectStatus.IN_PROGRESS, existing.clientId(), freelancerId
        );
        return toResponse(projectRepository.save(updated));
    }

    public void rateFreelancer(String projectId, RateFreelancerRequest request) {
        ProjectEntity project = getEntityById(projectId);
        if (project.status() != ProjectStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only rate freelancers on completed projects");
        }
        if (project.awardedFreelancerId() == null) {
            throw new IllegalArgumentException("No freelancer awarded for this project");
        }
        freelancerService.addRating(project.awardedFreelancerId(), request.rating());
    }

    public int countActiveProjectsForFreelancer(String freelancerId) {
        return projectRepository.findByAwardedFreelancerIdAndStatus(freelancerId, ProjectStatus.IN_PROGRESS).size();
    }

    private ProjectResponse toResponse(ProjectEntity entity) {
        ClientEntity client = clientService.getEntityById(entity.clientId());
        String freelancerName = null;
        if (entity.awardedFreelancerId() != null) {
            FreelancerEntity freelancer = freelancerService.getEntityById(entity.awardedFreelancerId());
            freelancerName = freelancer.name();
        }
        return new ProjectResponse(
                entity.id(), entity.title(), entity.description(),
                entity.requiredSkills(), entity.budget(),
                entity.status().name(), entity.clientId(), client.name(),
                entity.awardedFreelancerId(), freelancerName
        );
    }
}
