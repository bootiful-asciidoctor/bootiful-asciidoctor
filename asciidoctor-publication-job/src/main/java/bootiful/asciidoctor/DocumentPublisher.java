package bootiful.asciidoctor;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * Callback SPI for any code that wants to participate in the last stage of the Spring
 * Batch job.
 */
public interface DocumentPublisher {

	/**
	 * invoked with references to the produced artifacts
	 */
	void publish(Map<String, Collection<File>> files) throws Exception;

}
