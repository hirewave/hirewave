package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.ClientEntity;

import java.util.Optional;

@Repository
public interface ClientRepository extends MongoRepository<ClientEntity, String> {
    Optional<ClientEntity> findByEmail(String email);

    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'completedProjects': 1 } }")
    void incrementCompletedProjects(String id);

    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'cancelledProjects': 1 } }")
    void incrementCancelledProjects(String id);
}
