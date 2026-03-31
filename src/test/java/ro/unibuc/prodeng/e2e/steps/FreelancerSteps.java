package ro.unibuc.prodeng.e2e.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FreelancerSteps {
    private static final String BASE_URL = "http://localhost:8080";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResponseEntity<String> latestResponse;
    private final List<String> createdFreelancerIds = new ArrayList<>();
    private String lastCreatedFreelancerId;

    public FreelancerSteps() {
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        this.restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
            }
        });
    }

    @After
    public void cleanup() {
        for (String id : createdFreelancerIds) {
            try {
                restTemplate.delete(BASE_URL + "/api/freelancers/" + id);
            } catch (Exception ignored) {}
        }
        createdFreelancerIds.clear();
    }

    @Given("a freelancer with name {string} and email {string} exists")
    public void aFreelancerExists(String name, String email) throws Exception {
        createFreelancer(name, email);
    }

    @When("the user creates a freelancer with name {string} and email {string}")
    public void createFreelancerWhen(String name, String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("name", name, "email", email, "skills", List.of("Java", "Spring"), "hourlyRate", 50.0), headers);
        latestResponse = restTemplate.postForEntity(BASE_URL + "/api/freelancers", entity, String.class);
        extractAndRememberId(latestResponse);
    }

    @When("the user lists all freelancers")
    public void listAllFreelancers() {
        latestResponse = restTemplate.getForEntity(BASE_URL + "/api/freelancers", String.class);
    }

    @When("the user fetches the existing freelancer by id")
    public void fetchFreelancerById() {
        latestResponse = restTemplate.getForEntity(BASE_URL + "/api/freelancers/" + lastCreatedFreelancerId, String.class);
    }

    @Then("the freelancer api returns status {int}")
    public void verifyStatus(int status) {
        assertThat(latestResponse.getStatusCode().value()).isEqualTo(status);
    }

    @Then("the created freelancer has name {string} and email {string}")
    public void verifyCreatedFreelancer(String name, String email) throws Exception {
        JsonNode node = objectMapper.readTree(latestResponse.getBody());
        assertThat(node.get("name").asText()).isEqualTo(name);
        assertThat(node.get("email").asText()).isEqualTo(email);
    }

    @Then("the retrieved freelancer has name {string} and email {string}")
    public void verifyRetrievedFreelancer(String name, String email) throws Exception {
        JsonNode node = objectMapper.readTree(latestResponse.getBody());
        assertThat(node.get("name").asText()).isEqualTo(name);
        assertThat(node.get("email").asText()).isEqualTo(email);
    }

    @Then("the freelancers list contains at least {int} entry")
    public void theFreelancersListContainsCount(int count) throws Exception {
        JsonNode array = objectMapper.readTree(latestResponse.getBody());
        assertThat(array.size()).isGreaterThanOrEqualTo(count);
    }

    private void createFreelancer(String name, String email) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("name", name, "email", email, "skills", List.of("Java", "Spring"), "hourlyRate", 60.0), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/api/freelancers", entity, String.class);
        extractAndRememberId(response);
    }

    private void extractAndRememberId(ResponseEntity<String> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                JsonNode node = objectMapper.readTree(response.getBody());
                if (node.has("id")) {
                    String id = node.get("id").asText();
                    createdFreelancerIds.add(id);
                    lastCreatedFreelancerId = id;
                }
            } catch (Exception ignored) {}
        }
    }
}
