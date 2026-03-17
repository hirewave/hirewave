package ro.unibuc.prodeng.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.FreelancerEntity;
import ro.unibuc.prodeng.repository.FreelancerRepository;
import ro.unibuc.prodeng.request.CreateFreelancerRequest;
import ro.unibuc.prodeng.request.UpdateFreelancerRequest;
import ro.unibuc.prodeng.response.FreelancerResponse;

@ExtendWith(MockitoExtension.class)
class FreelancerServiceTest {

    @Mock
    private FreelancerRepository freelancerRepository;

    @InjectMocks
    private FreelancerService freelancerService;

    @Test
    void testGetAllFreelancers_returnsList() {
        // Arrange
        FreelancerEntity f1 = new FreelancerEntity("1", "F1", "f1@ex.com", Arrays.asList("Java"), 10.0, 2, 10);
        when(freelancerRepository.findAll()).thenReturn(Arrays.asList(f1));

        // Act
        List<FreelancerResponse> res = freelancerService.getAllFreelancers();

        // Assert
        assertEquals(1, res.size());
        assertEquals("F1", res.get(0).name());
        assertEquals(5.0, res.get(0).averageRating());
    }

    @Test
    void testGetFreelancerById_existing_returnsFreelancer() {
        // Arrange
        FreelancerEntity f1 = new FreelancerEntity("1", "F1", "f1@ex.com", Arrays.asList("Java"), 10.0, 0, 0);
        when(freelancerRepository.findById("1")).thenReturn(Optional.of(f1));

        // Act
        FreelancerResponse res = freelancerService.getFreelancerById("1");

        // Assert
        assertEquals("F1", res.name());
        assertEquals(0.0, res.averageRating());
    }

