package bootiful.asciidoctor.publishers;

import bootiful.asciidoctor.DocumentPublisher;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * Publishes a new release containing the output of the publication pipeline.
 */
@Deprecated
class GithubReleaseDocumentPublisher implements DocumentPublisher {

	@Override
	public void publish(Map<String, Collection<File>> files) throws Exception {

	}

}
