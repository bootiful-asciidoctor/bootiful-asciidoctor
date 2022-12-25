package app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

@Slf4j
public class EndToEndApplicationTest {

	@Test
	public void runAppEndToEnd() {
		SpringApplication.run(AsciidoctorPublicationJobApplication.class);
	}

}