    @Test
    void testGetFreelancerById_notFound_throwsException() {
        // Arrange
        when(freelancerRepository.findById("1")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> freelancerService.getFreelancerById("1"));
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0.0",    // Edge case: no ratings (zero safe)
        "2, 8, 4.0",    // Scenario 5: (5 + 3) = 8 ratingSum, 2 totals
        "5, 25, 5.0",   // Perfect 5.0 average
        "100, 100, 1.0",// Terrible 1.0 average
        "1, 3, 3.0",    // Single rating of 3
        "3, 10, 3.3333333333333335" // Decimal math check
    })
    void testAverageRatingCalculation(int totalRatings, int ratingSum, double expectedAverage) {
        // Arrange
        FreelancerEntity f = new FreelancerEntity("1", "Rated", "rated@ex.com", Arrays.asList("Java"), 10.0, totalRatings, ratingSum);
        when(freelancerRepository.findById("1")).thenReturn(Optional.of(f));

        // Act
        FreelancerResponse res = freelancerService.getFreelancerById("1");

        // Assert
        assertEquals(expectedAverage, res.averageRating(), 0.0001);
    }

    @Test    void testCreateFreelancer_valid_saves() {
        // Arrange
        CreateFreelancerRequest req = new CreateFreelancerRequest("F1", "f1@ex.com", Arrays.asList("Java"), 10.0);
        when(freelancerRepository.findByEmail("f1@ex.com")).thenReturn(Optional.empty());
        when(freelancerRepository.save(any(FreelancerEntity.class))).thenAnswer(inv -> {
            FreelancerEntity e = inv.getArgument(0);
            return new FreelancerEntity("gen-id", e.name(), e.email(), e.skills(), e.hourlyRate(), e.totalRatings(), e.ratingSum());
        });

        FreelancerResponse res = freelancerService.createFreelancer(req);
        assertEquals("gen-id", res.id());
    }

    @Test
    void testCreateFreelancer_emailTaken_throwsException() {
        // Arrange
        CreateFreelancerRequest req = new CreateFreelancerRequest("F1", "taken@ex.com", Arrays.asList("Java"), 10.0);
        when(freelancerRepository.findByEmail("taken@ex.com")).thenReturn(Optional.of(mock(FreelancerEntity.class)));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> freelancerService.createFreelancer(req));
    }

    @Test
    void testCreateFreelancer_duplicateKey_throwsException() {
        // Arrange
        CreateFreelancerRequest req = new CreateFreelancerRequest("F1", "f1@ex.com", Arrays.asList("Java"), 10.0);
        when(freelancerRepository.findByEmail("f1@ex.com")).thenReturn(Optional.empty());
        when(freelancerRepository.save(any(FreelancerEntity.class))).thenThrow(new DuplicateKeyException("dup"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> freelancerService.createFreelancer(req));
    }

    @Test
    void testUpdateFreelancer_valid_updates() {
        // Arrange
        UpdateFreelancerRequest req = new UpdateFreelancerRequest("F1-up", "up@ex.com", Arrays.asList("Java"), 20.0);
        FreelancerEntity existing = new FreelancerEntity("1", "F1", "f1@ex.com", Arrays.asList("Java"), 10.0, 1, 5);
        when(freelancerRepository.findById("1")).thenReturn(Optional.of(existing));
        when(freelancerRepository.findByEmail("up@ex.com")).thenReturn(Optional.empty());
        when(freelancerRepository.save(any(FreelancerEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        FreelancerResponse res = freelancerService.updateFreelancer("1", req);

        // Assert
        assertEquals("F1-up", res.name());
        assertEquals("up@ex.com", res.email());
        assertEquals(20.0, res.hourlyRate());
        assertEquals(5.0, res.averageRating());
    }

    @Test
    void testUpdateFreelancer_emailTaken_throwsException() {
        // Arrange
        UpdateFreelancerRequest req = new UpdateFreelancerRequest("F1-up", "taken@ex.com", Arrays.asList("Java"), 20.0);
        FreelancerEntity existing = new FreelancerEntity("1", "F1", "f1@ex.com", Arrays.asList("Java"), 10.0, 1, 5);
        when(freelancerRepository.findById("1")).thenReturn(Optional.of(existing));
        when(freelancerRepository.findByEmail("taken@ex.com")).thenReturn(Optional.of(mock(FreelancerEntity.class)));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> freelancerService.updateFreelancer("1", req));
    }

    @Test
    void testUpdateFreelancer_duplicateKey_throwsException() {
        // Arrange
        UpdateFreelancerRequest req = new UpdateFreelancerRequest("F1-up", "f1@ex.com", Arrays.asList("Java"), 20.0);
        FreelancerEntity existing = new FreelancerEntity("1", "F1", "f1@ex.com", Arrays.asList("Java"), 10.0, 1, 5);
        when(freelancerRepository.findById("1")).thenReturn(Optional.of(existing));
        when(freelancerRepository.save(any(FreelancerEntity.class))).thenThrow(new DuplicateKeyException("dup"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> freelancerService.updateFreelancer("1", req));
    }

    @Test
    void testDeleteFreelancer_existing_deletes() {
        // Arrange
        when(freelancerRepository.existsById("1")).thenReturn(true);

        // Act
        freelancerService.deleteFreelancer("1");

        // Assert
        verify(freelancerRepository).deleteById("1");
    }

    @Test
    void testDeleteFreelancer_notFound_throwsException() {
        // Arrange
        when(freelancerRepository.existsById("1")).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> freelancerService.deleteFreelancer("1"));
    }

    @Test
    void testAddRating_existing_increments() {
        // Arrange
        when(freelancerRepository.existsById("1")).thenReturn(true);
        FreelancerEntity f = new FreelancerEntity("1", "F", "f@e.c", Arrays.asList("J"), 10.0, 1, 4);
        when(freelancerRepository.findById("1")).thenReturn(Optional.of(f)); // called by getEntityById in addRating

        // Act
        FreelancerResponse res = freelancerService.addRating("1", 4);

        // Assert
        verify(freelancerRepository).incrementRating("1", 4);
        assertEquals(4.0, res.averageRating());
    }

    @Test
    void testAddRating_notFound_throwsException() {
        // Arrange
        when(freelancerRepository.existsById("1")).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> freelancerService.addRating("1", 5));
    }
}
