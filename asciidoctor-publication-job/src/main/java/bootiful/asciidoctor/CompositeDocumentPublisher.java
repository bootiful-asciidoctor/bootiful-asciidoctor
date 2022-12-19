package bootiful.asciidoctor;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Collection;
import java.util.Map;

@Slf4j
class CompositeDocumentPublisher implements DocumentPublisher {

	private final DocumentPublisher[] publishers;

	private final DocumentPublisher noOpDefault = (files) -> log
			.info("No " + DocumentPublisher.class.getSimpleName() + " configured to publish " + files);

	private final DocumentPublisher[] defaults = new DocumentPublisher[] { this.noOpDefault };

	CompositeDocumentPublisher(DocumentPublisher[] p) {
		this.publishers = p == null || p.length == 0 ? this.defaults : p;
		if (log.isDebugEnabled()) {
			log.debug("there are " + this.publishers.length + " " + DocumentPublisher.class.getName()
					+ " instances configured");
			for (var publisher : this.publishers) {
				log.debug("\t " + publisher.getClass().getName());
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
