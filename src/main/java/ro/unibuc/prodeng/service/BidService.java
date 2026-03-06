package ro.unibuc.prodeng.service;

import java.util.Collections;
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

    public List<BidResponse> getAllBids() {
        return bidRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BidResponse> getBidsByProject(String projectId) {
        return bidRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BidResponse> getBidsByFreelancer(String freelancerId) {
        return bidRepository.findByFreelancerId(freelancerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public BidResponse getBidById(String id) {
        BidEntity bid = bidRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bid " + id));
        return toResponse(bid);
    }

    public BidResponse createBid(CreateBidRequest request) {
        ProjectEntity project = projectService.getEntityById(request.projectId());
        FreelancerEntity freelancer = freelancerService.getEntityById(request.freelancerId());

        // Project must be OPEN
        if (project.status() != ProjectStatus.OPEN) {
            throw new IllegalArgumentException("Can only bid on projects with OPEN status");
        }

        // Prevent duplicate bids
        if (bidRepository.findByProjectIdAndFreelancerId(request.projectId(), request.freelancerId()).isPresent()) {
            throw new IllegalArgumentException("Freelancer has already bid on this project");
        }

        // Availability check: max concurrent active projects
        int activeProjects = projectService.countActiveProjectsForFreelancer(request.freelancerId());
        if (activeProjects >= MAX_CONCURRENT_PROJECTS) {
            throw new IllegalArgumentException("Freelancer is already working on " + MAX_CONCURRENT_PROJECTS + " concurrent projects");
        }

        // Skill-based eligibility: at least one matching skill
        boolean hasMatchingSkill = !Collections.disjoint(freelancer.skills(), project.requiredSkills());
        if (!hasMatchingSkill) {
            throw new IllegalArgumentException("Freelancer must have at least one skill matching the project requirements");
        }

        BidEntity entity = new BidEntity(
                null, request.projectId(), request.freelancerId(),
                request.amount(), BidStatus.PENDING
        );
        return toResponse(bidRepository.save(entity));
    }

    public BidResponse acceptBid(String bidId) {
        BidEntity bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new EntityNotFoundException("Bid " + bidId));

        if (bid.status() != BidStatus.PENDING) {
            throw new IllegalArgumentException("Can only accept pending bids");
        }

        ProjectEntity project = projectService.getEntityById(bid.projectId());
        if (project.status() != ProjectStatus.OPEN) {
            throw new IllegalArgumentException("Project is no longer open for bids");
        }

        // Accept this bid
        BidEntity accepted = new BidEntity(bid.id(), bid.projectId(), bid.freelancerId(), bid.amount(), BidStatus.ACCEPTED);
        bidRepository.save(accepted);

        // Reject all other pending bids for this project
        List<BidEntity> otherBids = bidRepository.findByProjectId(bid.projectId());
        for (BidEntity other : otherBids) {
            if (!other.id().equals(bid.id()) && other.status() == BidStatus.PENDING) {
                BidEntity rejected = new BidEntity(other.id(), other.projectId(), other.freelancerId(), other.amount(), BidStatus.REJECTED);
                bidRepository.save(rejected);
            }
        }

        // Move project to IN_PROGRESS
        projectService.startProject(bid.projectId(), bid.freelancerId());

        return toResponse(accepted);
    }

    public BidResponse rejectBid(String bidId) {
        BidEntity bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new EntityNotFoundException("Bid " + bidId));

        if (bid.status() != BidStatus.PENDING) {
            throw new IllegalArgumentException("Can only reject pending bids");
        }

        BidEntity rejected = new BidEntity(bid.id(), bid.projectId(), bid.freelancerId(), bid.amount(), BidStatus.REJECTED);
        return toResponse(bidRepository.save(rejected));
    }

    private BidResponse toResponse(BidEntity entity) {
        ProjectEntity project = projectService.getEntityById(entity.projectId());
        FreelancerEntity freelancer = freelancerService.getEntityById(entity.freelancerId());
        return new BidResponse(
                entity.id(), entity.projectId(), project.title(),
                entity.freelancerId(), freelancer.name(),
                entity.amount(), entity.status().name()
        );
    }
}
