package ro.unibuc.prodeng.service;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import ro.unibuc.prodeng.model.ProjectStatus;
import ro.unibuc.prodeng.repository.ProjectRepository;

@Service
public class MetricsService {

    private final MeterRegistry registry;
    private final Counter usersCreatedCounter;
    private final Counter userCreationFailedCounter;
    private final Timer userLookupTimer;
    private final AtomicInteger activeDbOperations = new AtomicInteger(0);

    public MetricsService(MeterRegistry registry, ProjectRepository projectRepository) {
        this.registry = registry;
        this.usersCreatedCounter = Counter.builder("app_users_created")
                .description("Total number of users created")
                .tag("type", "business")
                .register(registry);
        this.userCreationFailedCounter = Counter.builder("app_user_creation_failed")
                .description("Total number of failed user creation attempts")
                .tag("type", "error")
                .register(registry);
        Counter.builder("app_errors")
                .description("Total application errors by exception type")
                .tag("exception", "none")
                .register(registry);
        this.userLookupTimer = Timer.builder("app_user_lookup_duration_seconds")
                .description("Time taken to look up a user")
                .register(registry);
        Gauge.builder("app_db_connections_active", activeDbOperations, AtomicInteger::get)
                .description("Currently active database operations")
                .tag("type", "resource")
                .register(registry);
        Gauge.builder("app_open_projects", projectRepository, repository -> repository.countByStatus(ProjectStatus.OPEN))
                .description("Current number of open projects")
                .tag("type", "domain")
                .register(registry);
    }

    public void recordUserCreated() {
        usersCreatedCounter.increment();
    }

    public void recordUserCreationFailed() {
        userCreationFailedCounter.increment();
    }

    public void recordError(String exceptionType) {
        registry.counter("app_errors", "exception", exceptionType).increment();
    }

    public Timer.Sample startUserLookupTimer() {
        return Timer.start(registry);
    }

    public void stopUserLookupTimer(Timer.Sample sample) {
        sample.stop(userLookupTimer);
    }

    public void beginDbOperation() {
        activeDbOperations.incrementAndGet();
    }

    public void endDbOperation() {
        activeDbOperations.updateAndGet(current -> Math.max(0, current - 1));
    }
}
