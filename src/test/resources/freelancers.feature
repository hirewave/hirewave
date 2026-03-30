@E2E
Feature: freelancer management

  Scenario: create a new freelancer
    When the user creates a freelancer with name "Bob Coder" and email "bob@coder.com"
    Then the freelancer api returns status 201
    And the created freelancer has name "Bob Coder" and email "bob@coder.com"

  Scenario: list all freelancers
    Given a freelancer with name "Alice Dev" and email "alice@dev.com" exists
    When the user lists all freelancers
    Then the freelancer api returns status 200
    And the freelancers list contains at least 1 entry

  Scenario: retrieve freelancer by id
    Given a freelancer with name "Charlie Eng" and email "charlie@eng.com" exists
    When the user fetches the existing freelancer by id
    Then the freelancer api returns status 200
    And the retrieved freelancer has name "Charlie Eng" and email "charlie@eng.com"

  Scenario: create freelancer with duplicate email fails
    Given a freelancer with name "Dup Dev" and email "dupdev@test.com" exists
    When the user creates a freelancer with name "Dup Dev Two" and email "dupdev@test.com"
    Then the freelancer api returns status 400
