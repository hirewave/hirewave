package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record UpdateProjectRequest(
    @NotBlank(message = "Title is required")
    String title,

    @NotBlank(message = "Description is required")
    String description,

    @NotEmpty(message = "At least one required skill must be specified")
    List<String> requiredSkills,

    @Positive(message = "Budget must be positive")
    double budget
) {}
