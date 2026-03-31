package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.model.FreelancerEntity;
import ro.unibuc.prodeng.repository.FreelancerRepository;
import ro.unibuc.prodeng.request.CreateFreelancerRequest;
import ro.unibuc.prodeng.request.RateFreelancerRequest;
import ro.unibuc.prodeng.request.UpdateFreelancerRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FreelancerControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FreelancerRepository freelancerRepository;

    @AfterEach
    void cleanup() {
        freelancerRepository.deleteAll();
    }

    @Test
    void testGetAllFreelancers_returnsList() throws Exception {
        // Arrange
        freelancerRepository.save(new FreelancerEntity(null, "Freelancer 1", "f1@example.com", List.of("Java"), 10.0, 0, 0));
        freelancerRepository.save(new FreelancerEntity(null, "Freelancer 2", "f2@example.com", List.of("Python"), 20.0, 0, 0));

        // Act & Assert
        mockMvc.perform(get("/api/freelancers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Freelancer 1")));
    }

    @Test
    void testGetFreelancerById_returnsFreelancer() throws Exception {
        // Arrange
        FreelancerEntity freelancer = freelancerRepository.save(new FreelancerEntity(null, "Freelancer 1", "f1@example.com", List.of("Java"), 10.0, 0, 0));

        // Act & Assert
        mockMvc.perform(get("/api/freelancers/{id}", freelancer.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Freelancer 1")));
    }

    @Test
    void testCreateFreelancer_returnsCreated() throws Exception {
        // Arrange
        CreateFreelancerRequest req = new CreateFreelancerRequest("New Freelancer", "newf@example.com", List.of("Java"), 15.0);

        // Act & Assert
        mockMvc.perform(post("/api/freelancers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Freelancer")));
        
        assertThat(freelancerRepository.findAll()).hasSize(1);
    }

    @Test
    void testCreateFreelancer_missingSkills_returnsBadRequest() throws Exception {
        // Arrange
        CreateFreelancerRequest req = new CreateFreelancerRequest("New Freelancer", "newf@example.com", List.of(), 15.0);

        // Act & Assert
        mockMvc.perform(post("/api/freelancers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateFreelancer_returnsUpdated() throws Exception {
        // Arrange
        FreelancerEntity freelancer = freelancerRepository.save(new FreelancerEntity(null, "Freelancer 1", "f1@example.com", List.of("Java"), 10.0, 0, 0));
        UpdateFreelancerRequest req = new UpdateFreelancerRequest("Updated Freelancer", "updatedf@example.com", List.of("Python"), 20.0);

        // Act & Assert
        mockMvc.perform(put("/api/freelancers/{id}", freelancer.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Freelancer")));
        
        FreelancerEntity updatedFreelancer = freelancerRepository.findById(freelancer.id()).orElseThrow();
        assertThat(updatedFreelancer.name()).isEqualTo("Updated Freelancer");
    }

    @Test
    void testDeleteFreelancer_returnsNoContent() throws Exception {
        // Arrange
        FreelancerEntity freelancer = freelancerRepository.save(new FreelancerEntity(null, "Freelancer 1", "f1@example.com", List.of("Java"), 10.0, 0, 0));

        // Act & Assert
        mockMvc.perform(delete("/api/freelancers/{id}", freelancer.id()))
                .andExpect(status().isNoContent());
        
        assertThat(freelancerRepository.findById(freelancer.id())).isEmpty();
    }

    @Test
    void testAddRating_returnsUpdated() throws Exception {
        // Arrange
        FreelancerEntity freelancer = freelancerRepository.save(new FreelancerEntity(null, "Freelancer 1", "f1@example.com", List.of("Java"), 10.0, 0, 0));
        RateFreelancerRequest req = new RateFreelancerRequest(5);

        // Act & Assert
        mockMvc.perform(post("/api/freelancers/{id}/ratings", freelancer.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating", is(5.0)));
        
        FreelancerEntity ratedFreelancer = freelancerRepository.findById(freelancer.id()).orElseThrow();
        assertThat(ratedFreelancer.totalRatings()).isEqualTo(1);
        assertThat(ratedFreelancer.ratingSum()).isEqualTo(5);
    }

    @Test
    void testGetFreelancerById_missingFreelancer_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/freelancers/{id}", "unknown-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testCreateFreelancer_duplicateEmail_returnsBadRequest() throws Exception {
        freelancerRepository.save(new FreelancerEntity(null, "Existing Freelancer", "duplicate@example.com", List.of("Java"), 10.0, 0, 0));
        CreateFreelancerRequest req = new CreateFreelancerRequest("New Freelancer", "duplicate@example.com", List.of("Java"), 15.0);

        mockMvc.perform(post("/api/freelancers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testUpdateFreelancer_missingFreelancer_returnsNotFound() throws Exception {
        UpdateFreelancerRequest req = new UpdateFreelancerRequest("Updated Freelancer", "updatedf@example.com", List.of("Python"), 20.0);

        mockMvc.perform(put("/api/freelancers/{id}", "unknown-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testAddRating_missingFreelancer_returnsNotFound() throws Exception {
        RateFreelancerRequest req = new RateFreelancerRequest(5);

        mockMvc.perform(post("/api/freelancers/{id}/ratings", "unknown-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
