package ro.unibuc.prodeng.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ro.unibuc.prodeng.request.CreateFreelancerRequest;
import ro.unibuc.prodeng.request.RateFreelancerRequest;
import ro.unibuc.prodeng.request.UpdateFreelancerRequest;
import ro.unibuc.prodeng.response.FreelancerResponse;
import ro.unibuc.prodeng.service.FreelancerService;
import ro.unibuc.prodeng.service.MetricsService;

@RestController
@RequestMapping("/api/freelancers")
public class FreelancerController {

    @Autowired
    private FreelancerService freelancerService;

    @Autowired
    private MetricsService metricsService;

    @GetMapping
    public ResponseEntity<List<FreelancerResponse>> getAllFreelancers() {
        return ResponseEntity.ok(freelancerService.getAllFreelancers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FreelancerResponse> getFreelancerById(@PathVariable String id) {
        return metricsService.getFreelancerLookupTimer().record(() ->
            ResponseEntity.ok(freelancerService.getFreelancerById(id))
        );
    }

    @PostMapping
    public ResponseEntity<FreelancerResponse> createFreelancer(@Valid @RequestBody CreateFreelancerRequest request) {
        try {
            FreelancerResponse response = freelancerService.createFreelancer(request);
            metricsService.recordFreelancerCreated();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            metricsService.recordFreelancerCreationFailed();
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<FreelancerResponse> updateFreelancer(
            @PathVariable String id,
            @Valid @RequestBody UpdateFreelancerRequest request) {
        return ResponseEntity.ok(freelancerService.updateFreelancer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFreelancer(@PathVariable String id) {
        freelancerService.deleteFreelancer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ratings")
    public ResponseEntity<FreelancerResponse> addRating(
            @PathVariable String id,
            @Valid @RequestBody RateFreelancerRequest request) {
        
        FreelancerResponse response = freelancerService.addRating(id, request.rating());
        
        // Record the new rating metrics
        metricsService.recordFreelancerRating(request.rating());
        
        return ResponseEntity.ok(response);
    }
}
