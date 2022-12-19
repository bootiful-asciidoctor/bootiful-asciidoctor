package app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

@Slf4j
// @ActiveProfiles("git")
// @SpringBootTest(classes = AsciidoctorPublicationJobApplication.class)
public class EndToEndApplicationTest {

	////
	//// private final PipelineJobProperties pipelineJobProperties;
	////
	// private final ObjectProvider<DocumentProducer> producers;

	// @Autowired
	// EndToEndApplicationTest(PipelineJobProperties pipelineJobProperties,
	// ObjectProvider<DocumentProducer> producers) {
	// this.pipelineJobProperties = pipelineJobProperties;
	// this.producers = producers;
	// }

	@Test
	public void runAppEndToEnd() {
		var app = SpringApplication.run(AsciidoctorPublicationJobApplication.class);
		// var countOfProducers = this.producers.stream().count();
		// log.info("running the application end to end...");
		// var target = this.pipelineJobProperties.target();
		// assertTrue("the target directory " + target.getAbsolutePath() + " already
		// exists.", target.exists());
		// var foldersLength =
		// Objects.requireNonNull(target.listFiles(File::isDirectory)).length;
		// assertTrue("there are 4 or more " + DocumentProducer.class.getName() + "
		// instances.", foldersLength >= 4);
		// assertEquals(countOfProducers, foldersLength);
	}

}
