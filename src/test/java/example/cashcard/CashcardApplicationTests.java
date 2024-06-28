package example.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.net.URI;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashcardApplicationTests {
	@Autowired
	TestRestTemplate restTemplate;

	@Test
	void shouldReturnACashCardWhenDataIsSaved(){
		ResponseEntity<String> response =
				restTemplate.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isNotNull();
	}
	@Test
	void shouldNotReturnACashCardWithUnknownID(){
		ResponseEntity<String> response =
				restTemplate.getForEntity("/cashCards/1000",String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
//		assertThat(response.getBody()).isBlank();

	}
	@Test
	void shouldCreatANewCard(){
		CashCard cashCard = new CashCard(null, 100.0);
		ResponseEntity<Void> response =
				restTemplate.postForEntity("/cashcards", cashCard, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI locationOfNewCashCard = response.getHeaders().getLocation();
		ResponseEntity<String> responseForNewCashCard =
				restTemplate.getForEntity(locationOfNewCashCard, String.class);
		assertThat(responseForNewCashCard.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(responseForNewCashCard.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");

		assertThat(id).isNotNull();
		assertThat(amount).isEqualTo(100.00);

	}

	@Test
	void shouldReturnAllCashCards(){
		ResponseEntity<String> response =
				restTemplate.getForEntity("/cashcards", String.class);
		System.out.println(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

}
