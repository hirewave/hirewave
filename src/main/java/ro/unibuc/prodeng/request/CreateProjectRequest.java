package ro.unibuc.prodeng.request;
import java.util.List;
public class CreateProjectRequest {
    private String title;
    private String projectStatus;
    private String description;
    private Double budget;
    private String clientId;
    private String FreelancerId;
    private List<String> requiredSkills;



    public String geTitle(){return title;}
    public void setTitle(String name){this.title=name;}
    
    public String getProjectStatus(){return projectStatus;}
    public void setProjectStatus(String status){this.projectStatus=status;}  

    public String getDescription(){return description;}
    public void setDescription(String description){this.description=description;}

    public Double getBudget(){return budget;}
    public void setBudget(Double budget){this.budget=budget;}

    public String getClientId(){return clientId;}
    public void setClientId(String id){this.clientId=id;}

    public String getFreelancerId(){return FreelancerId;}
    public void setFreelancerId(String id){this.FreelancerId=id;}

    public List<String> getRequiredSkills(){return this.requiredSkills;}
    public void setRequiredSkills(List<String> Skills) {this.requiredSkills=Skills;} 
}
