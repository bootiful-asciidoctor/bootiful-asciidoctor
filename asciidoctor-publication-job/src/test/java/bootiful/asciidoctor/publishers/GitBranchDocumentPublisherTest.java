package bootiful.asciidoctor.publishers;

import bootiful.asciidoctor.git.CredentialsProviderGitCloneCallback;
import bootiful.asciidoctor.git.CredentialsProviderGitPushCallback;
import lombok.SneakyThrows;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Josh Long
 * @author Trisha Gee
 */
class GitBranchDocumentPublisherTest {

	@SneakyThrows
	private File fileFromClassPathPath(String path) {
		return new ClassPathResource(path).getFile();
	}

	@Test
	void publishToGitRepository() throws Exception {
		var repositoryUri = System.getenv("GIT_REPOSITORY_URI");
		var artifactBranch = System.getenv("GIT_ARTIFACT_BRANCH");
		var gitHttpUsername = System.getenv("GIT_USERNAME");
		var gitHttpPassword = System.getenv("GIT_PASSWORD");
		var repository = URI.create(repositoryUri);
		var credentialsProvider = new UsernamePasswordCredentialsProvider(gitHttpUsername, gitHttpPassword);
		var httpAuthGitClone = new CredentialsProviderGitCloneCallback(credentialsProvider);
		var httpAuthGitPush = new CredentialsProviderGitPushCallback(credentialsProvider);
		var dp = new GitBranchDocumentPublisher(repository, artifactBranch, httpAuthGitPush, httpAuthGitClone);
		var epub = fileFromClassPathPath("files/epub/index.epub");
		var html = fileFromClassPathPath("files/html/index.html");
		var map = Map.of("epub", (Collection<File>) List.of(epub), "html", (Collection<File>) List.of(html));
		dp.publish(map);

	}

}