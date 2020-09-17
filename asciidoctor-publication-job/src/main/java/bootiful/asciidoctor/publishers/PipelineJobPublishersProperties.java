package bootiful.asciidoctor.publishers;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@Data
@ConfigurationProperties("pipeline.job.publishers")
public class PipelineJobPublishersProperties {

	private final Git git = new Git();

	private final S3 s3 = new S3();

	@Data
	public static class S3 {

		private boolean enabled;

		private String accessKeyId;

		private String secretAccessKey;

		private String region;

		private String bucketName;

	}

	@Data
	public static class Git {

		private boolean enabled = false;

		private URI repository;

		private String artifactBranch;

	}

}
