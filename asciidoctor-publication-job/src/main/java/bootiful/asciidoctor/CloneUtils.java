package bootiful.asciidoctor;

import bootiful.asciidoctor.files.FileUtils;
import bootiful.asciidoctor.git.GitCloneCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;

abstract class CloneUtils {

	static File doClone(GitCloneCallback cloneCallback, URI uri, File output) {

		try {
			FileUtils.resetOrRecreateDirectory(output);
			cloneCallback.clone(uri, output);
			LoggerFactory.getLogger(CloneUtils.class).info("cloned {} to {}.", uri, output.getAbsolutePath());
		}
		catch (GitAPIException e) {
			throw new RuntimeException(e);
		}
		return output;
	}

}
