package bootiful.asciidoctor.autoconfigure;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Turns each chapter of the book into its own Markdown file.
 * <p>
 * Asciidoctor loads the book exactly as the other {@link DocumentProducer producers} see
 * it - every {@code include::}, attribute and cross reference resolved - and then each
 * chapter is rendered to DocBook on its own and handed to {@code pandoc}, which does the
 * actual Markdown conversion. Going by way of DocBook rather than HTML is what keeps
 * admonitions, callouts and source listings recognizable on the other side.
 * <p>
 * You need {@code pandoc} on the {@code $PATH}, or you need to say where it lives with
 * {@code publication.markdown.pandoc} or a {@code $PANDOC} environment variable.
 */
class MarkdownProducer implements DocumentProducer {

	private static final Logger log = LoggerFactory.getLogger(MarkdownProducer.class);

	private static final String DEFAULT_FLAVOR = "gfm";

	private final PublicationProperties properties;

	private final Asciidoctor asciidoctor;

	MarkdownProducer(PublicationProperties properties, Asciidoctor asciidoctor) {
		this.properties = properties;
		this.asciidoctor = asciidoctor;
	}

	@Override
	public File[] produce() throws Exception {
		var pandoc = this.pandocBinary();
		var root = this.properties.root();
		var markdownDirectory = new File(root, "markdown");
		Assert.isTrue(markdownDirectory.exists() || markdownDirectory.mkdirs(), () -> "the directory "
				+ markdownDirectory.getAbsolutePath() + " does not exist and couldn't be created");
		var chapters = this.readChapters(root);
		log.info("found {} chapter(s) in {}", chapters.size(), this.getIndexAdoc(root).getAbsolutePath());

		var markdownFiles = new ArrayList<File>();
		var broken = new ArrayList<String>();
		for (var chapter : chapters) {
			var chapterDocbook = new File(markdownDirectory, chapter.name() + ".xml");
			var markdown = new File(markdownDirectory, chapter.name() + ".md");
			Files.writeString(chapterDocbook.toPath(), chapter.docbook(), StandardCharsets.UTF_8);
			try {
				this.pandoc(pandoc, chapter, chapterDocbook, markdown);
				log.info("wrote '{}' to {}", chapter.title(), markdown.getAbsolutePath());
				markdownFiles.add(markdown);
				chapterDocbook.delete();
			}
			catch (BrokenChapterException bce) {
				// one chapter's worth of bad markup shouldn't cost you the other thirty,
				// so keep going and account for all of them at the end. The DocBook stays
				// on disk: it's the thing you want to read to work out what happened.
				log.error("could not convert the chapter '{}': {}", chapter.title(), bce.getMessage());
				broken.add("'" + chapter.title() + "' (see " + chapterDocbook.getAbsolutePath() + ")");
			}
		}

		Assert.state(broken.isEmpty() || this.ignoreBrokenChapters(),
				() -> broken.size() + " of " + chapters.size() + " chapters could not be converted to Markdown: "
						+ String.join(", ", broken) + ". pandoc rejects DocBook that isn't well formed, and that's "
						+ "almost always overlapping inline markup in the AsciiDoc - an unbalanced ` or a * or _ "
						+ "inside a `literal` - which HTML and PDF render happily but XML can't express. Fix the "
						+ "markup, or set publication.markdown.ignore-broken-chapters=true to publish the rest "
						+ "without them.");
		if (!broken.isEmpty()) {
			log.warn("publishing {} of {} chapters; {} were left out", markdownFiles.size(), chapters.size(),
					broken.size());
		}

		var images = new File(root, "images");
		if (images.exists()) {
			markdownFiles.add(images);
		}
		return markdownFiles.toArray(new File[0]);
	}

