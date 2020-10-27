package bootiful.asciidoctor.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.net.URI;

@RequiredArgsConstructor
public class TransportConfigCallbackGitCloneCallback implements GitCloneCallback {

	private final TransportConfigCallback configCallback;

	@Override
	public Git clone(URI uri, File dir) throws GitAPIException {
		return Git.cloneRepository()//
				.setTransportConfigCallback(configCallback)//
				.setURI(uri.toASCIIString()) //
				.setDirectory(dir).call();
	}

}
