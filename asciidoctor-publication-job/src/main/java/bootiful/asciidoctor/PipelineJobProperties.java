package bootiful.asciidoctor;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * This step clones all the git repositories to the local file system into a well-known
 * root directory. The goal here is to get all the requisite directories into the right
 * place for the Asdiidoctor pipeline
 */
@Data
@ConfigurationProperties("pipeline.job")
public class PipelineJobProperties {

	private boolean enabled = true;

	private File root;

	private String bookName; // an alias for `pipeline.book-name`

	private int maxThreadsInThreadpool = Runtime.getRuntime().availableProcessors();

	private String[] codeRepositories;

	// @NonNull
	private String documentRepository;

	// @NonNull
	private File target;

	@PostConstruct
	public void validate() {
		if (!this.target.exists()) {
			this.target.mkdirs();
		}
		Assert.state(this.target.exists(),
				() -> "the output directory " + this.target.getAbsolutePath() + " does not exist.");
	}

}
