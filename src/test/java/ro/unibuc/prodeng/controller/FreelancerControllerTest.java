package ro.unibuc.prodeng.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.prodeng.request.CreateFreelancerRequest;
import ro.unibuc.prodeng.request.RateFreelancerRequest;
import ro.unibuc.prodeng.request.UpdateFreelancerRequest;
import ro.unibuc.prodeng.response.FreelancerResponse;
import ro.unibuc.prodeng.service.FreelancerService;

@ExtendWith(MockitoExtension.class)
class FreelancerControllerTest {

    @Mock
    private FreelancerService freelancerService;

    @InjectMocks
    private FreelancerController freelancerController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(freelancerController).build();
    }

    @Test
    void testGetAllFreelancers_returnsList() throws Exception {
        // Arrange
        FreelancerResponse res = new FreelancerResponse("1", "F1", "f1@ex.com", Arrays.asList("Java"), 10.0, 0.0, 0);
        when(freelancerService.getAllFreelancers()).thenReturn(Arrays.asList(res));

        // Act & Assert
        mockMvc.perform(get("/api/freelancers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("F1")));
    }

    @Test
    void testGetFreelancerById_returnsFreelancer() throws Exception {
        // Arrange
        FreelancerResponse res = new FreelancerResponse("1", "F1", "f1@ex.com", Arrays.asList("Java"), 10.0, 0.0, 0);
        when(freelancerService.getFreelancerById("1")).thenReturn(res);

        // Act & Assert
        mockMvc.perform(get("/api/freelancers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("F1")));
    }

    @Test
    void testCreateFreelancer_returnsCreated() throws Exception {
        // Arrange
        CreateFreelancerRequest req = new CreateFreelancerRequest("F1", "f1@ex.com", Arrays.asList("Java"), 10.0);
        FreelancerResponse res = new FreelancerResponse("1", "F1", "f1@ex.com", Arrays.asList("Java"), 10.0, 0.0, 0);
        when(freelancerService.createFreelancer(any(CreateFreelancerRequest.class))).thenReturn(res);

        // Act & Assert
        mockMvc.perform(post("/api/freelancers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("1")));
    }

    @Test
    void testCreateFreelancer_missingSkills_returnsBadRequest() throws Exception {
        // Arrange
        // Skills list is empty (or null), which should trigger a @NotEmpty / @NotNull validation error
        CreateFreelancerRequest req = new CreateFreelancerRequest("F1", "f1@ex.com", java.util.Collections.emptyList(), 10.0);

        // Act & Assert
        mockMvc.perform(post("/api/freelancers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateFreelancer_returnsUpdated() throws Exception {
        // Arrange
        UpdateFreelancerRequest req = new UpdateFreelancerRequest("F1", "f1@ex.com", Arrays.asList("Java"), 10.0);
        FreelancerResponse res = new FreelancerResponse("1", "F1", "f1@ex.com", Arrays.asList("Java"), 10.0, 0.0, 0);
        when(freelancerService.updateFreelancer(eq("1"), any(UpdateFreelancerRequest.class))).thenReturn(res);

        // Act & Assert
        mockMvc.perform(put("/api/freelancers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("1")));
    }

    @Test
    void testDeleteFreelancer_returnsNoContent() throws Exception {
        // Arrange
        doNothing().when(freelancerService).deleteFreelancer("1");

        // Act & Assert
        mockMvc.perform(delete("/api/freelancers/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testAddRating_returnsUpdated() throws Exception {
        // Arrange
        RateFreelancerRequest req = new RateFreelancerRequest(5);
        FreelancerResponse res = new FreelancerResponse("1", "F1", "f1@ex.com", Arrays.asList("Java"), 10.0, 5.0, 1);
        when(freelancerService.addRating("1", 5)).thenReturn(res);

        // Act & Assert
        mockMvc.perform(post("/api/freelancers/1/ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating", is(5.0)));
    }
}
