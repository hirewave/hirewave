package ro.unibuc.prodeng.response;

public record ClientResponse(
    String id,
    String name,
    String email,
    int completedProjects,
    int cancelledProjects,
    double reputationScore
) {}
