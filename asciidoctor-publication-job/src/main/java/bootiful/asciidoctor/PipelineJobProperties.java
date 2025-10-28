package bootiful.asciidoctor;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.io.File;

/**
 * This step clones all the git repositories to the local file system into a well-known
 * root directory. The goal here is to get all the requisite directories into the right
 * place for the Asdiidoctor pipeline
 */
@ConfigurationProperties("pipeline.job")
public record PipelineJobProperties(boolean enabled, File root, String bookName, int maxThreadsInThreadpool,
		String[] codeRepositories, String documentRepository, File target) {

	private static final Logger log = LoggerFactory.getLogger(PipelineJobProperties.class);

	@PostConstruct
	public void validate() {
		log.debug("validating {}", this.getClass().getName());
		if (!this.target.exists()) {
			this.target.mkdirs();
		}
		Assert.state(this.target.exists(),
				() -> "the output directory " + this.target.getAbsolutePath() + " does not exist.");
	}

}
