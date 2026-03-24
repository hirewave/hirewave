package ro.unibuc.prodeng.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ro.unibuc.prodeng.model.Project;
import ro.unibuc.prodeng.response.ProjectDescriptionResponse;
import ro.unibuc.prodeng.service.ProjectService;
import ro.unibuc.prodeng.request.CreateProjectRequest;
import java.util.Map;
import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("")
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(projectService.getProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable String id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @GetMapping("/by-client/{clientId}")
    public ResponseEntity<List<Project>> getByClient(@PathVariable String clientId) {
        return ResponseEntity.ok(projectService.getByClientId(clientId));
    }

    @PostMapping("")
    public ResponseEntity<Project> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(201).body(projectService.CreateProject(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable String id, @Valid @RequestBody CreateProjectRequest request) {
        System.out.println("(projectController)Hello from update project!");
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Project> completeProject(@PathVariable String id) {
        return ResponseEntity.ok(projectService.completeProject(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Project> cancelProject(@PathVariable String id) {
        return ResponseEntity.ok(projectService.cancelProject(id));
    }
    @GetMapping("/skills/statistics")
    public ResponseEntity<Map<String, Double>> getListedSkills() {
        return ResponseEntity.ok(projectService.getListedSkills());
    }
    @GetMapping("/{id}/description")
    public ResponseEntity<ProjectDescriptionResponse> getDescription(@PathVariable String id) {
        return ResponseEntity.ok(projectService.getProjectDescription(id));
    }
}