package ro.unibuc.prodeng.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ro.unibuc.prodeng.model.Project;
import ro.unibuc.prodeng.response.ProjectDescriptionResponse;
import ro.unibuc.prodeng.service.ProjectService;
import ro.unibuc.prodeng.request.CreateProjectRequest;
@RestController              // this class handles HTTP requests
@RequestMapping("api/projects") // all routes here start with api/projects
public class ProjectController {

    private final ro.unibuc.prodeng.service.ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // GET /projects/{id}/description
    @GetMapping("/{id}/description")
    public ResponseEntity<ProjectDescriptionResponse> getDescription(@PathVariable String id) {
        System.out.println("Hello from Controller(Project, get description");
        return ResponseEntity.ok(projectService.getProjectDescription(id));
    }

    @PostMapping("/create")
    public ResponseEntity<Project> createProject(@RequestBody CreateProjectRequest request)
    {
        Project created=projectService.CreateProject(request);
        System.out.println("Hello from controller!(Project, create project)");
        return ResponseEntity.ok(created);
    }


}



/*
curl -H 'Content-Type: application/json' \
      -d '{ "projectName":"Testing","projectStatus":"START", "projectDescription": "Testam momentan", "projectBudget" : 67}' \
      -X POST \
      https://localhost:8090/projects


    private String projectName;
    private String projectStatus;
    private String projectDescription;
    private Double projectBudget;      

*/