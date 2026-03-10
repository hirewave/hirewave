package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.ProjectEntity;
import ro.unibuc.prodeng.model.ProjectStatus;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProjectRepository extends MongoRepository<ProjectEntity, String> {
    List<ProjectEntity> findByClientId(String clientId);
    long countByStatusAndIdIn(ProjectStatus status, Collection<String> ids);
}
