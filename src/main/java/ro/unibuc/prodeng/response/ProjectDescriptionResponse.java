package ro.unibuc.prodeng.response;

public class ProjectDescriptionResponse {
    private String projectId;
    private String projectName;
    private String description;

    public ProjectDescriptionResponse(String projectId, String projectName, String description) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.description = description;
    }
    // Getters
    public String getProjectId() { return projectId; }
    public String getProjectName() { return projectName; }
    public String getDescription() { return description; }
}