	/**
	 * loads the book and renders each of its chapters to DocBook on its own. Splitting on
	 * the document model rather than on the rendered XML means a chapter is whatever
	 * Asciidoctor thinks a chapter is, and it means one bad chapter is just one bad
	 * chapter.
	 */
	private List<Chapter> readChapters(File root) {
		var index = this.getIndexAdoc(root);
		var attributes = this
			.buildCommonAttributes(this.properties.bookName(), "(No ISBN required)", this.properties.code())
			.attribute("idseparator", "-") //
			.attribute("icons", "font");
		var options = this.buildCommonOptions("docbook", attributes.build()).docType("book");
		var document = this.asciidoctor.loadFile(index, options.build());
		var chapters = new ArrayList<Chapter>();
		for (var block : document.getBlocks()) {
			if (block instanceof Section section && section.getLevel() == 1) {
				var title = titleOf(section.getTitle(), "chapter");
				chapters.add(new Chapter(title, "%02d-%s".formatted(chapters.size() + 1, slugify(title)),
						declare(section.convert())));
			}
		}
		if (chapters.isEmpty()) {
			// not every document is a book: one without chapters is one long chapter
			var title = titleOf(document.getDoctitle(), this.properties.bookName());
			chapters.add(new Chapter(title, slugify(title), declare(document.convert())));
		}
		return chapters;
	}

	private void pandoc(File pandoc, Chapter chapter, File in, File out) throws Exception {
		var command = List.of(pandoc.getPath(), //
				"--from=docbook", //
				"--to=" + this.flavor(), //
				"--wrap=none", //
				"--output=" + out.getAbsolutePath(), //
				in.getAbsolutePath());
		log.debug("running {}", String.join(" ", command));
		var process = new ProcessBuilder(command)//
			.directory(this.properties.root())//
			.redirectErrorStream(true)//
			.start();
		var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
		Assert.state(process.waitFor(5, TimeUnit.MINUTES),
				() -> "pandoc did not finish converting '" + chapter.title() + "' in time");
		if (process.exitValue() != 0) {
			throw new BrokenChapterException("pandoc exited with " + process.exitValue() + ": " + output);
		}
		Assert.state(out.exists(), () -> "pandoc produced no " + out.getAbsolutePath());
		if (StringUtils.hasText(output)) {
			log.warn("pandoc said this about '{}': {}", chapter.title(), output);
		}
	}

	/**
	 * a converted chapter is a DocBook fragment; give it the declaration that makes it a
	 * document in its own right, which is what {@code pandoc} wants to be handed.
	 */
	private static String declare(String docbook) {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + docbook;
	}

	private static String titleOf(String title, String fallback) {
		return StringUtils.hasText(title) ? title.trim() : fallback;
	}

	private String flavor() {
		var markdown = this.properties.markdown();
		return (markdown != null && StringUtils.hasText(markdown.flavor())) ? markdown.flavor() : DEFAULT_FLAVOR;
	}

	private boolean ignoreBrokenChapters() {
		var markdown = this.properties.markdown();
		return markdown != null && markdown.ignoreBrokenChapters();
	}

	/**
	 * {@code publication.markdown.pandoc} wins, then {@code $PANDOC}, and failing both we
	 * assume {@code pandoc} is on the {@code $PATH}.
	 */
	private File pandocBinary() {
		var markdown = this.properties.markdown();
		if (markdown != null && markdown.pandoc() != null) {
			var binary = markdown.pandoc();
			Assert.state(binary.exists(), () -> "the pandoc binary " + binary.getAbsolutePath() + " does not exist");
			return binary;
		}
		var fromTheEnvironment = System.getenv("PANDOC");
		if (StringUtils.hasText(fromTheEnvironment)) {
			var binary = new File(fromTheEnvironment);
			Assert.state(binary.exists(),
					() -> "the pandoc binary " + binary.getAbsolutePath() + ", named by $PANDOC, does not exist");
			return binary;
		}
		return new File("pandoc");
	}

	private static String slugify(String title) {
		var slug = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
		return StringUtils.hasText(slug) ? slug : "chapter";
	}

	private record Chapter(String title, String name, String docbook) {
	}

	private static class BrokenChapterException extends RuntimeException {

		BrokenChapterException(String message) {
			super(message);
		}

	}

}
