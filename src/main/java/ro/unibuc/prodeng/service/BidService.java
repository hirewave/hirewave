package ro.unibuc.prodeng.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.BidEntity;
import ro.unibuc.prodeng.model.BidStatus;
import ro.unibuc.prodeng.model.FreelancerEntity;
import ro.unibuc.prodeng.model.ProjectEntity;
import ro.unibuc.prodeng.model.ProjectStatus;
import ro.unibuc.prodeng.repository.BidRepository;
import ro.unibuc.prodeng.request.CreateBidRequest;
import ro.unibuc.prodeng.response.BidResponse;

@Service
public class BidService {

    private static final int MAX_CONCURRENT_PROJECTS = 3;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private FreelancerService freelancerService;

    public List<BidResponse> getBidsForProject(String projectId) {
        projectService.getEntityById(projectId); // validates project exists
        return bidRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    public BidResponse getBidById(String id) {
        return toResponse(getEntityById(id));
    }

    public BidResponse submitBid(CreateBidRequest request) {
        ProjectEntity project = projectService.getEntityById(request.projectId());
        FreelancerEntity freelancer = freelancerService.getEntityById(request.freelancerId());

        if (project.status() != ProjectStatus.OPEN) {
            throw new IllegalArgumentException(
                    "Bids can only be placed on OPEN projects, project status: " + project.status());
        }

        if (bidRepository.findByProjectIdAndFreelancerId(request.projectId(), request.freelancerId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Freelancer has already placed a bid on this project");
        }

        boolean hasMatchingSkill = freelancer.skills().stream()
                .anyMatch(skill -> project.requiredSkills().contains(skill));
        if (!hasMatchingSkill) {
            throw new IllegalArgumentException(
                    "Freelancer does not have any of the required skills for this project");
        }

        long activeProjects = countActiveProjectsForFreelancer(request.freelancerId());
        if (activeProjects >= MAX_CONCURRENT_PROJECTS) {
            throw new IllegalArgumentException(
                    "Freelancer is already working on " + MAX_CONCURRENT_PROJECTS + " concurrent projects");
        }

        BidEntity bid = new BidEntity(
                null, request.projectId(), request.freelancerId(),
                request.amount(), request.message(), BidStatus.PENDING
        );
        return toResponse(bidRepository.save(bid));
    }

    public BidResponse acceptBid(String bidId) {
        BidEntity bid = getEntityById(bidId);

        if (bid.status() != BidStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING bids can be accepted, bid status: " + bid.status());
        }

        ProjectEntity project = projectService.getEntityById(bid.projectId());
        if (project.status() != ProjectStatus.OPEN) {
            throw new IllegalArgumentException(
                    "Can only accept bids on OPEN projects, project status: " + project.status());
        }

        BidEntity accepted = new BidEntity(
                bid.id(), bid.projectId(), bid.freelancerId(),
                bid.amount(), bid.message(), BidStatus.ACCEPTED
        );
        bidRepository.save(accepted);

        bidRepository.rejectAllPendingBidsExcept(bid.projectId(), bid.id());

        projectService.markInProgress(bid.projectId(), bid.freelancerId());

        return toResponse(accepted);
    }

    public BidResponse rejectBid(String bidId) {
        BidEntity bid = getEntityById(bidId);

        if (bid.status() != BidStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING bids can be rejected, bid status: " + bid.status());
        }

        BidEntity rejected = new BidEntity(
                bid.id(), bid.projectId(), bid.freelancerId(),
                bid.amount(), bid.message(), BidStatus.REJECTED
        );
        return toResponse(bidRepository.save(rejected));
    }

    private long countActiveProjectsForFreelancer(String freelancerId) {
        List<String> acceptedProjectIds = bidRepository
                .findByFreelancerIdAndStatus(freelancerId, BidStatus.ACCEPTED)
                .stream()
                .map(BidEntity::projectId)
                .toList();
        if (acceptedProjectIds.isEmpty()) {
            return 0;
        }
        return projectService.countInProgressByIds(acceptedProjectIds);
    }

    private BidEntity getEntityById(String id) {
        return bidRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bid " + id));
    }

    private BidResponse toResponse(BidEntity entity) {
        return new BidResponse(
                entity.id(), entity.projectId(), entity.freelancerId(),
                entity.amount(), entity.message(), entity.status()
        );
    }
}
