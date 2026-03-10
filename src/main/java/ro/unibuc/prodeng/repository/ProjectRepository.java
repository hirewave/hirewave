package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ro.unibuc.prodeng.model.Project;

public interface ProjectRepository extends MongoRepository<Project, String> {
}