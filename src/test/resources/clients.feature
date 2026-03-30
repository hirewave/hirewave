@E2E
Feature: client management

  Scenario: create a new client
    When the user creates a client with name "Acme Corp" and email "acme@test.com"
    Then the client api returns status 201
    And the created client has name "Acme Corp" and email "acme@test.com"

  Scenario: list all clients
    Given a client with name "Tech Ltd" and email "tech@test.com" exists
    When the user lists all clients
    Then the client api returns status 200
    And the clients list contains at least 1 entry

  Scenario: retrieve client by id
    Given a client with name "Global Inc" and email "global@test.com" exists
    When the user fetches the existing client by id
    Then the client api returns status 200
    And the retrieved client has name "Global Inc" and email "global@test.com"

  Scenario: create client with duplicate email fails
    Given a client with name "Dup Corp" and email "dup@test.com" exists
    When the user creates a client with name "Dup Two" and email "dup@test.com"
    Then the client api returns status 400
