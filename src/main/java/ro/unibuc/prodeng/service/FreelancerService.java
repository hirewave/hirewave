package ro.unibuc.prodeng.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.FreelancerEntity;
import ro.unibuc.prodeng.repository.FreelancerRepository;
import ro.unibuc.prodeng.request.CreateFreelancerRequest;
import ro.unibuc.prodeng.request.UpdateFreelancerRequest;
import ro.unibuc.prodeng.response.FreelancerResponse;

@Service
public class FreelancerService {

    @Autowired
    private FreelancerRepository freelancerRepository;

    public List<FreelancerResponse> getAllFreelancers() {
        return freelancerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public FreelancerResponse getFreelancerById(String id) {
        return toResponse(getEntityById(id));
    }

    public FreelancerEntity getEntityById(String id) {
        return freelancerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Freelancer " + id));
    }

    public FreelancerResponse createFreelancer(CreateFreelancerRequest request) {
        if (freelancerRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }
        FreelancerEntity entity = new FreelancerEntity(
                null, request.name(), request.email(),
                request.skills(), request.hourlyRate(),
                0, 0
        );
        return toResponse(freelancerRepository.save(entity));
    }

    public FreelancerResponse updateFreelancer(String id, UpdateFreelancerRequest request) {
        FreelancerEntity existing = getEntityById(id);
        // If email changed, check uniqueness
        if (!existing.email().equals(request.email())) {
            if (freelancerRepository.findByEmail(request.email()).isPresent()) {
                throw new IllegalArgumentException("Email already exists: " + request.email());
            }
        }
        FreelancerEntity updated = new FreelancerEntity(
                id, request.name(), request.email(),
                request.skills(), request.hourlyRate(),
                existing.totalRatings(), existing.ratingSum()
        );
        return toResponse(freelancerRepository.save(updated));
    }

    public void deleteFreelancer(String id) {
        if (!freelancerRepository.existsById(id)) {
            throw new EntityNotFoundException("Freelancer " + id);
        }
        freelancerRepository.deleteById(id);
    }

    public FreelancerResponse addRating(String freelancerId, int rating) {
        if (!freelancerRepository.existsById(freelancerId)) {
            throw new EntityNotFoundException("Freelancer " + freelancerId);
        }
        freelancerRepository.incrementRating(freelancerId, rating);
        return toResponse(getEntityById(freelancerId));
    }

    private FreelancerResponse toResponse(FreelancerEntity entity) {
        double averageRating = entity.totalRatings() == 0
                ? 0.0
                : (double) entity.ratingSum() / entity.totalRatings();
        return new FreelancerResponse(
                entity.id(), entity.name(), entity.email(),
                entity.skills(), entity.hourlyRate(),
                averageRating, entity.totalRatings()
        );
    }
}