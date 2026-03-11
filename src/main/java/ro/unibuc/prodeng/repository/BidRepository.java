package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.BidEntity;
import ro.unibuc.prodeng.model.BidStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends MongoRepository<BidEntity, String> {
    Optional<BidEntity> findByProjectIdAndFreelancerId(String projectId, String freelancerId);
    List<BidEntity> findByProjectId(String projectId);
    List<BidEntity> findByFreelancerIdAndStatus(String freelancerId, BidStatus status);

    @Query("{ 'projectId': ?0, 'status': 'PENDING', '_id': { '$ne': ?1 } }")
    @Update("{ '$set': { 'status': 'REJECTED' } }")
    void rejectAllPendingBidsExcept(String projectId, String excludeBidId);

    @Query("{ 'projectId': ?0, 'status': 'PENDING' }")
    @Update("{ '$set': { 'status': 'REJECTED' } }")
    void rejectAllPendingBids(String projectId);
}
