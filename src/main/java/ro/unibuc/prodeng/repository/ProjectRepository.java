package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ro.unibuc.prodeng.model.Project;
import ro.unibuc.prodeng.model.ProjectStatus;
import java.util.List;
public interface ProjectRepository extends MongoRepository<Project, String> {
        long countByAwardedFreelancerIdAndStatus(String freelancerId, ProjectStatus status);
        List<Project> findByClientId(String clientId);
}