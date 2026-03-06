package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.BidEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends MongoRepository<BidEntity, String> {
    List<BidEntity> findByProjectId(String projectId);
    List<BidEntity> findByFreelancerId(String freelancerId);
    Optional<BidEntity> findByProjectIdAndFreelancerId(String projectId, String freelancerId);
}
