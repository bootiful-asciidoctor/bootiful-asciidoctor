package bootiful.asciidoctor.autoconfigure;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.io.File;

@Data
@ConfigurationProperties("publication")
public class PublicationProperties {

	private File root, target, code;

	private String bookName = "";

	private Runner runner = new Runner();

	private Epub epub = new Epub();

	private Pdf pdf = new Pdf();

	private Mobi mobi = new Mobi();

	private Html html = new Html();

	/**
	 * Should the autoconfiguration kick off and run all the {@code DocumentProducers }
	 * serially at startup? This is disabled by default.
	 */
	@Data
	public static class Runner {

		private boolean enabled = false;

	}

	@Data
	public static class Html {

		private boolean enabled;

	}

	@Data
	public static class Pdf {

		private boolean enabled;

		private String isbn;

		private File fonts, styles;

		// private Pdf.PdfMedia media = Pdf.PdfMedia.SCREEN;
		// public enum PdfMedia {
		// PREPRESS, SCREEN
		// }

		private Prepress prepress = new Prepress();

		private Screen screen = new Screen();

		@Data
		public static class Prepress {

			private AbstractPdfProducer.PdfOptimizerQuality quality = AbstractPdfProducer.PdfOptimizerQuality.PREPRESS;

			private boolean enabled;

			private boolean optimize;

			private String media = "prepress";

		}

		@Data
		public static class Screen {

			private AbstractPdfProducer.PdfOptimizerQuality quality = AbstractPdfProducer.PdfOptimizerQuality.SCREEN;

			private boolean enabled;

			private boolean optimize;

			private String media = "screen";

		}

	}

	@Data
	public static class Epub {

		private boolean enabled;

		private String isbn;

	}

	@Data
	public static class Mobi {

		private boolean enabled;

		private String isbn;

		private Kindlegen kindlegen = new Kindlegen();

		@Data
		@Log4j2
		public static class Kindlegen {

			private File binaryLocation;

			Kindlegen() {
				String kindleGenEnvVariableName = "KINDLEGEN";
				String kindleGenEnvVariableValue = System.getenv(kindleGenEnvVariableName);
				Assert.hasText(kindleGenEnvVariableValue, "$" + kindleGenEnvVariableName + " must not be null");
				this.binaryLocation = new File(kindleGenEnvVariableValue);
			}

		}

	}

}
