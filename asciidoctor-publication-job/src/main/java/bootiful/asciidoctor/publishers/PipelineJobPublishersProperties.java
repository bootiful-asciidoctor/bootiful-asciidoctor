package bootiful.asciidoctor.publishers;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("pipeline.job.publishers")
public record PipelineJobPublishersProperties(Git git, S3 s3) {

	public record S3(boolean enabled, String accessKeyId, String secretAccessKey, String region, String bucketName) {
	}

	public record Git(boolean enabled, URI repository, String artifactBranch) {
	}

}
