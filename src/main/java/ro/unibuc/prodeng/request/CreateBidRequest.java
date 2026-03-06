package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateBidRequest(
    @NotBlank(message = "Project ID is required")
    String projectId,

    @NotBlank(message = "Freelancer ID is required")
    String freelancerId,

    @Positive(message = "Bid amount must be positive")
    double amount
) {}
