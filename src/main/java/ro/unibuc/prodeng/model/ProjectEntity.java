package ro.unibuc.prodeng.model;

import java.util.List;

public record ProjectEntity(
    String id,
    String title,
    String description,
    String clientId,
    List<String> requiredSkills,
    Double budget,
    ProjectStatus status,
    String awardedFreelancerId
) {}