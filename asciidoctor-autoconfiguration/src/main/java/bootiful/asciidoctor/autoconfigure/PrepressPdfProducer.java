package bootiful.asciidoctor.autoconfigure;

import org.asciidoctor.Asciidoctor;

class PrepressPdfProducer extends AbstractPdfProducer {

	private final PdfProducerConfiguration configuration;

	PrepressPdfProducer(PublicationProperties properties, Asciidoctor asciidoctor) {
		super(properties, asciidoctor);
		var pdf = properties.getPdf();
		var mediaConfig = pdf.getPrepress();
		this.configuration = new PdfProducerConfiguration(mediaConfig.getQuality(), mediaConfig.isOptimize(),
				mediaConfig.getMedia());
	}

	@Override
	protected PdfProducerConfiguration getPdfProducerConfiguration() {
		return this.configuration;
	}

}
