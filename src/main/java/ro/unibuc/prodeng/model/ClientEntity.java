package ro.unibuc.prodeng.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "clients")
public record ClientEntity(
    @Id String id,
    String name,
    @Indexed(unique = true) String email,
    int completedProjects,
    int cancelledProjects
) {}
