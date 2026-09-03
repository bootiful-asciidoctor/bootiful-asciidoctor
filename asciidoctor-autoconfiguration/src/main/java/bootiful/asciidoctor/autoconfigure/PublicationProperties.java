package bootiful.asciidoctor.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

@ConfigurationProperties("publication")
public record PublicationProperties(File root, File target, File code, String bookName, Runner runner, Html html,
		Pdf pdf, Epub epub, Mobi mobi, Markdown markdown) {

	public record Runner(boolean enabled) {
	}

	public record Html(boolean enabled) {
	}

	public record Pdf(boolean enabled, String isbn, File fonts, File styles, Prepress prepress, Screen screen) {
		public record Prepress(boolean enabled, boolean optimize) {
			public AbstractPdfProducer.PdfOptimizerQuality quality() {
				return AbstractPdfProducer.PdfOptimizerQuality.PREPRESS;
			}
		}

		public record Screen(boolean enabled, boolean optimize) {
			public AbstractPdfProducer.PdfOptimizerQuality quality() {
				return AbstractPdfProducer.PdfOptimizerQuality.SCREEN;
			}
		}
	}

	public record Epub(boolean enabled, String isbn) {
	}

	public record Mobi(boolean enabled, String isbn, Kindlegen kindlegen) {
		public record Kindlegen(File binaryLocation) {
		}
	}

	/**
	 * one Markdown file per chapter. {@code pandoc} is the binary itself - if it's null
	 * we look at {@code $PANDOC} and then the {@code $PATH} - and {@code flavor} is
	 * whatever {@code pandoc --to} accepts, e.g. {@code gfm} (the default),
	 * {@code commonmark} or {@code markdown_strict}. A chapter whose markup doesn't
	 * survive the trip through XML fails the whole run unless
	 * {@code ignoreBrokenChapters} says to publish the rest without it.
	 */
	public record Markdown(boolean enabled, File pandoc, String flavor, boolean ignoreBrokenChapters) {
	}
}
