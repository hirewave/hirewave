package ro.unibuc.prodeng.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "bids")
public record BidEntity(
    @Id String id,
    String projectId,
    String freelancerId,
    double amount,
    BidStatus status
) {}
