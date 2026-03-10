package ro.unibuc.prodeng.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.ProjectEntity;
import ro.unibuc.prodeng.model.ProjectStatus;
import ro.unibuc.prodeng.repository.BidRepository;
import ro.unibuc.prodeng.repository.ProjectRepository;
import ro.unibuc.prodeng.request.CreateProjectRequest;
import ro.unibuc.prodeng.request.UpdateProjectRequest;
import ro.unibuc.prodeng.response.ProjectResponse;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private BidRepository bidRepository;

    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProjectResponse> getProjectsByClientId(String clientId) {
        clientService.getEntityById(clientId); // validates client exists
        return projectRepository.findByClientId(clientId).stream()
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

    public ProjectResponse createProject(CreateProjectRequest request) {
        clientService.getEntityById(request.clientId()); // validates client exists
        ProjectEntity entity = new ProjectEntity(
                null, request.title(), request.description(),
                request.clientId(), request.requiredSkills(),
                request.budget(), ProjectStatus.OPEN, null
        );
        return toResponse(projectRepository.save(entity));
    }

    public ProjectResponse updateProject(String id, UpdateProjectRequest request) {
        ProjectEntity existing = getEntityById(id);
        if (existing.status() != ProjectStatus.OPEN) {
            throw new IllegalArgumentException(
                    "Project can only be edited when in OPEN status, current status: " + existing.status());
        }
        ProjectEntity updated = new ProjectEntity(
                id, request.title(), request.description(),
                existing.clientId(), request.requiredSkills(),
                request.budget(), existing.status(), existing.awardedFreelancerId()
        );
        return toResponse(projectRepository.save(updated));
    }

    public ProjectResponse cancelProject(String id) {
        ProjectEntity existing = getEntityById(id);
        if (existing.status() != ProjectStatus.OPEN) {
            throw new IllegalArgumentException(
                    "Only OPEN projects can be cancelled, current status: " + existing.status());
        }
        bidRepository.rejectAllPendingBids(id);
        ProjectEntity cancelled = new ProjectEntity(
                id, existing.title(), existing.description(),
                existing.clientId(), existing.requiredSkills(),
                existing.budget(), ProjectStatus.CANCELLED, existing.awardedFreelancerId()
        );
        ProjectResponse response = toResponse(projectRepository.save(cancelled));
        clientService.incrementCancelled(existing.clientId());
        return response;
    }

    public ProjectResponse completeProject(String id) {
        ProjectEntity existing = getEntityById(id);
        if (existing.status() != ProjectStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Only IN_PROGRESS projects can be completed, current status: " + existing.status());
        }
        ProjectEntity completed = new ProjectEntity(
                id, existing.title(), existing.description(),
                existing.clientId(), existing.requiredSkills(),
                existing.budget(), ProjectStatus.COMPLETED, existing.awardedFreelancerId()
        );
        ProjectResponse response = toResponse(projectRepository.save(completed));
        clientService.incrementCompleted(existing.clientId());
        return response;
    }

    // Called by BidService to count active projects for availability check
    public long countInProgressByIds(java.util.Collection<String> projectIds) {
        return projectRepository.countByStatusAndIdIn(ProjectStatus.IN_PROGRESS, projectIds);
    }

    // Called by BidService when a bid is accepted
    public void markInProgress(String id, String awardedFreelancerId) {
        ProjectEntity existing = getEntityById(id);
        if (existing.status() != ProjectStatus.OPEN) {
            throw new IllegalArgumentException(
                    "Project must be OPEN to accept a bid, current status: " + existing.status());
        }
        ProjectEntity updated = new ProjectEntity(
                id, existing.title(), existing.description(),
                existing.clientId(), existing.requiredSkills(),
                existing.budget(), ProjectStatus.IN_PROGRESS, awardedFreelancerId
        );
        projectRepository.save(updated);
    }

    private ProjectResponse toResponse(ProjectEntity entity) {
        return new ProjectResponse(
                entity.id(), entity.title(), entity.description(),
                entity.clientId(), entity.requiredSkills(),
                entity.budget(), entity.status(), entity.awardedFreelancerId()
        );
    }
}
