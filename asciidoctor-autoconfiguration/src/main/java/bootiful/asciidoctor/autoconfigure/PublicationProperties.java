package bootiful.asciidoctor.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

@ConfigurationProperties("publication")
public record PublicationProperties(File root, File target, File code, String bookName, Runner runner, Html html,
		Pdf pdf, Epub epub, Mobi mobi) {

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
}
