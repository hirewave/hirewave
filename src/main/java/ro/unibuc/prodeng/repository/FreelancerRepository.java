package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.FreelancerEntity;

import java.util.Optional;

@Repository
public interface FreelancerRepository extends MongoRepository<FreelancerEntity, String> {
    Optional<FreelancerEntity> findByEmail(String email);
}
