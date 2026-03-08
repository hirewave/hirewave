package ro.unibuc.prodeng.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "freelancers")
public record FreelancerEntity(
    @Id String id,
    String name,
    @Indexed(unique = true) String email,
    List<String> skills,
    double hourlyRate,
    double averageRating,
    int totalRatings,
    int ratingSum
) {}
