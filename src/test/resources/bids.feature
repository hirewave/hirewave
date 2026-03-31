@E2E
Feature: bids management

  Scenario: submit a valid bid on an open project
    Given an available bid setup with project skills Java
    When the bidder submits a bid amount 120
    Then the bids api returns status 201
    And the latest bid status is PENDING

  Scenario: prevent duplicate bid by same freelancer on same project
    Given an available bid setup with project skills Java
    When the bidder submits a bid amount 130
    When the bidder submits a bid amount 150
    Then the bids api returns status 400

  Scenario: list bids for a project
    Given a project with two freelancers and skill Java
    When both freelancers submit bids amount 90 and 110
    Then listing bids for current project returns 2 entries

  Scenario: accept a pending bid
    Given an available bid setup with project skills Java
    When the bidder submits a bid amount 99
    When the client accepts the latest bid
    Then the bids api returns status 200
    And the latest bid status is ACCEPTED

  Scenario: accepting one bid rejects competing pending bids
    Given a project with two freelancers and skill Java
    When both freelancers submit bids amount 80 and 95
    When the client accepts the first submitted bid
    Then the first submitted bid status is ACCEPTED
    And the second submitted bid status is REJECTED

  Scenario: reject a pending bid
    Given an available bid setup with project skills Java
    When the bidder submits a bid amount 140
    When the client rejects the latest bid
    Then the bids api returns status 200
    And the latest bid status is REJECTED

  Scenario: cannot reject an accepted bid
    Given an available bid setup with project skills Java
    When the bidder submits a bid amount 111
    And the client accepts the latest bid
    When the client rejects the latest bid
    Then the bids api returns status 400

  Scenario: cannot accept a rejected bid
    Given an available bid setup with project skills Java
    When the bidder submits a bid amount 111
    And the client rejects the latest bid
    When the client accepts the latest bid
    Then the bids api returns status 400

  Scenario: cannot submit bid without matching skill
    Given an available bid setup with project skills Python
    When the bidder submits a bid amount 100
    Then the bids api returns status 400

  Scenario: cannot submit bid on cancelled project
    Given an available bid setup with project skills Java
    When the client cancels the current project
    And the bidder submits a bid amount 100
    Then the bids api returns status 400

  Scenario: cannot submit bid with invalid amount
    Given an available bid setup with project skills Java
    When the bidder submits a bid amount 0
    Then the bids api returns status 400

  Scenario: fetch bid by id
    Given an available bid setup with project skills Java
    When the bidder submits a bid amount 170
    When the client fetches the latest bid
    Then the bids api returns status 200
    And the latest bid status is PENDING

  Scenario: fetch non existing bid returns not found
    Given an available bid setup with project skills Java
    When the client fetches bid id non-existing-bid-id
    Then the bids api returns status 404

  Scenario: list bids for non existing project returns not found
    Given an available bid setup with project skills Java
    When listing bids for project id non-existing-project-id
    Then the bids api returns status 404

  Scenario: cannot accept bid after project was cancelled
    Given an available bid setup with project skills Java
    When the bidder submits a bid amount 200
    And the client cancels the current project
    When the client accepts the latest bid
    Then the bids api returns status 400