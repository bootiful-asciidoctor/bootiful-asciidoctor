package bootiful.asciidoctor.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;

import java.io.File;
import java.net.URI;

@RequiredArgsConstructor
public class CredentialsProviderGitCloneCallback implements GitCloneCallback {

	private final CredentialsProvider credentialsProvider;

	@Override
	public Git clone(URI uri, File dir) throws GitAPIException {
		return Git.cloneRepository()//
				.setCredentialsProvider(this.credentialsProvider)//
				.setURI(uri.toASCIIString()) //
				.setDirectory(dir).call();
	}

}
