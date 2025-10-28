package bootiful.asciidoctor.publishers;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class AwsS3DocumentPublisherTest {

	@Test
	void publishToGitRepository() throws Exception {

		var awsAccessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
		var region = System.getenv("AWS_REGION");
		var secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        var bucketName = System.getenv("AWS_BUCKET_NAME");
		var aws = buildAmazonS3Instance(awsAccessKeyId, secretAccessKey, region);
		var dp = new AwsS3DocumentPublisher(aws, bucketName);
		var epub = fileFromClassPathPath("files/epub/index.epub");
		var html = fileFromClassPathPath("files/html/index.html");
		var map = Map.of("epub", List.of(epub), "html", (Collection<File>) List.of(html));
		dp.publish(map);

	}

	private File fileFromClassPathPath(String path) {
		try {
			return new ClassPathResource(path).getFile();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private S3Client buildAmazonS3Instance(String accessKey, String secret, String region) {
		var credentials = AwsBasicCredentials.builder().accessKeyId(accessKey).secretAccessKey(secret).build();
		var timeout = Duration.ofMinutes(5);
		var clientConfiguration = ClientOverrideConfiguration.builder().apiCallTimeout(timeout).build();

		return S3Client.builder().overrideConfiguration(clientConfiguration)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
				.region(Region.of(region)).build();
	}

}
