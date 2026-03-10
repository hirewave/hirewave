package ro.unibuc.prodeng.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "projects")
public record ProjectEntity(
    @Id String id,
    String title,
    String description,
    String clientId,
    List<String> requiredSkills,
    double budget,
    ProjectStatus status,
    String awardedFreelancerId
) {}
