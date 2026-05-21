Feature: Backstage Query Translator endpoint

  Background:
    * url baseUrl

  Scenario: single AND group returns deduplicated items
    Given path '/translator/entities'
    And param filter = 'spec.type=image,spec.appId=CLAUT'
    And param limit = 50
    When method get
    Then status 200
    And match response.degraded == false
    And match response.total == '#number'
    And match response.items == '#[_ >= 1]'
    And match each response.items contains { metadata: '#object' }

  Scenario: OR across two filters unions and dedupes by id
    Given path '/translator/entities'
    And param filter = ['spec.type=image', 'spec.available=true']
    When method get
    Then status 200
    And match response.degraded == false
    # downstream returns uids 1,2 per call; union deduped to 2
    And match response.total == 2

  Scenario: pagination is honoured after merge
    Given path '/translator/entities'
    And param filter = 'spec.type=image'
    And param limit = 1
    And param offset = 0
    When method get
    Then status 200
    And match response.items == '#[1]'
    And match response.page.limit == 1

  Scenario: malformed filter is rejected with 400
    Given path '/translator/entities'
    And param filter = 'no-operator-here'
    When method get
    Then status 400
    And match response.error == 'Bad Request'

  Scenario: advanced operators are accepted and translated
    Given path '/translator/entities'
    And param filter = 'count>=5,metadata.name=~web'
    When method get
    Then status 200
    And match response.degraded == false
