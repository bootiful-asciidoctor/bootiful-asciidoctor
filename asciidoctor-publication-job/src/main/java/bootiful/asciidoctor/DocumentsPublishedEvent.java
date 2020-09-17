package bootiful.asciidoctor;

import org.springframework.context.ApplicationEvent;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * Applications may listen for this event to know when all the files have finished being
 * produced. They may use this in addition to, or in lieu of, the
 * {@link DocumentPublisher} SPI interface.
 */
public class DocumentsPublishedEvent extends ApplicationEvent {

	public DocumentsPublishedEvent(Map<String, Collection<File>> source) {
		super(source);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Collection<File>> getSource() {
		return (Map<String, Collection<File>>) super.getSource();
	}

}
