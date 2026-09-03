package bootiful.asciidoctor.autoconfigure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIf("bookIsCheckedOutLocally")
class ScreenPdfProducerTest {

	private static final Logger log = LoggerFactory.getLogger(ScreenPdfProducerTest.class);

	private static final String BOOK_NAME = "Secure All The Things (with Spring Security and OAuth)!";

	/**
	 * stands in for {@code pipeline.job.root}, the directory into which the pipeline
	 * clones everything.
	 */
	private static final File ROOT = new File(System.getProperty("book.root", System.getenv()
		.getOrDefault("BOOK_ROOT", System.getProperty("user.home") + "/code/secure-all-the-things-book")));

	/**
	 * stands in for {@code $root/docs}: the checkout of the document repository, the one
	 * holding {@code index.adoc}.
	 */
	private static final File DOCS = new File(ROOT, "book");

	static boolean bookIsCheckedOutLocally() {
		return new File(DOCS, "index.adoc").exists();
	}

	@Test
	void producesAScreenReadyPdf() throws Exception {

		var pdfStyles = pdfStylesDirectory();
		var target = new File("target/publication");
		var expected = new File(DOCS, "index-screen.pdf");
		var expectedExistedBeforehand = expected.exists();

		try {
			new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(AsciidoctorPublicationAutoConfiguration.class))
				// these are exactly the properties the pipeline's
				// ForwardingEnvironmentPostProcessor derives from pipeline.job.root
				.withPropertyValues(//
						"publication.book-name=" + BOOK_NAME, //
						"publication.root=" + DOCS.getAbsolutePath(), //
						"publication.code=" + ROOT.getAbsolutePath(), //
						"publication.target=" + target.getAbsolutePath(), //
						"publication.pdf.styles=" + pdfStyles.getAbsolutePath(), //
						"publication.pdf.fonts=" + new File(pdfStyles, "fonts").getAbsolutePath(), //
						// ...and these are the formats the book's pipeline turns on,
						// narrowed to the screen-ready PDF
						"publication.html.enabled=false", //
						"publication.epub.enabled=false", //
						"publication.mobi.enabled=false", //
						"publication.pdf.prepress.enabled=false", //
						"publication.pdf.screen.enabled=true", //
						// optimizing shells out to Ghostscript by way of
						// asciidoctor-pdf-optimize, which the pipeline only has on CI
						"publication.pdf.screen.optimize=false" //
				)//
				.run(context -> {

					assertThat(context).hasNotFailed();
					var producer = context.getBean("screenPdfProducer", DocumentProducer.class);
					assertThat(producer).isInstanceOf(ScreenPdfProducer.class);

					// step one: the DocumentProducerTasklet asks the producer to render
					var produced = producer.produce();
					assertThat(produced).as("the screen PDF producer renders a single PDF").hasSize(1);
					var pdf = produced[0];
					assertThat(pdf).isEqualTo(expected).exists();
					assertThat(isPdf(pdf)).as("%s is a PDF", pdf.getAbsolutePath()).isTrue();

					// step two: the same tasklet copies what came back into
					// $target/$producer, which is what the publishers then upload
					var published = new File(new File(target, producer.getClass().getSimpleName()), pdf.getName());
					published.getParentFile().mkdirs();
					FileCopyUtils.copy(pdf, published);
					assertThat(published).exists();
					assertThat(published.length()).isEqualTo(pdf.length());
					log.info("published a {}-byte screen-ready PDF to {}", published.length(),
							published.getAbsolutePath());
				});
		} //
		finally {
			// the producer renders in-place, into the document checkout; leave that
			// checkout as we found it now that the PDF has been copied to the target
			if (!expectedExistedBeforehand) {
				Files.deleteIfExists(expected.toPath());
			}
		}
	}

	/**
	 * the PDF theme ({@code screen-theme.yml}) and font directories. The pipeline expects
	 * them under {@code styles/pdf}, but this book keeps them in {@code pdf}.
	 */
	private static File pdfStylesDirectory() {
		var candidates = new File[] { new File(DOCS, "styles/pdf"), new File(DOCS, "pdf") };
		for (var candidate : candidates) {
			if (new File(candidate, "screen-theme.yml").exists()) {
				return candidate;
			}
		}
		throw new IllegalStateException("could not find screen-theme.yml in " + DOCS.getAbsolutePath());
	}

	private static boolean isPdf(File file) throws IOException {
		Assert.state(file.length() > 0, () -> file.getAbsolutePath() + " is empty");
		try (var raf = new RandomAccessFile(file, "r")) {
			var header = new byte[5];
			raf.readFully(header);
			return "%PDF-".equals(new String(header));
		}
	}

}
