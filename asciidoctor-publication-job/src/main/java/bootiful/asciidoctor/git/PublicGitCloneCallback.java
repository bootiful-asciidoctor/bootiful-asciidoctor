package bootiful.asciidoctor.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.net.URI;

public class PublicGitCloneCallback implements GitCloneCallback {

	@Override
	public void clone(URI uri, File dir) throws GitAPIException {
		Git.cloneRepository().setURI(uri.toASCIIString()).setDirectory(dir).call();
	}

}
