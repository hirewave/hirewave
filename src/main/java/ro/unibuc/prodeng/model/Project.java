
package ro.unibuc.prodeng.model;
//import static org.hamcrest.Matchers.startsWith;
import ro.unibuc.prodeng.model.ProjectStats;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "projects")
public class Project {

    @Id
    private String id;
    private String awardedFreelancerId;
    private String clientId;
    private String title;
    private String description;
    private List<String> requiredSkills;
    private Double Budget;
    private ProjectStats status;
    



    public Project() {this.status=ProjectStats.OPEN;}
    public String getId() { return id; }
    public String getDescription() { return description; }
    public String getTitle() { return title; }
    public Double getBudget() {return Budget;}
    public String getClientId() {return clientId;}
    public ProjectStats getStatus() {return status;}
    public List<String> getRequiredSkills() {return this.requiredSkills;}
    public String getAwardedFreelancerId() {return this.awardedFreelancerId;}

    public void setDescription(String description) { this.description=description; }
    public void setTitle(String name) {  this.title=name; }
    public void setBudget(Double budget) {this.Budget=budget;}
    public void setClientId(String clientId) {this.clientId=clientId;}
    public void setRequiredSkills(List<String> skills) {this.requiredSkills=skills;}
    public void setFreelancerId(String id){this.awardedFreelancerId=id;}
    public void setStatus(String Status){
        switch (Status) {
            case "OPEN":
                this.status=ProjectStats.OPEN;
                break;
            case "IN_PROGRESS":
                this.status=ProjectStats.IN_PROGRESS;
                break;
            case "COMPLETED":
                this.status=ProjectStats.COMPLETED;
                break;
            case "CANCELLED":
                this.status=ProjectStats.CANCELLED;
                break;
            default:
                this.status=ProjectStats.OPEN;
                break;
        }
    }
}