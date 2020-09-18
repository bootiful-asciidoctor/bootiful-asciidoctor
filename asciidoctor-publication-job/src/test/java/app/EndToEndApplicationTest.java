package app;

import bootiful.asciidoctor.PipelineJobProperties;
import bootiful.asciidoctor.autoconfigure.DocumentProducer;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.Objects;

import static org.junit.Assert.assertTrue;

@Log4j2
@ActiveProfiles("git")
@SpringBootTest(classes = AsciidoctorPublicationJobApplication.class)
public class EndToEndApplicationTest {

	@Autowired
	private PipelineJobProperties pipelineJobProperties;

	@Autowired
	private ObjectProvider<DocumentProducer> producers;

	@Test
	public void runAppEndToEnd() {
		var countOfProducers = this.producers.stream().count();
		log.info("running the application end to end...");
		var target = this.pipelineJobProperties.getTarget();
		assertTrue("the target directory " + target.getAbsolutePath() + " already exists.", target.exists());
		var foldersLength = Objects.requireNonNull(target.listFiles(File::isDirectory)).length;
		assertTrue("there are 4 or more " + DocumentProducer.class.getName() + " instances.", foldersLength >= 4);
		Assert.assertEquals(countOfProducers, foldersLength);
	}

}
