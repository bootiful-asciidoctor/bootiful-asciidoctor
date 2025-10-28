package bootiful.asciidoctor.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;

import java.io.File;
import java.net.URI;

public class CredentialsProviderGitCloneCallback implements GitCloneCallback {

	private final CredentialsProvider credentialsProvider;

	public CredentialsProviderGitCloneCallback(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	@Override
	public Git clone(URI uri, File dir) throws GitAPIException {
		return Git.cloneRepository()//
				.setCredentialsProvider(this.credentialsProvider)//
				.setURI(uri.toASCIIString()) //
				.setDirectory(dir).call();
	}

}
