package bootiful.asciidoctor.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.SshSessionFactory;

/**
 * this is useful when you want to authenticate using SSH. See
 * {@link com.joshlong.git.GitUtils#createSshTransportConfigCallback(SshSessionFactory)}
 * or, better, {@link com.joshlong.git.GitUtils#createSshTransportConfigCallback(String)}
 * for some easy factory methods to build a bean of type {@link TransportConfigCallback}
 * which this class will need to authenticate when doing the {@code git push}
 *
 */
@RequiredArgsConstructor
public class TransportConfigCallbackGitPushCallback implements GitPushCallback {

	private final TransportConfigCallback configCallback;

	@Override
	public void push(Git git, String remote) throws GitAPIException {
		git.push().setRemote(remote).setTransportConfigCallback(configCallback).call();
	}

}
