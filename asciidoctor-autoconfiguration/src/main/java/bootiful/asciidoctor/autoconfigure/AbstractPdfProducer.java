package bootiful.asciidoctor.autoconfigure;

import org.asciidoctor.Asciidoctor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;

abstract class AbstractPdfProducer implements DocumentProducer {

	private static final Logger log = LoggerFactory.getLogger(AbstractPdfProducer.class);

	private final PublicationProperties properties;

	private final Asciidoctor asciidoctor;

	// https://asciidoctor.org/docs/asciidoctor-pdf/#optimizing-the-generated-pdf

	protected AbstractPdfProducer(PublicationProperties properties, Asciidoctor asciidoctor) {
		this.properties = properties;
		this.asciidoctor = asciidoctor;
	}

	/**
	 * contains the subclass specific customizations of how the
	 * {@link AbstractPdfProducer} should do its work.
	 */
	protected abstract PdfProducerConfiguration getPdfProducerConfiguration();

	@Override
	public File[] produce() {
		var producerConfiguration = this.getPdfProducerConfiguration();
		var optimize = producerConfiguration.optimize();
		var media = producerConfiguration.media();
		var files = new ArrayList<File>();
		var regularFile = this.producePdf(media,
				new File(this.properties.root(), "index-" + media.toLowerCase() + ".pdf"));
		files.add(regularFile);
		return files.toArray(new File[0]);
	}

	private File producePdf(String media, File file) {
		var bookName = this.properties.bookName();
		var indexAdoc = getIndexAdoc(this.properties.root());
		var pdf = this.properties.pdf();
		var attributesBuilder = this.buildCommonAttributes(bookName, pdf.isbn(), indexAdoc)
				.attribute("idseparator", "-") //
				.imagesDir("images") //
				.attribute("media", media) //
				.attribute("code", this.properties.code().getAbsolutePath()) //
				.attribute("icons", "font") //
				.attribute("pdf-style", media) //
				.attribute("idprefix") //
				.attribute("project-version", "2.0.0-SNAPSHOT") //
				.attribute("subject", bookName) //
				.attribute("project-name", bookName) //
				.attribute("pdfmarks")//
				.attribute("notitle")//
				.attribute("pdf-stylesdir", pdf.styles().getAbsolutePath()) //
				.attribute("pdf-fontsdir", pdf.fonts().getAbsolutePath());

		if (this.getPdfProducerConfiguration().optimize()) {
			var pdfOptimizerQuality = this.getPdfProducerConfiguration().pdfOptimizerQuality();
			log.debug("optimize value = {}", pdfOptimizerQuality.toString().toLowerCase());
			attributesBuilder.attribute("optimize", pdfOptimizerQuality.toString().toLowerCase());
		}

		var optionsBuilder = this.buildCommonOptions("pdf", attributesBuilder.build()).docType("book").toFile(file);
		try {
			asciidoctor.convertFile(this.getIndexAdoc(this.properties.root()), optionsBuilder.build());
		}
		catch (RuntimeException exception) {
			Assert.state(false, exception.getMessage());
		}
		log.info("inside {} & just created {}.", this.getClass().getName(), file.getAbsolutePath());
		return file;
	}

	public enum PdfOptimizerQuality {

		STANDARD, SCREEN, EBOOK, PRINTER, PREPRESS

	}

	public record PdfProducerConfiguration(PdfOptimizerQuality pdfOptimizerQuality, boolean optimize, String media) {
	}

}
