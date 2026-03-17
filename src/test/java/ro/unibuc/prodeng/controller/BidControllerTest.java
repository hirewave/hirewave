package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ro.unibuc.prodeng.model.BidStatus;
import ro.unibuc.prodeng.request.CreateBidRequest;
import ro.unibuc.prodeng.response.BidResponse;
import ro.unibuc.prodeng.service.BidService;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
class BidControllerTest {

    @Mock
    private BidService bidService;

    @InjectMocks
    private BidController bidController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private BidResponse testBid1 = new BidResponse("bid-1", "project-1", "freelancer-1", 100.0, "Message 1", BidStatus.PENDING);
    private BidResponse testBid2 = new BidResponse("bid-2", "project-1", "freelancer-2", 200.0, "Message 2", BidStatus.ACCEPTED);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bidController).build();
    }

    @Test
    void testGetBidById_returnsBid() throws Exception {
        // Arrange
        when(bidService.getBidById("bid-1")).thenReturn(testBid1);


        // Act & Assert
        mockMvc.perform(get("/api/bids/{id}", "bid-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("bid-1")))
                .andExpect(jsonPath("$.projectId", is("project-1")))
                .andExpect(jsonPath("$.amount", is(100.0)));

        verify(bidService, times(1)).getBidById("bid-1");
    }

    @Test
    void testGetBidsForProject_returnsList() throws Exception {
        // Arrange
        List<BidResponse> bids = Arrays.asList(testBid1, testBid2);
        when(bidService.getBidsForProject("project-1")).thenReturn(bids);


        // Act & Assert
        mockMvc.perform(get("/api/bids")
                        .param("projectId", "project-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("bid-1")))
                .andExpect(jsonPath("$[1].id", is("bid-2")));

        verify(bidService, times(1)).getBidsForProject("project-1");
    }

    @Test
    void testSubmitBid_createsBid() throws Exception {
        // Arrange
        CreateBidRequest request = new CreateBidRequest("project-1", "freelancer-1", 100.0, "Message 1");
        when(bidService.submitBid(any(CreateBidRequest.class))).thenReturn(testBid1);


        // Act & Assert
        mockMvc.perform(post("/api/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("bid-1")))
                .andExpect(jsonPath("$.message", is("Message 1")));

        verify(bidService, times(1)).submitBid(any(CreateBidRequest.class));
    }

    @Test
    void testAcceptBid_returnsAcceptedBid() throws Exception {
        // Arrange
        when(bidService.acceptBid("bid-2")).thenReturn(testBid2);


        // Act & Assert
        mockMvc.perform(patch("/api/bids/{id}/accept", "bid-2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("bid-2")))
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        verify(bidService, times(1)).acceptBid("bid-2");
    }

    @Test
    void testRejectBid_returnsRejectedBid() throws Exception {
        // Arrange
        BidResponse rejectedBid = new BidResponse("bid-1", "project-1", "freelancer-1", 100.0, "Message 1", BidStatus.REJECTED);
        when(bidService.rejectBid("bid-1")).thenReturn(rejectedBid);


        // Act & Assert
        mockMvc.perform(patch("/api/bids/{id}/reject", "bid-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("bid-1")))
                .andExpect(jsonPath("$.status", is("REJECTED")));

        verify(bidService, times(1)).rejectBid("bid-1");
    }
}
