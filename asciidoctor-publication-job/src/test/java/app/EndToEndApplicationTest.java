package app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.SpringApplication;

@EnabledIfEnvironmentVariable(named = "GIT_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GIT_PASSWORD", matches = ".+")
class EndToEndApplicationTest {

	@Test
	void runAppEndToEnd() {
		SpringApplication.run(AsciidoctorPublicationJobApplication.class);
	}

}
