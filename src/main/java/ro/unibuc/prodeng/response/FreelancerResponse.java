package ro.unibuc.prodeng.response;

import java.util.List;

public record FreelancerResponse(
    String id,
    String name,
    String email,
    List<String> skills,
    double hourlyRate,
    double averageRating,
    int totalRatings
) {}
