package ro.unibuc.prodeng.response;

import ro.unibuc.prodeng.model.ProjectStatus;

import java.util.List;

public record ProjectResponse(
    String id,
    String title,
    String description,
    String clientId,
    List<String> requiredSkills,
    double budget,
    ProjectStatus status,
    String awardedFreelancerId
) {}
