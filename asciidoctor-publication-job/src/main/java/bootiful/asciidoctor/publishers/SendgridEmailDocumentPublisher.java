package bootiful.asciidoctor.publishers;

import bootiful.asciidoctor.DocumentPublisher;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * Sends the artifacts as attachments in an email using the Sendgrid email service.
 */
@Deprecated
class SendgridEmailDocumentPublisher implements DocumentPublisher {

	@Override
	public void publish(Map<String, Collection<File>> files) throws Exception {

	}

}
