package bootiful.asciidoctor.autoconfigure;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.Cursor;
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
import java.util.regex.Pattern;

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

	/** the opening tag of a fragment, which is where the namespaces have to go */
	private static final Pattern ROOT_ELEMENT = Pattern.compile("<([A-Za-z_][\\w.-]*)");

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
				// check the XML ourselves first. pandoc catches this too, but it can only
				// report against the file it was handed; going first lets us describe the
				// failure in terms of the AsciiDoc that has to change
				var problem = DocbookDiagnostics.wellFormednessOf(chapter.docbook());
				if (problem != null) {
					throw new BrokenChapterException(problem.message(),
							DocbookDiagnostics.report(chapter, problem, chapterDocbook, root, "the XML parser"));
				}
				this.pandoc(pandoc, chapter, chapterDocbook, markdown);
				log.info("wrote '{}' to {}", chapter.title(), markdown.getAbsolutePath());
				markdownFiles.add(markdown);
				if (this.keepDocbook()) {
					log.info("keeping the DocBook for '{}' at {}", chapter.title(), chapterDocbook.getAbsolutePath());
				}
				else {
					chapterDocbook.delete();
				}
			}
			catch (BrokenChapterException bce) {
				// one chapter's worth of bad markup shouldn't cost you the other thirty,
				// so keep going and account for all of them at the end. The DocBook stays
				// on disk: it's the thing you want to read to work out what happened.
				log.error("could not convert the chapter '{}'.\n\n{}", chapter.title(), bce.report());
				broken.add("'" + chapter.title() + "' (see " + chapterDocbook.getAbsolutePath() + ")");
			}
		}

		Assert.state(broken.isEmpty() || this.ignoreBrokenChapters(),
				() -> broken.size() + " of " + chapters.size() + " chapters could not be converted to Markdown: "
						+ String.join(", ", broken) + ". Each one was logged at ERROR above with the line of "
						+ "AsciiDoc to go and fix. pandoc rejects DocBook that isn't well formed, and that's "
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
		// sourcemap costs a little to build and buys the one thing the DocBook backend
		// won't tell us: which file, of all the ones include::d into the book, a chapter
		// actually came from. Without it a broken chapter can only be named by its title
		var options = this.buildCommonOptions("docbook", attributes.build()).docType("book").sourcemap(true);
		var document = this.asciidoctor.loadFile(index, options.build());
		var chapters = new ArrayList<Chapter>();
		for (var block : document.getBlocks()) {
			if (block instanceof Section section && section.getLevel() == 1) {
				var title = titleOf(section.getTitle(), "chapter");
				chapters.add(chapter(title, "%02d-%s".formatted(chapters.size() + 1, slugify(title)), section.convert(),
						section.getSourceLocation()));
			}
		}
		if (chapters.isEmpty()) {
			// not every document is a book: one without chapters is one long chapter
			var title = titleOf(document.getDoctitle(), this.properties.bookName());
			chapters.add(chapter(title, slugify(title), document.convert(), document.getSourceLocation()));
		}
		return chapters;
	}

	/**
	 * pairs a converted chapter with where it came from, and says so in the log while
	 * it's at it - when a later chapter turns out to be broken, the line that read it is
	 * the one that names the file to open.
	 */
	private static Chapter chapter(String title, String name, String docbook, Cursor location) {
		var source = (location != null && location.getFile() != null) ? new File(location.getFile()) : null;
		var line = location != null ? location.getLineNumber() : 0;
		if (source != null) {
			log.info("read the chapter '{}' from {}, line {}", title, source.getAbsolutePath(), line);
		}
		else {
			log.info("read the chapter '{}'; Asciidoctor didn't say which file it came from", title);
		}
		return new Chapter(title, name, declare(docbook), source, line);
	}

	private void pandoc(File pandoc, Chapter chapter, File in, File out) throws Exception {
		var command = List.of(pandoc.getPath(), //
				"--from=docbook", //
				"--to=" + this.flavor(), //
				"--wrap=none", //
				"--output=" + out.getAbsolutePath(), //
				in.getAbsolutePath());
		log.info("converting '{}': {}", chapter.title(), String.join(" ", command));
		var process = new ProcessBuilder(command)//
			.directory(this.properties.root())//
			.redirectErrorStream(true)//
			.start();
		var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
		Assert.state(process.waitFor(5, TimeUnit.MINUTES),
				() -> "pandoc did not finish converting '" + chapter.title() + "' in time");
		if (process.exitValue() != 0) {
			// the XML parsed for us and pandoc still won't have it, so this is something
			// else - a construct it doesn't implement, most likely. Report it the same
			// way
			// regardless: its coordinates, if it gave any, against the same excerpts
			var message = "pandoc exited with " + process.exitValue() + ": " + output;
			throw new BrokenChapterException(message, DocbookDiagnostics.report(chapter,
					DocbookDiagnostics.fromPandoc(output), in, this.properties.root(), "pandoc"));
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
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + namespaced(docbook);
	}

	/**
	 * binds the namespaces the chapter uses but doesn't declare.
	 * <p>
	 * Asciidoctor declares them once, on the {@code <book>} element, and converting a
	 * chapter on its own never produces that element - so a chapter that links to
	 * anything uses the {@code xl:} prefix without ever binding it. pandoc's reader lets
	 * that go, but a conforming XML parser is entitled not to, and one of those now reads
	 * the chapter first. Binding them here makes the fragment the standalone document
	 * we're already claiming it is, rather than teaching the checker to overlook it.
	 */
	private static String namespaced(String docbook) {
		var matcher = ROOT_ELEMENT.matcher(docbook);
		if (!matcher.find()) {
			return docbook;
		}
		var end = docbook.indexOf('>', matcher.end());
		if (end < 0) {
			return docbook;
		}
		var root = docbook.substring(matcher.start(), end);
		var bindings = new StringBuilder();
		if (!root.contains("xmlns=")) {
			bindings.append(" xmlns=\"http://docbook.org/ns/docbook\"");
		}
		// both spellings of the XLink prefix, because which one Asciidoctor reaches for
		// depends on the version, and binding one it doesn't use costs nothing
		if (!root.contains("xmlns:xl=")) {
			bindings.append(" xmlns:xl=\"http://www.w3.org/1999/xlink\"");
		}
		if (!root.contains("xmlns:xlink=")) {
			bindings.append(" xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
		}
		if (!root.contains("version=")) {
			bindings.append(" version=\"5.0\"");
		}
		return bindings.isEmpty() ? docbook
				: docbook.substring(0, matcher.end()) + bindings + docbook.substring(matcher.end());
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
	 * a broken chapter's DocBook is always left behind; this keeps the ones that
	 * converted too, for when you want to read what pandoc was actually given.
	 */
	private boolean keepDocbook() {
		var markdown = this.properties.markdown();
		return markdown != null && markdown.keepDocbook();
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

	/**
	 * a converted chapter and the {@code .adoc} it was read from. {@code source} is null,
	 * and {@code sourceLine} zero, only when Asciidoctor wouldn't say.
	 */
	record Chapter(String title, String name, String docbook, File source, int sourceLine) {
	}

	private static class BrokenChapterException extends RuntimeException {

		/** the long form: coordinates, excerpts and a guess at the cause */
		private final String report;

		BrokenChapterException(String message, String report) {
			super(message);
			this.report = report;
		}

		String report() {
			return this.report;
		}

	}

}
