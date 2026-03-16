package ro.unibuc.prodeng.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.unibuc.prodeng.model.Project;
import ro.unibuc.prodeng.model.ProjectEntity;
import ro.unibuc.prodeng.model.ProjectStatus;
import ro.unibuc.prodeng.repository.ProjectRepository;
import ro.unibuc.prodeng.response.ProjectDescriptionResponse;
import ro.unibuc.prodeng.response.ProjectResponse;
import ro.unibuc.prodeng.request.CreateProjectRequest;
import ro.unibuc.prodeng.request.RateFreelancerRequest;
import ro.unibuc.prodeng.repository.ClientRepository;
import ro.unibuc.prodeng.repository.FreelancerRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final FreelancerRepository freelancerRepository;
    private final ClientService clientService;
    private final FreelancerService freelancerService;
    public ProjectService(ProjectRepository projectRepository, ClientRepository clientRepository, FreelancerRepository freelancerRepository, ClientService clientService, FreelancerService freelancerService) {
        this.projectRepository = projectRepository;
        this.freelancerRepository = freelancerRepository;
        this.clientRepository = clientRepository;
        this.clientService = clientService;
        this.freelancerService = freelancerService;
    }

    public Project CreateProject(CreateProjectRequest request) {
        if (clientRepository.findById(request.getClientId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ClientId not found!");
        }
        Project p = new Project();
        p.setBudget(request.getBudget());
        p.setDescription(request.getDescription());
        p.setTitle(request.getTitle());
        p.setClientId(request.getClientId());
        p.setRequiredSkills(request.getRequiredSkills());
        return projectRepository.save(p);
    }

    public List<Project> getProjects() {
        return projectRepository.findAll();
    }

    public Project getProjectById(String id) {
        Optional<Project> attempting = projectRepository.findById(id);
        if (attempting.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project with id not found");
        return attempting.get();
    }

    public List<Project> getByClientId(String clientId) {
        return projectRepository.findByClientId(clientId);
    }

    

    public Project updateProject(String id, CreateProjectRequest request) {
        Optional<Project> attempting = projectRepository.findById(id);
        if (attempting.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project with id not found");
        Project p = attempting.get();
        if (request.getTitle() != null)
            p.setTitle(request.getTitle());
        if (request.getDescription() != null)
            p.setDescription(request.getDescription());
        if (request.getBudget() != null)
            p.setBudget(request.getBudget());
        if (request.getRequiredSkills() != null)
            p.setRequiredSkills(request.getRequiredSkills());
        return projectRepository.save(p);
    }

    public Project completeProject(String id) {
        Optional<Project> attempting = projectRepository.findById(id);
        if (attempting.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project with id not found");
        Project p = attempting.get();
        p.setStatus("COMPLETED");
        if(clientService != null)
            clientService.incrementCompleted(p.getClientId());
        return projectRepository.save(p);
    }

    public Project cancelProject(String id) {
        Optional<Project> attempting = projectRepository.findById(id);
        if (attempting.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project with id not found");
        Project p = attempting.get();
        if (p.getStatus() != null && p.getStatus().name().equals("CANCELLED"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project is already cancelled");
        p.setStatus("CANCELLED");
        if (p.getClientId() != null)
            clientService.incrementCancelled(p.getClientId());
        return projectRepository.save(p);
    }

    // Called by BidService when a bid is accepted
    public void markInProgress(String projectId, String freelancerId) {
        Optional<Project> attempting = projectRepository.findById(projectId);
        if (attempting.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project with id not found");
        Project p = attempting.get();
        p.setStatus("IN_PROGRESS");
        p.setFreelancerId(freelancerId);
        projectRepository.save(p);
    }

    // Called by BidService — converts Project to the ProjectEntity record it expects
    public ProjectEntity getEntityById(String id) {
        Optional<Project> attempting = projectRepository.findById(id);
        if (attempting.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project with id not found");
        Project p = attempting.get();
        ProjectStatus status = p.getStatus() == null ? ProjectStatus.OPEN
            : ProjectStatus.valueOf(p.getStatus().name());
        return new ProjectEntity(p.getId(), p.getTitle(), p.getDescription(),
            p.getClientId(), p.getRequiredSkills(), p.getBudget(), status, p.getAwardedFreelancerId());
    }

    // Called by BidService to check how many of a freelancer's accepted projects are IN_PROGRESS
    public long countInProgressByIds(Collection<String> projectIds) {
        long count = 0L;
        for (Project project : projectRepository.findAllById(projectIds)) {
            if (project.getStatus() == ProjectStatus.IN_PROGRESS) {
                count++;
            }
        }
        return count;
    }

    public void assignProject(String Projectid, String freelancerId) {
        Project existing = getProjectById(Projectid);
        if (existing.getStatus() != ProjectStatus.OPEN) {
            throw new IllegalArgumentException("Can only start projects with OPEN status");
        }
        existing.setFreelancerId(freelancerId);
        existing.setStatus("IN_PROGRESS");
        projectRepository.save(existing);
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

    public long countActiveProjectsForFreelancer(String freelancerId) {
        return projectRepository.countByAwardedFreelancerIdAndStatus(freelancerId, ProjectStatus.IN_PROGRESS);
    }

    public ProjectDescriptionResponse getProjectDescription(String projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
        return new ProjectDescriptionResponse(
            project.getId(),
            project.getTitle(),
            project.getDescription()
        );
    }
}