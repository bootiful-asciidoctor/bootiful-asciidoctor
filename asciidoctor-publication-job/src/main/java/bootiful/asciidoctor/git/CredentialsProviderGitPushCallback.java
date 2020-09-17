package bootiful.asciidoctor.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;

/**
 * This class is useful when doing username and password based authentication. See
 * {@link org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider} for an
 * implementation that you will need when authenticating.
 */
@RequiredArgsConstructor
public class CredentialsProviderGitPushCallback implements GitPushCallback {

	private final CredentialsProvider credentialsProvider;

	@Override
	public void push(Git git, String remote) throws GitAPIException {
		git.push().setRemote(remote).setCredentialsProvider(credentialsProvider).call();
	}

}
