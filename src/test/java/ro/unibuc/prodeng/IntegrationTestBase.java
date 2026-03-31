package ro.unibuc.prodeng;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that need a real MongoDB database.
 * Uses Testcontainers to spin up a MongoDB instance in Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Tag("IntegrationTest")
public abstract class IntegrationTestBase {
    private static final String DEFAULT_LOCAL_MONGO_URL =
            "mongodb://root:example@localhost:27017";

    private static final MongoDBContainer mongoDBContainer =
            new MongoDBContainer("mongo:6.0.20")
                    .withExposedPorts(27017)
                    .withSharding()
                    .withLabel("ro.unibuc.prodeng", "integration-test-mongo");

    private static String resolvedMongoUrl = null;

    static {
        try {
            mongoDBContainer.start();
            resolvedMongoUrl = "mongodb://localhost:" + mongoDBContainer.getMappedPort(27017);
        } catch (Exception ex) {
            String fromEnv = System.getenv("MONGODB_CONECTION_URL");
            resolvedMongoUrl = (fromEnv == null || fromEnv.isBlank())
                    ? DEFAULT_LOCAL_MONGO_URL
                    : fromEnv;
            System.out.println("[IntegrationTestBase] Docker/Testcontainers unavailable. "
                    + "Falling back to Mongo URL: " + resolvedMongoUrl);
        }
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("mongodb.connection.url", () -> resolvedMongoUrl);
    }
}
