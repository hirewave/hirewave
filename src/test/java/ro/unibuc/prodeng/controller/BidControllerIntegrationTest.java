package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.model.BidEntity;
import ro.unibuc.prodeng.model.BidStatus;
import ro.unibuc.prodeng.model.ClientEntity;
import ro.unibuc.prodeng.model.FreelancerEntity;
import ro.unibuc.prodeng.model.Project;
import ro.unibuc.prodeng.repository.BidRepository;
import ro.unibuc.prodeng.repository.ClientRepository;
import ro.unibuc.prodeng.repository.FreelancerRepository;
import ro.unibuc.prodeng.repository.ProjectRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BidControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FreelancerRepository freelancerRepository;

    @Autowired
    private ClientRepository clientRepository;

    private ClientEntity client;
    private FreelancerEntity freelancer;
    private Project project;

    @BeforeEach
    void setup() {
        client = clientRepository.save(new ClientEntity(null, "Client", "client-it@example.com", 0, 0));
        freelancer = freelancerRepository.save(new FreelancerEntity(
                null,
                "Freelancer",
                "freelancer-it@example.com",
                List.of("Java", "Spring"),
                55.0,
                0,
                0
        ));
        project = createProject(client.id(), "Base Project", List.of("Java"), "OPEN");
    }

    @AfterEach
    void cleanup() {
        bidRepository.deleteAll();
        projectRepository.deleteAll();
        freelancerRepository.deleteAll();
        clientRepository.deleteAll();
    }

    @Test
    void testSubmitBid_validRequest_createsPendingBid() throws Exception {
        // Arrange
        String requestJson = bidRequest(project.getId(), freelancer.id(), 120.0, "Can start now");

        // Act & Assert
        mockMvc.perform(post("/api/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(project.getId()))
                .andExpect(jsonPath("$.freelancerId").value(freelancer.id()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testSubmitBid_projectNotOpen_returnsBadRequest() throws Exception {
        // Arrange
        Project cancelledProject = createProject(client.id(), "Cancelled Project", List.of("Java"), "CANCELLED");

        // Act & Assert
        mockMvc.perform(post("/api/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bidRequest(cancelledProject.getId(), freelancer.id(), 100.0, "Bid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testSubmitBid_duplicateBid_returnsBadRequest() throws Exception {
        // Arrange
        bidRepository.save(new BidEntity(null, project.getId(), freelancer.id(), 100.0, "First", BidStatus.PENDING));

        // Act & Assert
        mockMvc.perform(post("/api/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bidRequest(project.getId(), freelancer.id(), 110.0, "Second")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Freelancer has already placed a bid on this project"));
    }

    @Test
    void testSubmitBid_missingSkillMatch_returnsBadRequest() throws Exception {
        // Arrange
        Project pythonProject = createProject(client.id(), "Python Project", List.of("Python"), "OPEN");

        // Act & Assert
        mockMvc.perform(post("/api/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bidRequest(pythonProject.getId(), freelancer.id(), 140.0, "I can do it")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Freelancer does not have any of the required skills for this project"));
    }

    @Test
    void testSubmitBid_maxConcurrentProjectsReached_returnsBadRequest() throws Exception {
        // Arrange
        for (int i = 0; i < 3; i++) {
            Project activeProject = createProject(client.id(), "InProgress " + i, List.of("Java"), "IN_PROGRESS");
            bidRepository.save(new BidEntity(null, activeProject.getId(), freelancer.id(), 90.0 + i, "accepted", BidStatus.ACCEPTED));
        }

        // Act & Assert
        mockMvc.perform(post("/api/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bidRequest(project.getId(), freelancer.id(), 150.0, "new")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Freelancer is already working on 3 concurrent projects"));
    }

    @Test
    void testSubmitBid_unknownProject_returnsNotFound() throws Exception {
        // Arrange
        String unknownProjectId = "missing-project-id";

        // Act & Assert
        mockMvc.perform(post("/api/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bidRequest(unknownProjectId, freelancer.id(), 90.0, "unknown")))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSubmitBid_invalidAmount_returnsBadRequest() throws Exception {
        // Arrange
        String invalidAmountRequest = bidRequest(project.getId(), freelancer.id(), 0.0, "invalid amount");

        // Act & Assert
        mockMvc.perform(post("/api/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidAmountRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testGetBidById_existingBid_returnsBid() throws Exception {
        // Arrange
        BidEntity bid = bidRepository.save(new BidEntity(null, project.getId(), freelancer.id(), 200.0, "hello", BidStatus.PENDING));

        // Act & Assert
        mockMvc.perform(get("/api/bids/{id}", bid.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bid.id()))
                .andExpect(jsonPath("$.amount").value(200.0));
    }

    @Test
    void testGetBidById_missingBid_returnsNotFound() throws Exception {
        // Arrange
        String unknownBidId = "missing-bid-id";

        // Act & Assert
        mockMvc.perform(get("/api/bids/{id}", unknownBidId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Bid " + unknownBidId));
    }

    @Test
    void testGetBidsForProject_existingProject_returnsProjectBidsOnly() throws Exception {
        // Arrange
        FreelancerEntity secondFreelancer = freelancerRepository.save(new FreelancerEntity(
                null,
                "Freelancer2",
                "freelancer2-it@example.com",
                List.of("Java"),
                45.0,
                0,
                0
        ));
        bidRepository.save(new BidEntity(null, project.getId(), freelancer.id(), 101.0, "m1", BidStatus.PENDING));
        bidRepository.save(new BidEntity(null, project.getId(), secondFreelancer.id(), 102.0, "m2", BidStatus.PENDING));

        // Act & Assert
        mockMvc.perform(get("/api/bids").param("projectId", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetBidsForProject_missingProject_returnsNotFound() throws Exception {
        // Arrange
        String unknownProjectId = "unknown-project";

        // Act & Assert
        mockMvc.perform(get("/api/bids").param("projectId", unknownProjectId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testRejectBid_pendingBid_changesStatusToRejected() throws Exception {
        // Arrange
        BidEntity bid = bidRepository.save(new BidEntity(null, project.getId(), freelancer.id(), 90.0, "reject me", BidStatus.PENDING));

        // Act & Assert
        mockMvc.perform(patch("/api/bids/{id}/reject", bid.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void testRejectBid_nonPendingBid_returnsBadRequest() throws Exception {
        // Arrange
        BidEntity bid = bidRepository.save(new BidEntity(null, project.getId(), freelancer.id(), 90.0, "already accepted", BidStatus.ACCEPTED));

        // Act & Assert
        mockMvc.perform(patch("/api/bids/{id}/reject", bid.id()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testAcceptBid_pendingBid_acceptsAndUpdatesProjectAndRejectsCompetitors() throws Exception {
        // Arrange
        FreelancerEntity secondFreelancer = freelancerRepository.save(new FreelancerEntity(
                null,
                "Freelancer3",
                "freelancer3-it@example.com",
                List.of("Java"),
                49.0,
                0,
                0
        ));

        BidEntity acceptedCandidate = bidRepository.save(new BidEntity(null, project.getId(), freelancer.id(), 80.0, "pick me", BidStatus.PENDING));
        BidEntity toReject = bidRepository.save(new BidEntity(null, project.getId(), secondFreelancer.id(), 85.0, "pick me 2", BidStatus.PENDING));

        // Act & Assert
        mockMvc.perform(patch("/api/bids/{id}/accept", acceptedCandidate.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        BidEntity refreshedAccepted = bidRepository.findById(acceptedCandidate.id()).orElseThrow();
        BidEntity refreshedRejected = bidRepository.findById(toReject.id()).orElseThrow();
        Project refreshedProject = projectRepository.findById(project.getId()).orElseThrow();

        assertThat(refreshedAccepted.status()).isEqualTo(BidStatus.ACCEPTED);
        assertThat(refreshedRejected.status()).isEqualTo(BidStatus.REJECTED);
        assertThat(refreshedProject.getStatus().name()).isEqualTo("IN_PROGRESS");
        assertThat(refreshedProject.getAwardedFreelancerId()).isEqualTo(freelancer.id());
    }

    @Test
    void testAcceptBid_bidNotPending_returnsBadRequest() throws Exception {
        // Arrange
        BidEntity rejectedBid = bidRepository.save(new BidEntity(null, project.getId(), freelancer.id(), 80.0, "done", BidStatus.REJECTED));

        // Act & Assert
        mockMvc.perform(patch("/api/bids/{id}/accept", rejectedBid.id()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testAcceptBid_projectNotOpen_returnsBadRequest() throws Exception {
        // Arrange
        Project cancelledProject = createProject(client.id(), "Cannot Accept Here", List.of("Java"), "CANCELLED");
        BidEntity bid = bidRepository.save(new BidEntity(null, cancelledProject.getId(), freelancer.id(), 130.0, "cannot accept", BidStatus.PENDING));

        // Act & Assert
        mockMvc.perform(patch("/api/bids/{id}/accept", bid.id()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    private String bidRequest(String projectId, String freelancerId, double amount, String message) throws Exception {
        return objectMapper.writeValueAsString(new BidRequest(projectId, freelancerId, amount, message));
    }

    private Project createProject(String clientId, String title, List<String> requiredSkills, String status) {
        Project p = new Project();
        p.setTitle(title);
        p.setDescription("Integration test project");
        p.setClientId(clientId);
        p.setRequiredSkills(requiredSkills);
        p.setBudget(1000.0);
        p.setStatus(status);
        return projectRepository.save(p);
    }

    private record BidRequest(String projectId, String freelancerId, double amount, String message) {
    }
}