package bootiful.asciidoctor.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.net.URI;

public class TransportConfigCallbackGitCloneCallback implements GitCloneCallback {

	private final TransportConfigCallback configCallback;

	public TransportConfigCallbackGitCloneCallback(TransportConfigCallback configCallback) {
		this.configCallback = configCallback;
	}

	@Override
	public Git clone(URI uri, File dir) throws GitAPIException {
		return Git.cloneRepository()//
				.setTransportConfigCallback(configCallback)//
				.setURI(uri.toASCIIString()) //
				.setDirectory(dir).call();
	}

}
