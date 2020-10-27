package bootiful.asciidoctor.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.net.URI;

public interface GitCloneCallback {

	Git clone(URI uri, File dir) throws GitAPIException;

}
