package bootiful.asciidoctor.publishers;

import bootiful.asciidoctor.git.GitCloneCallback;
import bootiful.asciidoctor.git.GitPushCallback;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;

/**
 * @author Josh Long
 * @author Trisha Gee
 */
@AutoConfiguration
@EnableConfigurationProperties(PipelineJobPublishersProperties.class)
class DocumentPublisherAutoConfiguration {

	private final PipelineJobPublishersProperties properties;

	DocumentPublisherAutoConfiguration(PipelineJobPublishersProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnProperty(value = "pipeline.job.publishers.git.enabled", havingValue = "true")
	GitBranchDocumentPublisher gitBranchDocumentPublisher(GitPushCallback gitPushCallback,
			GitCloneCallback gitCloneCallback) {
		var repo = this.properties.git().repository();
		var branch = this.properties.git().artifactBranch();
		return new GitBranchDocumentPublisher(repo, branch, gitPushCallback, gitCloneCallback);
	}

	@Configuration
	@ConditionalOnClass(S3Client.class)
	@ConditionalOnProperty(value = "pipeline.job.publishers.s3.enabled", havingValue = "true")
	public static class AmazonS3Configuration {

		private final PipelineJobPublishersProperties properties;

		public AmazonS3Configuration(PipelineJobPublishersProperties properties) {
			this.properties = properties;
		}

		@Bean
		AwsS3DocumentPublisher awsS3DocumentPublisher(S3Client s3) {
			return new AwsS3DocumentPublisher(s3, this.properties.s3().bucketName());
		}

		@Bean
		@ConditionalOnMissingBean
		S3Client amazonS3() {
			var s3 = this.properties.s3();
			var accessKey = s3.accessKeyId();
			var secret = s3.secretAccessKey();
			var region = Region.of(s3.region());
			var credentials = AwsBasicCredentials.builder().accessKeyId(accessKey).secretAccessKey(secret).build();
			var timeout = Duration.ofMinutes(5);

			var clientConfiguration = ClientOverrideConfiguration.builder().apiCallTimeout(timeout).build();

			return S3Client.builder().credentialsProvider(StaticCredentialsProvider.create(credentials))
					.overrideConfiguration(clientConfiguration).region(region).build();
		}

	}

}
