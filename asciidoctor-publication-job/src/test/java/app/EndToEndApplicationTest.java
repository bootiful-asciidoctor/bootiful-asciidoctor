package app;

import bootiful.asciidoctor.PipelineJobProperties;
import bootiful.asciidoctor.autoconfigure.DocumentProducer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@Slf4j
@ActiveProfiles("git")
@SpringBootTest(classes = AsciidoctorPublicationJobApplication.class)
public class EndToEndApplicationTest {

	@Autowired
	private PipelineJobProperties pipelineJobProperties;

	@Autowired
	private ObjectProvider<DocumentProducer> producers;

	@BeforeEach
	public void before() {
		// todo sweep through the configured Amazon S3 bucket and delete everything to
		// periodically purge old files
	}

	@Test
	public void runAppEndToEnd() {
		var countOfProducers = this.producers.stream().count();
		log.info("running the application end to end...");
		var target = this.pipelineJobProperties.getTarget();
		assertTrue("the target directory " + target.getAbsolutePath() + " already exists.", target.exists());
		var foldersLength = Objects.requireNonNull(target.listFiles(File::isDirectory)).length;
		assertTrue("there are 4 or more " + DocumentProducer.class.getName() + " instances.", foldersLength >= 4);
		assertEquals(countOfProducers, foldersLength);
	}

}
