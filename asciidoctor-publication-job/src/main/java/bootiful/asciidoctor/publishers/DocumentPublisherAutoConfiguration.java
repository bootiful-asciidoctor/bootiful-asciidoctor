package bootiful.asciidoctor.publishers;

import bootiful.asciidoctor.git.GitCloneCallback;
import bootiful.asciidoctor.git.GitPushCallback;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Josh Long
 * @author Trisha Gee
 */
@AutoConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties(PipelineJobPublishersProperties.class)
class DocumentPublisherAutoConfiguration {

	private final PipelineJobPublishersProperties properties;

	@Bean
	@ConditionalOnProperty(value = "pipeline.job.publishers.git.enabled", havingValue = "true")
	GitBranchDocumentPublisher gitBranchDocumentPublisher(GitPushCallback gitPushCallback,
			GitCloneCallback gitCloneCallback) {
		var repo = this.properties.git().repository();
		var branch = this.properties.git().artifactBranch();
		return new GitBranchDocumentPublisher(repo, branch, gitPushCallback, gitCloneCallback);
	}

	@Configuration
	@RequiredArgsConstructor
	@ConditionalOnClass(AmazonS3.class)
	@ConditionalOnProperty(value = "pipeline.job.publishers.s3.enabled", havingValue = "true")
	public static class AmazonS3Configuration {

		private final PipelineJobPublishersProperties properties;

		@Bean
		AwsS3DocumentPublisher awsS3DocumentPublisher(AmazonS3 s3) {
			return new AwsS3DocumentPublisher(s3, this.properties.s3().bucketName());
		}

		@Bean
		@ConditionalOnMissingBean
		AmazonS3 amazonS3() {
			var s3 = this.properties.s3();
			var accessKey = s3.accessKeyId();
			var secret = s3.secretAccessKey();
			var region = s3.region();
			var credentials = new BasicAWSCredentials(accessKey, secret);
			var timeout = 5 * 60 * 1000;
			var clientConfiguration = new ClientConfiguration().withClientExecutionTimeout(timeout)
					.withConnectionMaxIdleMillis(timeout).withConnectionTimeout(timeout).withConnectionTTL(timeout)
					.withRequestTimeout(timeout);
			return AmazonS3ClientBuilder.standard().withClientConfiguration(clientConfiguration)
					.withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(Regions.fromName(region))
					.build();
		}

	}

}
