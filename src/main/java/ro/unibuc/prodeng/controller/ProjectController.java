package ro.unibuc.prodeng.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ro.unibuc.prodeng.model.ProjectStatus;
import ro.unibuc.prodeng.request.CreateProjectRequest;
import ro.unibuc.prodeng.request.RateFreelancerRequest;
import ro.unibuc.prodeng.request.UpdateProjectRequest;
import ro.unibuc.prodeng.response.ProjectResponse;
import ro.unibuc.prodeng.service.ProjectService;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects(
            @RequestParam(required = false) String status) {
        if (status != null) {
            ProjectStatus ps = ProjectStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(projectService.getProjectsByStatus(ps));
        }
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable String id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable String id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ProjectResponse> cancelProject(@PathVariable String id) {
        return ResponseEntity.ok(projectService.cancelProject(id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ProjectResponse> completeProject(@PathVariable String id) {
        return ResponseEntity.ok(projectService.completeProject(id));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<Void> rateFreelancer(
            @PathVariable String id,
            @Valid @RequestBody RateFreelancerRequest request) {
        projectService.rateFreelancer(id, request);
        return ResponseEntity.ok().build();
    }
}
