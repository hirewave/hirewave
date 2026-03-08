package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record UpdateFreelancerRequest(
    @NotBlank(message = "Name is required")
    String name,

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    String email,

    @NotEmpty(message = "At least one skill is required")
    List<String> skills,

    @Positive(message = "Hourly rate must be positive")
    double hourlyRate
) {}
