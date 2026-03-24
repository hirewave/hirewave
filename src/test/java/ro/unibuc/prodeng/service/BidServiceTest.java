package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.BidEntity;
import ro.unibuc.prodeng.model.BidStatus;
import ro.unibuc.prodeng.model.FreelancerEntity;
import ro.unibuc.prodeng.model.ProjectEntity;
import ro.unibuc.prodeng.model.ProjectStatus;
import ro.unibuc.prodeng.repository.BidRepository;
import ro.unibuc.prodeng.request.CreateBidRequest;
import ro.unibuc.prodeng.response.BidResponse;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private FreelancerService freelancerService;

    @InjectMocks
    private BidService bidService;

    private final FreelancerEntity freelancer = new FreelancerEntity(
            "fid", "Dev User", "dev@example.com",
            List.of("Java", "Spring"), 80.0, 0, 0);

    private final ProjectEntity openProject = new ProjectEntity(
            "pid", "Build REST API", "A REST API project", "cid",
            List.of("Java", "Python"), 2000.0, ProjectStatus.OPEN, null);

    private final CreateBidRequest validRequest =
            new CreateBidRequest("pid", "fid", 1500.0, "I can handle this");

    @Test
    void getBidById_existingBid_returnsBid() {
        // Arrange
        BidEntity bid = new BidEntity("bid1", "proj1", "free1", 100.0, "msg", BidStatus.PENDING);
        when(bidRepository.findById("bid1")).thenReturn(Optional.of(bid));

        // Act
        BidResponse result = bidService.getBidById("bid1");

        // Assert
        assertNotNull(result);
        assertEquals("bid1", result.id());
    }

    @Test
    void getBidById_nonExistingBid_throwsEntityNotFoundException() {
        // Arrange
        when(bidRepository.findById("nonexist")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> bidService.getBidById("nonexist"));
    }

    // ─── submitBid ────────────────────────────────────────────────────────────

    @Test
    void submitBid_validRequest_returnsPendingBid() {
        when(projectService.getEntityById("pid")).thenReturn(openProject);
        when(freelancerService.getEntityById("fid")).thenReturn(freelancer);
        when(bidRepository.findByProjectIdAndFreelancerId("pid", "fid")).thenReturn(Optional.empty());
        when(bidRepository.findByFreelancerIdAndStatus("fid", BidStatus.ACCEPTED)).thenReturn(Collections.emptyList());
        when(bidRepository.save(any(BidEntity.class))).thenAnswer(inv -> {
            BidEntity b = inv.getArgument(0);
            return new BidEntity("bid1", b.projectId(), b.freelancerId(), b.amount(), b.message(), b.status());
        });

        BidResponse result = bidService.submitBid(validRequest);

        assertNotNull(result.id());
        assertEquals("pid", result.projectId());
        assertEquals("fid", result.freelancerId());
        assertEquals(1500.0, result.amount());
        assertEquals(BidStatus.PENDING, result.status());
        verify(bidRepository, times(1)).save(any(BidEntity.class));
    }

    @Test
    void submitBid_projectNotOpen_throwsIllegalArgumentException() {
        ProjectEntity inProgressProject = new ProjectEntity(
                "pid", "Build REST API", "A REST API project", "cid",
                List.of("Java"), 2000.0, ProjectStatus.IN_PROGRESS, "fid");
        when(projectService.getEntityById("pid")).thenReturn(inProgressProject);
        when(freelancerService.getEntityById("fid")).thenReturn(freelancer);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bidService.submitBid(validRequest));

        assertTrue(ex.getMessage().contains("OPEN"));
    }

    @Test
    void submitBid_duplicateBid_throwsIllegalArgumentException() {
        BidEntity existing = new BidEntity("existingBid", "pid", "fid", 1200.0, "already bid", BidStatus.PENDING);
        when(projectService.getEntityById("pid")).thenReturn(openProject);
        when(freelancerService.getEntityById("fid")).thenReturn(freelancer);
        when(bidRepository.findByProjectIdAndFreelancerId("pid", "fid")).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bidService.submitBid(validRequest));

        assertTrue(ex.getMessage().contains("already placed a bid"));
    }

    @Test
    void submitBid_skillMismatch_throwsIllegalArgumentException() {
        // Arrange
        FreelancerEntity noMatchFreelancer = new FreelancerEntity(
                "fid", "Dev User", "dev@example.com",
                List.of("PHP", "Ruby"), 80.0, 0, 0);
        when(projectService.getEntityById("pid")).thenReturn(openProject);
        when(freelancerService.getEntityById("fid")).thenReturn(noMatchFreelancer);
        when(bidRepository.findByProjectIdAndFreelancerId("pid", "fid")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bidService.submitBid(validRequest));

        assertTrue(ex.getMessage().contains("required skills"));
    }

    @Test
    void submitBid_freelancerAtMaxCapacity_throwsIllegalArgumentException() {
        List<BidEntity> acceptedBids = List.of(
                new BidEntity("b1", "p1", "fid", 100.0, null, BidStatus.ACCEPTED),
                new BidEntity("b2", "p2", "fid", 100.0, null, BidStatus.ACCEPTED),
                new BidEntity("b3", "p3", "fid", 100.0, null, BidStatus.ACCEPTED));
        when(projectService.getEntityById("pid")).thenReturn(openProject);
        when(freelancerService.getEntityById("fid")).thenReturn(freelancer);
        when(bidRepository.findByProjectIdAndFreelancerId("pid", "fid")).thenReturn(Optional.empty());
        when(bidRepository.findByFreelancerIdAndStatus("fid", BidStatus.ACCEPTED)).thenReturn(acceptedBids);
        when(projectService.countInProgressByIds(anyCollection())).thenReturn(3L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bidService.submitBid(validRequest));

        assertTrue(ex.getMessage().contains("3 concurrent projects"));
    }

    // ─── acceptBid ────────────────────────────────────────────────────────────

    @Test
    void acceptBid_pendingBidOnOpenProject_acceptsAndCascades() {
        BidEntity pendingBid = new BidEntity("bid1", "pid", "fid", 1500.0, "my bid", BidStatus.PENDING);
        when(bidRepository.findById("bid1")).thenReturn(Optional.of(pendingBid));
        when(projectService.getEntityById("pid")).thenReturn(openProject);
        when(bidRepository.save(any(BidEntity.class))).thenAnswer(inv -> inv.getArgument(0));


        // Act
        BidResponse result = bidService.acceptBid("bid1");

        // Assert

        assertEquals(BidStatus.ACCEPTED, result.status());
        verify(bidRepository).rejectAllPendingBidsExcept("pid", "bid1");
        verify(projectService).markInProgress("pid", "fid");
    }

    @Test
    void acceptBid_projectNotOpen_throwsIllegalArgumentException() {
        // Arrange
        BidEntity bid = new BidEntity("bid1", "proj1", "free1", 100.0, "msg", BidStatus.PENDING);
        when(bidRepository.findById("bid1")).thenReturn(Optional.of(bid));
        ProjectEntity closedProject = new ProjectEntity("proj1", "Title", "Desc", "client1", List.of(), 500.0, ProjectStatus.IN_PROGRESS, null);
        when(projectService.getEntityById("proj1")).thenReturn(closedProject);


        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> bidService.acceptBid("bid1"));
    }

    @Test
    void acceptBid_alreadyAccepted_throwsIllegalArgumentException() {
        // Arrange
        BidEntity acceptedBid = new BidEntity("bid1", "pid", "fid", 1500.0, "my bid", BidStatus.ACCEPTED);
        when(bidRepository.findById("bid1")).thenReturn(Optional.of(acceptedBid));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bidService.acceptBid("bid1"));

        assertTrue(ex.getMessage().contains("PENDING"));
    }

    @Test
    void acceptBid_bidNotFound_throwsEntityNotFoundException() {
        // Arrange
        when(bidRepository.findById("unknown")).thenReturn(Optional.empty());


        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> bidService.acceptBid("unknown"));
    }

    // ─── rejectBid ────────────────────────────────────────────────────────────

    @Test
    void rejectBid_pendingBid_setsStatusToRejected() {
        BidEntity pendingBid = new BidEntity("bid1", "pid", "fid", 1500.0, "my bid", BidStatus.PENDING);
        when(bidRepository.findById("bid1")).thenReturn(Optional.of(pendingBid));
        when(bidRepository.save(any(BidEntity.class))).thenAnswer(inv -> inv.getArgument(0));


        // Act
        BidResponse result = bidService.rejectBid("bid1");

        // Assert

        assertEquals(BidStatus.REJECTED, result.status());
        verify(bidRepository).save(argThat(b -> b.status() == BidStatus.REJECTED));
    }

    @Test
    void rejectBid_alreadyRejected_throwsIllegalArgumentException() {
        // Arrange
        BidEntity rejectedBid = new BidEntity("bid1", "pid", "fid", 1500.0, "my bid", BidStatus.REJECTED);
        when(bidRepository.findById("bid1")).thenReturn(Optional.of(rejectedBid));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bidService.rejectBid("bid1"));

        assertTrue(ex.getMessage().contains("PENDING"));
    }

    // ─── getBidsForProject ────────────────────────────────────────────────────

    @Test
    void getBidsForProject_existingProject_returnsBids() {
        // Arrange
        BidEntity bid = new BidEntity("bid1", "pid", "fid", 1500.0, "my bid", BidStatus.PENDING);
        when(projectService.getEntityById("pid")).thenReturn(openProject);
        when(bidRepository.findByProjectId("pid")).thenReturn(List.of(bid));

        List<BidResponse> result = bidService.getBidsForProject("pid");

        assertEquals(1, result.size());
        assertEquals("bid1", result.get(0).id());
    }
}
