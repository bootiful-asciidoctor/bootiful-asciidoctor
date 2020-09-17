package bootiful.asciidoctor.publishers;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class AwsS3DocumentPublisherTest {

	@Test
	void publishToGitRepository() throws Exception {

		var awsAccessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
		var region = System.getenv("AWS_REGION");
		var secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
		var aws = buildAmazonS3Instance(awsAccessKeyId, secretAccessKey, region);
		var dp = new AwsS3DocumentPublisher(aws, "bootiful-asciidoctor");
		var epub = fileFromClassPathPath("files/epub/index.epub");
		var html = fileFromClassPathPath("files/html/index.html");
		var map = Map.of("epub", (Collection<File>) List.of(epub), "html", (Collection<File>) List.of(html));
		dp.publish(map);

	}

	@SneakyThrows
	private File fileFromClassPathPath(String path) {
		return new ClassPathResource(path).getFile();
	}

	private AmazonS3 buildAmazonS3Instance(String accessKey, String secret, String region) {
		var credentials = new BasicAWSCredentials(accessKey, secret);
		var timeout = 5 * 60 * 1000;
		var clientConfiguration = new ClientConfiguration().withClientExecutionTimeout(timeout) //
				.withConnectionMaxIdleMillis(timeout) //
				.withConnectionTimeout(timeout) //
				.withConnectionTTL(timeout)//
				.withRequestTimeout(timeout);
		return AmazonS3ClientBuilder.standard() //
				.withClientConfiguration(clientConfiguration) //
				.withCredentials(new AWSStaticCredentialsProvider(credentials)) //
				.withRegion(Regions.fromName(region))//
				.build();
	}

}