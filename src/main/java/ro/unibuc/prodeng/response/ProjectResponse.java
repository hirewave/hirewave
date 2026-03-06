package ro.unibuc.prodeng.response;

import java.util.List;

public record ProjectResponse(
    String id,
    String title,
    String description,
    List<String> requiredSkills,
    double budget,
    String status,
    String clientId,
    String clientName,
    String awardedFreelancerId,
    String awardedFreelancerName
) {}
