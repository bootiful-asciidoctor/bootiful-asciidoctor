package bootiful.asciidoctor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Map;

class CompositeDocumentPublisher implements DocumentPublisher {

	private static final Logger log = LoggerFactory.getLogger(CompositeDocumentPublisher.class);

	private final DocumentPublisher[] publishers;

	private final DocumentPublisher noOpDefault = (files) -> log.info("No {} configured to publish {}",
			DocumentPublisher.class.getSimpleName(), files);

	private final DocumentPublisher[] defaults = new DocumentPublisher[] { this.noOpDefault };

	CompositeDocumentPublisher(DocumentPublisher[] p) {
		this.publishers = p == null || p.length == 0 ? this.defaults : p;
		if (log.isDebugEnabled()) {
			log.debug("there are {} {} instances configured", this.publishers.length,
					DocumentPublisher.class.getName());
			for (var publisher : this.publishers) {
				log.debug("\t {}", publisher.getClass().getName());
			}
		}
	}

	@Override
	public void publish(Map<String, Collection<File>> files) throws Exception {
		for (var documentPublisher : this.publishers) {
			documentPublisher.publish(files);
		}
	}

}
