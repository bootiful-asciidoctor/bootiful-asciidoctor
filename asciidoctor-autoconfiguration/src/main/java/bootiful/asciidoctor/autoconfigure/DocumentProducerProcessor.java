package bootiful.asciidoctor.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;

import java.io.File;
import java.util.stream.Stream;

import static bootiful.asciidoctor.autoconfigure.FileCopyUtils.copy;

/**
 * This is only run IF it's specifically enabled. The client of this API may run the
 * {@link DocumentProducer} instances as they please.
 */
@Slf4j
@RequiredArgsConstructor
class DocumentProducerProcessor {

	private final DocumentProducer[] producers;

	private final PublicationProperties properties;

	// todo figure out if this is required
	// @EventListener(ApplicationReadyEvent.class)
	public void produceDocuments() {
		log.info("there are " + this.producers.length + " " + DocumentProducer.class.getName() + " instances");
		Stream.of(this.producers).forEach(producer -> {
			try {
				var filesArray = producer.produce();
				if (filesArray.length > 0) {
					var fileStream = Stream.of(filesArray);
					this.collectOutputFiles(producer, fileStream);
				}
			}
			catch (Exception e) {
				log.error("had trouble running " + producer.getClass().getName()
						+ " and received the following exception: ", e);
			}
		});
	}

	private void collectOutputFiles(DocumentProducer producer, Stream<File> files) {
		var name = producer.getClass().getSimpleName().toLowerCase().replace("producer", "");
		var target = new File(this.properties.target(), name);
		Assert.isTrue(target.exists() || target.mkdirs(),
				"the target directory " + target.getAbsolutePath() + " does not exist and couldn't be created");
		files.forEach(inputFile -> doCopy(inputFile, new File(target, inputFile.getName())));
	}

	private void doCopy(File in, File out) {
		Assert.isTrue(in.exists(), "The input file " + in.getAbsolutePath() + " does not exist");
		copy(in, out);
		var outAbsolutePath = out.getAbsolutePath();
		Assert.isTrue(out.exists(), "The output file, " + outAbsolutePath + ", does not exist; copy failed.");
	}

}
