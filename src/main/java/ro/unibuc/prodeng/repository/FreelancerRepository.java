package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.FreelancerEntity;

import java.util.Optional;

@Repository
public interface FreelancerRepository extends MongoRepository<FreelancerEntity, String> {
    Optional<FreelancerEntity> findByEmail(String email);

    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'ratingSum': ?1, 'totalRatings': 1 } }")
    void incrementRating(String id, int rating);
}
