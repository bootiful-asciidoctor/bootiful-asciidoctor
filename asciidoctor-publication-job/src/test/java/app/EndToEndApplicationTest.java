package app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

class EndToEndApplicationTest {

	@Test
	void runAppEndToEnd() {
		SpringApplication.run(AsciidoctorPublicationJobApplication.class);
	}

}
