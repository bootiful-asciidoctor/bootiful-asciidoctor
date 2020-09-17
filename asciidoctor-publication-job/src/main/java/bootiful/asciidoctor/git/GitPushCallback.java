package bootiful.asciidoctor.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

public interface GitPushCallback {

	void push(Git git, String remote) throws GitAPIException;

	default void push(Git git) throws GitAPIException {
		this.push(git, "origin");
	}

}
