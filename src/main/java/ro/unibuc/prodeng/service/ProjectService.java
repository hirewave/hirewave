
package ro.unibuc.prodeng.service;
import java.util.Optional;

import org.springframework.stereotype.Service;
import ro.unibuc.prodeng.model.Project;
import ro.unibuc.prodeng.repository.ProjectRepository;
import ro.unibuc.prodeng.response.ProjectDescriptionResponse;
import ro.unibuc.prodeng.request.CreateProjectRequest;
import ro.unibuc.prodeng.request.RateFreelancerRequest;
import ro.unibuc.prodeng.repository.ClientRepository; //Needing these 2 for FK id validations
import ro.unibuc.prodeng.repository.FreelancerRepository;//

@Service  
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final FreelancerRepository freelancerRepository;
   
    public ProjectService(ProjectRepository projectRepository, ClientRepository clientRepository, FreelancerRepository freelancerRepository) {
        this.projectRepository = projectRepository;
        this.freelancerRepository= freelancerRepository;
        this.clientRepository= clientRepository;
    }

    public Project CreateProject(ro.unibuc.prodeng.request.CreateProjectRequest request )
    {
        ro.unibuc.prodeng.model.Project p = new Project();
        try{
            if(freelancerRepository.findById(request.getFreelancerId()).isEmpty())
        {    
            throw new IllegalArgumentException("FreelancerId not found!");
        };
        if(clientRepository.findById(request.getClientId()).isEmpty())
        {
                throw new IllegalArgumentException("ClientId not found!");
        };
        p.setBudget(request.getProjectBudget());
        p.setDescription(request.getProjectDescription());
        p.setTitle(request.getProjectName());
        p.setStatus(request.getProjectStatus());
        p.setFreelancerId(request.getFreelancerId());
        p.setClientId(request.getClientId());
        p.setRequiredSkills(request.getProjectSkills());
        return projectRepository.save(p);}
        catch(Exception e){
            System.out.println("Something went wrong!(ProjectService/CreateProject");
            System.out.println(e);
            
        };
        return null; //TODO: Bad practice, probably.
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
    /////

    public ProjectDescriptionResponse getProjectDescription(String projectId) {

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        return new ProjectDescriptionResponse(
            project.getProjectId(),
            project.getTitle(),
            project.getDescription()
        );
    }
}