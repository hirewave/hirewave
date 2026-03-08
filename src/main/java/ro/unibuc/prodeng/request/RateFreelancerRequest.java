package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RateFreelancerRequest(
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    int rating
) {}
