package ro.unibuc.prodeng.response;

public record BidResponse(
    String id,
    String projectId,
    String projectTitle,
    String freelancerId,
    String freelancerName,
    double amount,
    String status
) {}
