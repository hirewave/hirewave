package ro.unibuc.prodeng.service;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Service;

import ro.unibuc.prodeng.repository.ClientRepository;
import ro.unibuc.prodeng.repository.FreelancerRepository;

@Service
public class MetricsService {
    private final Counter clientCreatedCounter;
    private final Counter freelancerCreatedCounter;
    private final Counter clientCreationFailedCounter;
    private final Counter freelancerCreationFailedCounter;
    private final Timer clientLookupTimer;
    private final Timer freelancerLookupTimer;
    private final DistributionSummary freelancerRatingDistribution;
    private final MeterRegistry registry;

    public MetricsService(MeterRegistry registry, ClientRepository clientRepository, FreelancerRepository freelancerRepository) {
        this.registry = registry;
        this.clientCreatedCounter = Counter.builder("app_clients_created_total")
                .description("Total number of clients created")
                .tag("type", "business").register(registry);
                
        this.freelancerCreatedCounter = Counter.builder("app_freelancers_created_total")
                .description("Total number of freelancers created")
                .tag("type", "business").register(registry);

        this.clientCreationFailedCounter = Counter.builder("app_client_creation_failed_total")
                .description("Total number of failed client creation attempts")
                .tag("type", "error").register(registry);
                
        this.freelancerCreationFailedCounter = Counter.builder("app_freelancer_creation_failed_total")
                .description("Total number of failed freelancer creation attempts")
                .tag("type", "error").register(registry);

        this.clientLookupTimer = Timer.builder("app_client_lookup_duration_seconds")
                .description("Time taken to look up a client")
                .tag("type", "performance")
                .register(registry);
                
        this.freelancerLookupTimer = Timer.builder("app_freelancer_lookup_duration_seconds")
                .description("Time taken to look up a freelancer")
                .tag("type", "performance")
                .register(registry);

        this.freelancerRatingDistribution = DistributionSummary.builder("app_freelancer_rating_value_distribution")
                .description("Distribution of rating values submitted for freelancers")
                .baseUnit("stars")
                .tag("type", "domain-specific")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(registry);

        Gauge.builder("app_active_clients_total", clientRepository, repo -> repo.count())
                .description("Current number of active clients in the database")
                .tag("type", "resource")
                .register(registry);
                
        Gauge.builder("app_active_freelancers_total", freelancerRepository, repo -> repo.count())
                .description("Current number of active freelancers in the database")
                .tag("type", "domain-specific")
                .register(registry);
    }

    public void recordClientCreated() { clientCreatedCounter.increment(); }
    public void recordFreelancerCreated() { freelancerCreatedCounter.increment(); }
    
    public void recordClientCreationFailed() { clientCreationFailedCounter.increment(); }
    public void recordFreelancerCreationFailed() { freelancerCreationFailedCounter.increment(); }
    
    public Timer getClientLookupTimer() { return clientLookupTimer; }
    public Timer getFreelancerLookupTimer() { return freelancerLookupTimer; }

    public void recordFreelancerRating(int rating) {
        // Record into the DistributionSummary to calculate averages and percentiles over time
        freelancerRatingDistribution.record(rating);

        // Also record a simple Counter categorized by the number of stars given
        // This makes it easy to build pie charts in Grafana!
        Counter.builder("app_freelancer_rating_submitted_total")
                .description("Total ratings submitted by score")
                .tag("score", String.valueOf(rating))
                .tag("type", "domain-specific")
                .register(registry)
                .increment();
    }
}
