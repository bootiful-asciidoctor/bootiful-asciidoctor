package app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

public class EndToEndApplicationTest {

	@Test
	public void runAppEndToEnd() {
		SpringApplication.run(AsciidoctorPublicationJobApplication.class);
	}

}
