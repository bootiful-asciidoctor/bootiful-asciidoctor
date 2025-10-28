package bootiful.asciidoctor.publishers;

import bootiful.asciidoctor.git.CredentialsProviderGitCloneCallback;
import bootiful.asciidoctor.git.CredentialsProviderGitPushCallback;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Josh Long
 * @author Trisha Gee
 */
class GitBranchDocumentPublisherTest {

	private final Logger logger = LoggerFactory.getLogger(getClass().getName());

	private File fileFromClassPathPath(String path) {
		try {
			return new ClassPathResource(path).getFile();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void publishToGitRepository() throws Exception {
		var repositoryUri = System.getenv("GIT_REPOSITORY_URI");
		var artifactBranch = System.getenv("GIT_ARTIFACT_BRANCH");
		var gitHttpUsername = System.getenv("GIT_USERNAME");
		var gitHttpPassword = System.getenv("GIT_PASSWORD");
		var repository = URI.create(repositoryUri);

		this.logger.debug("{}:{}", gitHttpUsername, gitHttpPassword);

		var credentialsProvider = new UsernamePasswordCredentialsProvider(gitHttpUsername, gitHttpPassword);
		var httpAuthGitClone = new CredentialsProviderGitCloneCallback(credentialsProvider);
		var httpAuthGitPush = new CredentialsProviderGitPushCallback(credentialsProvider);
		var dp = new GitBranchDocumentPublisher(repository, artifactBranch, httpAuthGitPush, httpAuthGitClone);
		var epub = fileFromClassPathPath("files/epub/index.epub");
		var html = fileFromClassPathPath("files/html/index.html");
		var map = Map.of("epub", List.of(epub), "html", (Collection<File>) List.of(html));
		dp.publish(map);

	}

}
