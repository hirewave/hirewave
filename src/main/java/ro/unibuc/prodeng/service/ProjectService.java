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
import ro.unibuc.prodeng.request.CreateProjectRequest;
import ro.unibuc.prodeng.request.RateFreelancerRequest;
import ro.unibuc.prodeng.repository.ClientRepository;
import ro.unibuc.prodeng.repository.FreelancerRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final FreelancerRepository freelancerRepository;

    public ProjectService(ProjectRepository projectRepository, ClientRepository clientRepository, FreelancerRepository freelancerRepository) {
        this.projectRepository = projectRepository;
        this.freelancerRepository = freelancerRepository;
        this.clientRepository = clientRepository;
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
        return projectRepository.findAll().stream()
            .filter(p -> clientId.equals(p.getClientId()))
            .collect(Collectors.toList());
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
        return projectIds.stream()
            .map(id -> projectRepository.findById(id))
            .filter(opt -> opt.isPresent() && opt.get().getStatus() == ProjectStatus.IN_PROGRESS)
            .count();
    }

    ///////TODO
    public void assignProject(String Projectid, String freelancerId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void rateFreelancer(String projectId, RateFreelancerRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public int countActiveProjectsForFreelancer(String freelancerId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    ///////

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