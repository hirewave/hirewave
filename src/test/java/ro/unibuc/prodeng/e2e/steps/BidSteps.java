package ro.unibuc.prodeng.e2e.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class BidSteps {

    private static final String BASE_URL = "http://localhost:8080";

    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResponseEntity<String> latestResponse;

    private String clientId;
    private String projectId;
    private String freelancerId;
    private String secondFreelancerId;
    private String latestBidId;
    private String firstBidId;
    private String secondBidId;

    public BidSteps() {
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                // no-op, we want to assert non-2xx responses in scenarios
            }
        });
    }

    @Given("an available bid setup with project skills {word}")
    public void anAvailableBidSetupWithProjectSkills(String skill) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        clientId = createClient("Client-" + suffix, "client-" + suffix + "@mail.test");
        freelancerId = createFreelancer("Freelancer-" + suffix, "freelancer-" + suffix + "@mail.test", List.of("Java", "Spring"), 50.0);
        projectId = createProject("Project-" + suffix, clientId, List.of(skill), 1000.0);
    }

    @Given("a project with two freelancers and skill {word}")
    public void aProjectWithTwoFreelancersAndSkill(String skill) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        clientId = createClient("Client2-" + suffix, "client2-" + suffix + "@mail.test");
        freelancerId = createFreelancer("FreelancerA-" + suffix, "freelancerA-" + suffix + "@mail.test", List.of("Java", "Spring"), 50.0);
        secondFreelancerId = createFreelancer("FreelancerB-" + suffix, "freelancerB-" + suffix + "@mail.test", List.of("Java", "Kotlin"), 55.0);
        projectId = createProject("Project2-" + suffix, clientId, List.of(skill), 900.0);
    }

    @When("the bidder submits a bid amount {int}")
    public void theBidderSubmitsABidAmount(int amount) {
        latestResponse = postBid(projectId, freelancerId, (double) amount, "bid message");
        latestBidId = extractIdOrNull(latestResponse);
    }

    @When("both freelancers submit bids amount {int} and {int}")
    public void bothFreelancersSubmitBidsAmountAnd(int firstAmount, int secondAmount) {
        ResponseEntity<String> firstResponse = postBid(projectId, freelancerId, (double) firstAmount, "first bid");
        firstBidId = extractIdOrNull(firstResponse);

        ResponseEntity<String> secondResponse = postBid(projectId, secondFreelancerId, (double) secondAmount, "second bid");
        secondBidId = extractIdOrNull(secondResponse);

        latestResponse = secondResponse;
        latestBidId = secondBidId;
    }

    @When("the client accepts the latest bid")
    public void theClientAcceptsTheLatestBid() {
        latestResponse = exchangeNoBody("/api/bids/" + latestBidId + "/accept", HttpMethod.PATCH);
    }

    @When("the client rejects the latest bid")
    public void theClientRejectsTheLatestBid() {
        latestResponse = exchangeNoBody("/api/bids/" + latestBidId + "/reject", HttpMethod.PATCH);
    }

    @When("the client accepts the first submitted bid")
    public void theClientAcceptsTheFirstSubmittedBid() {
        latestResponse = exchangeNoBody("/api/bids/" + firstBidId + "/accept", HttpMethod.PATCH);
    }

    @When("the client fetches the latest bid")
    public void theClientFetchesTheLatestBid() {
        latestResponse = restTemplate.getForEntity(BASE_URL + "/api/bids/" + latestBidId, String.class);
    }

    @When("the client fetches bid id {word}")
    public void theClientFetchesBidId(String bidId) {
        latestResponse = restTemplate.getForEntity(BASE_URL + "/api/bids/" + bidId, String.class);
    }

    @When("listing bids for project id {word}")
    public void listingBidsForProjectId(String targetProjectId) {
        latestResponse = restTemplate.getForEntity(BASE_URL + "/api/bids?projectId=" + targetProjectId, String.class);
    }

    @Then("the bids api returns status {int}")
    public void theBidsApiReturnsStatus(int status) {
        assertThat(latestResponse.getStatusCode().value()).isEqualTo(status);
    }

    @Then("the latest bid status is {word}")
    public void theLatestBidStatusIs(String expectedStatus) throws Exception {
        JsonNode node = objectMapper.readTree(latestResponse.getBody());
        assertThat(node.get("status").asText()).isEqualTo(expectedStatus);
    }

    @Then("listing bids for current project returns {int} entries")
    public void listingBidsForCurrentProjectReturnsEntries(int count) throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL + "/api/bids?projectId=" + projectId, String.class);
        JsonNode array = objectMapper.readTree(response.getBody());
        assertThat(array.size()).isEqualTo(count);
        latestResponse = response;
    }

    @Then("the first submitted bid status is {word}")
    public void theFirstSubmittedBidStatusIs(String expectedStatus) throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL + "/api/bids/" + firstBidId, String.class);
        JsonNode node = objectMapper.readTree(response.getBody());
        assertThat(node.get("status").asText()).isEqualTo(expectedStatus);
    }

    @Then("the second submitted bid status is {word}")
    public void theSecondSubmittedBidStatusIs(String expectedStatus) throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL + "/api/bids/" + secondBidId, String.class);
        JsonNode node = objectMapper.readTree(response.getBody());
        assertThat(node.get("status").asText()).isEqualTo(expectedStatus);
    }

    @When("the client cancels the current project")
    public void theClientCancelsTheCurrentProject() {
        exchangeNoBody("/api/projects/" + projectId + "/cancel", HttpMethod.PATCH);
    }

    private String createClient(String name, String email) throws Exception {
        ResponseEntity<String> response = postJson("/api/clients", Map.of(
                "name", name,
                "email", email
        ));
        return extractId(response);
    }

    private String createFreelancer(String name, String email, List<String> skills, double hourlyRate) throws Exception {
        ResponseEntity<String> response = postJson("/api/freelancers", Map.of(
                "name", name,
                "email", email,
                "skills", skills,
                "hourlyRate", hourlyRate
        ));
        return extractId(response);
    }

    private String createProject(String title, String targetClientId, List<String> skills, double budget) throws Exception {
        ResponseEntity<String> response = postJson("/api/projects", Map.of(
                "title", title,
                "description", "E2E project",
                "budget", budget,
                "clientId", targetClientId,
                "requiredSkills", skills
        ));
        return extractId(response);
    }

    private ResponseEntity<String> postBid(String targetProjectId, String targetFreelancerId, double amount, String message) {
        return postJson("/api/bids", Map.of(
                "projectId", targetProjectId,
                "freelancerId", targetFreelancerId,
                "amount", amount,
                "message", message
        ));
    }

    private ResponseEntity<String> postJson(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(BASE_URL + path, entity, String.class);
    }

    private ResponseEntity<String> exchangeNoBody(String path, HttpMethod method) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(BASE_URL + path, method, entity, String.class);
    }

    private String extractId(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody()).get("id").asText();
    }

    private String extractIdOrNull(ResponseEntity<String> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            return null;
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode idNode = jsonNode.get("id");
            return idNode == null ? null : idNode.asText();
        } catch (Exception ex) {
            return null;
        }
    }
}