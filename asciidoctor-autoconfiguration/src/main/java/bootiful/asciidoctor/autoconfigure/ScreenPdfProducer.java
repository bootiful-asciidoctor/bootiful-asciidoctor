package bootiful.asciidoctor.autoconfigure;

import org.asciidoctor.Asciidoctor;

class ScreenPdfProducer extends AbstractPdfProducer {

	private final PdfProducerConfiguration configuration;

	ScreenPdfProducer(PublicationProperties properties, Asciidoctor asciidoctor) {
		super(properties, asciidoctor);
		var pdf = properties.pdf();
		var mediaConfig = pdf.screen();
		this.configuration = new PdfProducerConfiguration(mediaConfig.quality(), mediaConfig.optimize(), "screen");

	}

	@Override
	protected PdfProducerConfiguration getPdfProducerConfiguration() {
		return this.configuration;
	}

}
