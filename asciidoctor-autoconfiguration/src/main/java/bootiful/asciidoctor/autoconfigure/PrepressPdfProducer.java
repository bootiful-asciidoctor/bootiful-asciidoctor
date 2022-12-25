package bootiful.asciidoctor.autoconfigure;

import org.asciidoctor.Asciidoctor;

class PrepressPdfProducer extends AbstractPdfProducer {

	private final PdfProducerConfiguration configuration;

	PrepressPdfProducer(PublicationProperties properties, Asciidoctor asciidoctor) {
		super(properties, asciidoctor);
		var pdf = properties.pdf();
		var mediaConfig = pdf.prepress();
		this.configuration = new PdfProducerConfiguration(mediaConfig.quality(), mediaConfig.optimize(),
				mediaConfig.media());
	}

	@Override
	protected PdfProducerConfiguration getPdfProducerConfiguration() {
		return this.configuration;
	}

}
