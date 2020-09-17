package bootiful.asciidoctor.publishers;

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

class GitBranchDocumentPublisherTest {

	@SneakyThrows
	private File fileFromClassPathPath(String path) {
		return new ClassPathResource(path).getFile();
	}

	@Test
	void publishToGitRepository() throws Exception {
		var repositoryUri = System.getenv("GIT_REPOSITORY_URI");
		var artifactBranch = System.getenv("GIT_ARTIFACT_BRANCH");
		var gitHttpUsername = System.getenv("GIT_USERNAME"); // this could be your github
		var gitHttpPassword = System.getenv("GIT_PASSWORD"); // this could be your github
		var repository = URI.create(repositoryUri);
		var httpAuth = new CredentialsProviderGitPushCallback(
				new UsernamePasswordCredentialsProvider(gitHttpUsername, gitHttpPassword));
		var dp = new GitBranchDocumentPublisher(repository, artifactBranch, httpAuth);
		var epub = fileFromClassPathPath("files/epub/index.epub");
		var html = fileFromClassPathPath("files/html/index.html");
		var map = Map.of("epub", (Collection<File>) List.of(epub), "html", (Collection<File>) List.of(html));
		dp.publish(map);

	}

}