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

public class ClientSteps {
    private static final String BASE_URL = "http://localhost:8080";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResponseEntity<String> latestResponse;
    private final List<String> createdClientIds = new ArrayList<>();
    private String lastCreatedClientId;

    public ClientSteps() {
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
        for (String id : createdClientIds) {
            try {
                restTemplate.delete(BASE_URL + "/api/clients/" + id);
            } catch (Exception ignored) {}
        }
        createdClientIds.clear();
    }

    @Given("a client with name {string} and email {string} exists")
    public void aClientExists(String name, String email) throws Exception {
        createClient(name, email);
    }

    @When("the user creates a client with name {string} and email {string}")
    public void createClientWhen(String name, String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("name", name, "email", email), headers);
        latestResponse = restTemplate.postForEntity(BASE_URL + "/api/clients", entity, String.class);
        extractAndRememberId(latestResponse);
    }

    @When("the user lists all clients")
    public void listAllClients() {
        latestResponse = restTemplate.getForEntity(BASE_URL + "/api/clients", String.class);
    }

    @When("the user fetches the existing client by id")
    public void fetchClientById() {
        latestResponse = restTemplate.getForEntity(BASE_URL + "/api/clients/" + lastCreatedClientId, String.class);
    }

    @Then("the client api returns status {int}")
    public void verifyStatus(int status) {
        assertThat(latestResponse.getStatusCode().value()).isEqualTo(status);
    }

    @Then("the created client has name {string} and email {string}")
    public void verifyCreatedClient(String name, String email) throws Exception {
        JsonNode node = objectMapper.readTree(latestResponse.getBody());
        assertThat(node.get("name").asText()).isEqualTo(name);
        assertThat(node.get("email").asText()).isEqualTo(email);
    }

    @Then("the retrieved client has name {string} and email {string}")
    public void verifyRetrievedClient(String name, String email) throws Exception {
        JsonNode node = objectMapper.readTree(latestResponse.getBody());
        assertThat(node.get("name").asText()).isEqualTo(name);
        assertThat(node.get("email").asText()).isEqualTo(email);
    }

    @Then("the clients list contains at least {int} entry")
    public void theClientsListContainsCount(int count) throws Exception {
        JsonNode array = objectMapper.readTree(latestResponse.getBody());
        assertThat(array.size()).isGreaterThanOrEqualTo(count);
    }

    private void createClient(String name, String email) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("name", name, "email", email), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/api/clients", entity, String.class);
        extractAndRememberId(response);
    }

    private void extractAndRememberId(ResponseEntity<String> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                JsonNode node = objectMapper.readTree(response.getBody());
                if (node.has("id")) {
                    String id = node.get("id").asText();
                    createdClientIds.add(id);
                    lastCreatedClientId = id;
                }
            } catch (Exception ignored) {}
        }
    }
}
