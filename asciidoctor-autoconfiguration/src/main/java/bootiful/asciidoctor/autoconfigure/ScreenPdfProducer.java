package bootiful.asciidoctor.autoconfigure;

import org.asciidoctor.Asciidoctor;

class ScreenPdfProducer extends AbstractPdfProducer {

	private final PdfProducerConfiguration configuration;

	ScreenPdfProducer(PublicationProperties properties, Asciidoctor asciidoctor) {
		super(properties, asciidoctor);
		var pdf = properties.getPdf();
		var mediaConfig = pdf.getScreen();
		this.configuration = new PdfProducerConfiguration(mediaConfig.getQuality(), mediaConfig.isOptimize(),
				mediaConfig.getMedia());

	}

	@Override
	protected PdfProducerConfiguration getPdfProducerConfiguration() {
		return this.configuration;
	}

}
