package bootiful.asciidoctor.publishers;

import bootiful.asciidoctor.git.GitPushCallback;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(PipelineJobPublishersProperties.class)
class DocumentPublisherAutoConfiguration {

	private final PipelineJobPublishersProperties properties;

	// @Bean
	GithubReleaseDocumentPublisher githubReleaseDocumentPublisher() {
		return new GithubReleaseDocumentPublisher();
	}

	// @Bean
	SendgridEmailDocumentPublisher sendgridEmailDocumentPublisher() {
		return new SendgridEmailDocumentPublisher();
	}

	@Bean
	@ConditionalOnProperty(value = "pipeline.job.publishers.git.enabled", havingValue = "true")
	GitBranchDocumentPublisher gitBranchDocumentPublisher(GitPushCallback gitPushCallback) {
		var repo = this.properties.getGit().getRepository();
		var branch = this.properties.getGit().getArtifactBranch();
		return new GitBranchDocumentPublisher(repo, branch, gitPushCallback);
	}

	@Configuration
	@RequiredArgsConstructor
	@ConditionalOnClass(AmazonS3.class)
	@ConditionalOnProperty(value = "pipeline.job.publishers.s3.enabled", havingValue = "true")
	public static class AmazonS3Configuration {

		private final PipelineJobPublishersProperties properties;

		@Bean
		AwsS3DocumentPublisher awsS3DocumentPublisher(AmazonS3 s3) {
			return new AwsS3DocumentPublisher(s3, this.properties.getS3().getBucketName());
		}

		@Bean
		@ConditionalOnMissingBean
		AmazonS3 amazonS3() {
			var s3 = this.properties.getS3();
			var accessKey = s3.getAccessKeyId();
			var secret = s3.getSecretAccessKey();
			var region = s3.getRegion();
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
