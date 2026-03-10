package ro.unibuc.prodeng.request;
import java.util.List;
public class CreateProjectRequest {
    private String projectName;
    private String projectStatus;
    private String projectDescription;
    private Double projectBudget;
    private String clientId;
    private String FreelancerId;
    private List<String> projectSkills;

/*
    private String id;
    private String awardedFreelancerId;
    private String clientId;
    private String title;
    private String description;
    private List<String> requiredSkills;
    private Double Budget;
    private String status;


*/



    public String getProjectName(){return projectName;}
    public void setProjectName(String name){this.projectName=name;}
    
    public String getProjectStatus(){return projectStatus;}
    public void setProjectStatus(String status){this.projectStatus=status;}  

    public String getProjectDescription(){return projectDescription;}
    public void setProjectDescription(String description){this.projectDescription=description;}

    public Double getProjectBudget(){return projectBudget;}
    public void setProjectBudget(Double budget){this.projectBudget=budget;}

    public String getClientId(){return clientId;}
    public void setClientId(String id){this.clientId=id;}

    public String getFreelancerId(){return FreelancerId;}
    public void setFreelancerId(String id){this.FreelancerId=id;}

    public List<String> getProjectSkills(){return this.projectSkills;}
    public void setProjectSkills(List<String> Skills) {this.projectSkills=Skills;} 
}
