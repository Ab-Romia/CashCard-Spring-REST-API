package example.cashcard;


import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;


import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class CashcardApplicationTests {
	@Autowired
	TestRestTemplate restTemplate;

	@Test
	void shouldReturnACashCardWhenDataIsSaved(){
		ResponseEntity<String> response =
				restTemplate
						.withBasicAuth("sarah1","abc123")
						.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isNotNull();
	}
	@Test
	void shouldNotReturnACashCardWithUnknownID(){
		ResponseEntity<String> response = restTemplate
						.withBasicAuth("sarah1","abc123")
						.getForEntity("/cashcards/1000",String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

	}
	@Test
	@DirtiesContext
	void shouldCreatANewCard(){
		CashCard cashCard = new CashCard(null, 100.00, null);
		ResponseEntity<Void> response =
				restTemplate
						.withBasicAuth("sarah1","abc123")
						.postForEntity("/cashcards", cashCard, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI locationOfNewCashCard = response.getHeaders().getLocation();
		ResponseEntity<String> responseForNewCashCard =
				restTemplate
						.withBasicAuth("sarah1","abc123")
						.getForEntity(locationOfNewCashCard, String.class);
		assertThat(responseForNewCashCard.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(responseForNewCashCard.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");

		assertThat(id).isNotNull();
		assertThat(amount).isEqualTo(100.00);

	}

	@Test
	void shouldReturnAllCashCards(){
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1","abc123")
				.getForEntity("/cashcards", String.class);
		System.out.println(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void shouldReturnAllCashCardsWhenListIsRequested(){
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1","abc123")
				.getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Integer cashCardCount = documentContext.read("$.length()");
		assertThat(cashCardCount).isEqualTo(3);

		JSONArray ids = documentContext.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99,100,101);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(123.45,1.00,150.00);	}

	@Test
	void shouldReturnAPageOfCashCards(){
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1","abc123")
				.getForEntity("/cashcards?page=0&size=1&sort=amount,desc", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		System.out.println(page.toString());
		assertThat(page.size()).isEqualTo(1);

		Double amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(150);
	}

	@Test
	void shouldReturnUnauthorizedWhenIncorrectAuth(){
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("abdo","abc123")
				.getForEntity("/cashcards/100",String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		ResponseEntity<String> response1 = restTemplate
				.withBasicAuth("abdo","rrr")
				.getForEntity("/cashcards/100",String.class);
		assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldRejectAnNonCardOwnerUser(){
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("mo1","asdf123")
				.getForEntity("/cashcards/100",String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldNotAllowAccessToCashCardTheyDontOwn(){
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1","abc123")
				.getForEntity("/cashcards/102",String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	void shouldUpdateAnExistingCashCard(){
		CashCard cashCardUpdate = new CashCard(null, 19.99,null);
		HttpEntity<CashCard> request = new HttpEntity<>(cashCardUpdate);
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("sarah1","abc123")
				.exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("sarah1","abc123")
				.getForEntity("/cashcards/99",String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		DocumentContext	documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");
		assertThat(id).isEqualTo(99);
		assertThat(amount).isEqualTo(19.99);
	}

	@Test
	void tryingToUpdateCashCardThatDoesntExistOrNotAuthorized(){
		CashCard cashCard = new CashCard(null, 90.00, null);
		HttpEntity<CashCard> request = new HttpEntity<>(cashCard);
		ResponseEntity<Void> responseNotInDB = restTemplate
				.withBasicAuth("sarah1","abc123")
				.exchange("/cashcards/9999", HttpMethod.PUT, request, Void.class);
		assertThat(responseNotInDB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		ResponseEntity<Void> responseNotAuth = restTemplate
				.withBasicAuth("sarah1","abc123")
				.exchange("/cashcards/102",HttpMethod.PUT, request, Void.class);
		assertThat(responseNotAuth.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	void deletingAnExistingCashCard(){
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/99",HttpMethod.DELETE,null,Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("sarah1","abc123")
				.getForEntity("/cashcards/99",String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

	}

	@Test
	@DirtiesContext
	void deletingANonExistingOrNonAuthorizedCashCard(){
		ResponseEntity<Void> responseNonExisting = restTemplate
				.withBasicAuth("sarah1","abc123")
				.exchange("/cashcards/9999",HttpMethod.DELETE,null, Void.class);
		assertThat(responseNonExisting.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		ResponseEntity<Void> responseNotAuth = restTemplate
				.withBasicAuth("sarah1","abc123")
				.exchange("/cashcards/102",HttpMethod.DELETE,null,Void.class);
		assertThat(responseNotAuth.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}



}
