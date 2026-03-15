package ro.unibuc.prodeng.response;

import ro.unibuc.prodeng.model.BidStatus;

public record BidResponse(
    String id,
    String projectId,
    String freelancerId,
    double amount,
    String message,
    BidStatus status
) {}
