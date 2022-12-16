package bootiful.asciidoctor.autoconfigure;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.Asciidoctor;

import java.io.File;
import java.util.ArrayList;

@Slf4j
abstract class AbstractPdfProducer implements DocumentProducer {

	private final PublicationProperties properties;

	private final Asciidoctor asciidoctor;

	// https://asciidoctor.org/docs/asciidoctor-pdf/#optimizing-the-generated-pdf

	public enum PdfOptimizerQuality {

		STANDARD, SCREEN, EBOOK, PRINTER, PREPRESS

	}

	@Data
	@RequiredArgsConstructor
	public static class PdfProducerConfiguration {

		private final PdfOptimizerQuality pdfOptimizerQuality;

		private final boolean optimize;

		private final String media;

	}

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
		var optimize = this.getPdfProducerConfiguration().isOptimize();
		var media = this.getPdfProducerConfiguration().getMedia();
		var files = new ArrayList<File>();
		var regularFile = this.producePdf(media,
				new File(this.properties.getRoot(), "index-" + media.toLowerCase() + ".pdf"));
		files.add(regularFile);
		return files.toArray(new File[0]);
	}

	private File producePdf(String media, File file) {
		var bookName = this.properties.getBookName();
		var indexAdoc = getIndexAdoc(this.properties.getRoot());
		var pdf = this.properties.getPdf();
		var attributesBuilder = this.buildCommonAttributes(bookName, pdf.getIsbn(), indexAdoc)
				.attribute("idseparator", "-") //
				.imagesDir("images") //
				.attribute("media", media) //
				.attribute("code", this.properties.getCode().getAbsolutePath()) //
				.attribute("icons", "font") //
				.attribute("pdf-style", media) //
				.attribute("idprefix") //
				.attribute("project-version", "2.0.0-SNAPSHOT") //
				.attribute("subject", bookName) //
				.attribute("project-name", bookName) //
				.attribute("pdfmarks")//
				.attribute("notitle")//
				.attribute("pdf-stylesdir", pdf.getStyles().getAbsolutePath()) //
				.attribute("pdf-fontsdir", pdf.getFonts().getAbsolutePath());

		if (this.getPdfProducerConfiguration().isOptimize()) {
			PdfOptimizerQuality pdfOptimizerQuality = this.getPdfProducerConfiguration().getPdfOptimizerQuality();
			log.debug("optimize value = " + pdfOptimizerQuality.toString().toLowerCase());
			attributesBuilder.attribute("optimize", pdfOptimizerQuality.toString().toLowerCase());
		}

		var optionsBuilder = this.buildCommonOptions("pdf", attributesBuilder.build()).docType("book").toFile(file);
		asciidoctor.convertFile(this.getIndexAdoc(this.properties.getRoot()), optionsBuilder.build());
		log.info("inside " + this.getClass().getName() + " & just created " + file.getAbsolutePath() + '.');
		return file;
	}

}
