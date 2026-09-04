package bootiful.asciidoctor.autoconfigure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@EnabledIf("pandocIsInstalled")
class MarkdownProducerTest {

	private static final Logger log = LoggerFactory.getLogger(MarkdownProducerTest.class);

	/**
	 * a chapter whose markup doesn't survive the trip through XML. The lone {@code *} on
	 * the first line and the first {@code *} of the second pair up into a bold run that
	 * straddles both {@code literal} spans, so the tags interleave rather than nest -
	 * something HTML and PDF shrug off and DocBook can't express. Not a hypothetical:
	 * this is copied from the {@literal Secure All The Things} manuscript, one of the two
	 * places in it that pandoc rejects.
	 */
	private static final String BROKEN_CHAPTER = """
			= Ant Patterns

			A single `*` matches within one path segment.
			A double `**` matches any number of segments, including none at all.
			""";

	static boolean pandocIsInstalled() {
		return pandoc() != null;
	}

	/**
	 * the producer itself falls back to the {@code $PATH}, but the test has to know
	 * whether there's any point in running at all.
	 */
	private static File pandoc() {
		var fromTheEnvironment = System.getenv("PANDOC");
		var candidates = fromTheEnvironment != null ? new File[] { new File(fromTheEnvironment) } : new File[] {
				new File("/opt/homebrew/bin/pandoc"), new File("/usr/local/bin/pandoc"), new File("/usr/bin/pandoc") };
		for (var candidate : candidates) {
			if (candidate.exists()) {
				return candidate;
			}
		}
		return null;
	}

	@Test
	void producesOneMarkdownFilePerChapter(@TempDir File root) throws Exception {

		write(root, "index.adoc", """
				:doctype: book
				:leveloffset: 1

				include::first.adoc[]

				include::second.adoc[]
				""");
		write(root, "first.adoc", """
				= The First Chapter

				Hello from the first chapter.

				== A Section Within It

				NOTE: an admonition, which is the reason we go by way of DocBook.

				[source,java]
				----
				var greeting = "hello";
				----
				""");
		write(root, "second.adoc", """
				= The Second Chapter

				Hello from the second chapter.
				""");

		this.produce(root, produced -> {

			var markdown = Stream.of(produced).filter(f -> f.getName().endsWith(".md")).toList();
			assertThat(markdown).as("one Markdown file per chapter, in order").hasSize(2);
			assertThat(markdown.get(0)).hasName("01-the-first-chapter.md");
			assertThat(markdown.get(1)).hasName("02-the-second-chapter.md");
			assertThat(markdown).allSatisfy(file -> assertThat(file).exists());

			var first = Files.readString(markdown.get(0).toPath());
			assertThat(first).as("the chapter title becomes the document's own top-level heading")
				.startsWith("# The First Chapter");
			assertThat(first).contains("## A Section Within It");
			assertThat(first).as("the source listing keeps its language").contains("``` java");
			assertThat(first).as("the admonition survives as prose, not as a table")
				.contains("an admonition")
				.doesNotContain("|---");
			assertThat(Files.readString(markdown.get(1).toPath())).startsWith("# The Second Chapter");

			assertThat(new File(root, "markdown").listFiles((_, name) -> name.endsWith(".xml")))
				.as("the intermediate DocBook is cleaned up")
				.isEmpty();
		});
	}

	@Test
	void refusesToPublishABookWithAChapterItCouldNotConvert(@TempDir File root) throws Exception {

		write(root, "index.adoc", """
				:doctype: book
				:leveloffset: 1

				include::good.adoc[]

				include::broken.adoc[]
				""");
		write(root, "good.adoc", """
				= A Good Chapter

				Nothing wrong here.
				""");
		write(root, "broken.adoc", BROKEN_CHAPTER);

		assertThatIllegalStateException().isThrownBy(() -> this.produce(root, _ -> {
		}))
			.withMessageContaining("1 of 2 chapters")
			.withMessageContaining("Ant Patterns")
			.withMessageContaining("publication.markdown.ignore-broken-chapters");

		assertThat(new File(new File(root, "markdown"), "02-ant-patterns.xml"))
			.as("the DocBook that pandoc refused is left behind to be read")
			.exists();
	}

	@Test
	void publishesTheRestWhenBrokenChaptersAreIgnored(@TempDir File root) throws Exception {

		write(root, "index.adoc", """
				:doctype: book
				:leveloffset: 1

				include::good.adoc[]

				include::broken.adoc[]
				""");
		write(root, "good.adoc", """
				= A Good Chapter

				Nothing wrong here.
				""");
		write(root, "broken.adoc", BROKEN_CHAPTER);

		this.produce(root, produced -> {
			var markdown = Stream.of(produced).filter(f -> f.getName().endsWith(".md")).toList();
			assertThat(markdown).as("the one chapter that converted still gets published").hasSize(1);
			assertThat(markdown.get(0)).hasName("01-a-good-chapter.md");
		}, "publication.markdown.ignore-broken-chapters=true");
	}

	@Test
	void treatsADocumentWithoutChaptersAsOneChapter(@TempDir File root) throws Exception {

		write(root, "index.adoc", """
				= Just The One Thing

				No chapters here, only prose.
				""");

		this.produce(root, produced -> {
			var markdown = Stream.of(produced).filter(f -> f.getName().endsWith(".md")).toList();
			assertThat(markdown).hasSize(1);
			// the book name, because that's the title the producers all render under
			assertThat(markdown.get(0)).hasName("a-book.md");
			assertThat(Files.readString(markdown.get(0).toPath())).contains("No chapters here");
		});
	}

	/**
	 * the same thing against the real manuscript, when there's one checked out next door.
	 * It has chapters pandoc won't take, hence the leniency. The Markdown is left in
	 * {@code target/publication/MarkdownProducer} afterwards - go read it.
	 */
	@Test
	@EnabledIf("bookIsCheckedOutLocally")
	void producesMarkdownForTheBook() throws Exception {
		var docs = new File(book(), "book");
		var markdownDirectory = new File(docs, "markdown");
		var published = new File(new File("target/publication"), MarkdownProducer.class.getSimpleName());
		try {
			this.produce(docs, book(), produced -> {
				var markdown = Stream.of(produced).filter(f -> f.getName().endsWith(".md")).toList();
				assertThat(markdown).as("the book has rather more than one chapter").hasSizeGreaterThan(10);
				assertThat(markdown).allSatisfy(file -> assertThat(Files.readString(file.toPath()))
					.as("%s starts with the chapter's own heading", file.getName())
					.startsWith("# "));
				assertThat(markdown).anySatisfy(file -> assertThat(Files.readString(file.toPath()))
					.as("the listings the book include::s from the code checkout come through")
					.contains("class Main"));
				assertThat(markdown).allSatisfy(file -> assertThat(Files.readString(file.toPath()))
					.as("%s resolved every include", file.getName())
					.doesNotContain("Unresolved directive"));

				// step two: the same thing the DocumentProducerTasklet does with whatever
				// a producer hands back - copy it into $target/$producer, which is what
				// the publishers then upload
				assertThat(published.exists() || published.mkdirs()).isTrue();
				for (var file : produced) {
					FileCopyUtils.copy(file, new File(published, file.getName()));
				}

				// and what's been published has to stand on its own: every image a
				// chapter points at has to be one we published next to it. Setting
				// imagesdir on top of paths the book already spells out in full used to
				// make these images/images/..., which resolved to nothing
				var references = imageReferencesIn(published);
				assertThat(references).as("the book's chapters do reference images").isNotEmpty();
				assertThat(references).allSatisfy(reference -> assertThat(new File(published, reference))
					.as("%s, referenced by the published Markdown", reference)
					.exists());
				log.info("published {} Markdown chapter(s) to {}", markdown.size(), published.getAbsolutePath());
			}, "publication.markdown.ignore-broken-chapters=true");
		} //
		finally {
			// the producer renders in place, into the document checkout; leave that
			// checkout as we found it now that the Markdown has been copied to the target
			org.springframework.util.FileSystemUtils.deleteRecursively(markdownDirectory);
		}
	}

	static boolean bookIsCheckedOutLocally() {
		return pandocIsInstalled() && new File(new File(book(), "book"), "index.adoc").exists();
	}

	private static File book() {
		return new File(System.getProperty("book.root", System.getenv()
			.getOrDefault("BOOK_ROOT", System.getProperty("user.home") + "/code/secure-all-the-things-book")));
	}

	private static void write(File root, String name, String content) throws Exception {
		Files.writeString(new File(root, name).toPath(), content);
	}

	/**
	 * every path the published Markdown points an image at, however pandoc chose to write
	 * it - {@code ![alt](path)} or an {@code <img src="path">} it fell back to.
	 */
	private static List<String> imageReferencesIn(File directory) throws Exception {
		var pattern = java.util.regex.Pattern.compile("!\\[[^]]*]\\(([^)\\s]+)\\)|<img[^>]*src=\"([^\"]+)\"");
		var references = new java.util.ArrayList<String>();
		var files = directory.listFiles((_, name) -> name.endsWith(".md"));
		for (var file : files == null ? new File[0] : files) {
			var matcher = pattern.matcher(Files.readString(file.toPath()));
			while (matcher.find()) {
				references.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
			}
		}
		return references;
	}

	private void produce(File root, ThrowingConsumer<File[]> consumer, String... extraProperties) throws Exception {
		this.produce(root, root, consumer, extraProperties);
	}

	/**
	 * {@code root} is the document checkout, the one holding {@code index.adoc};
	 * {@code code} is the checkout the book's listings are {@code include::}d from. The
	 * pipeline derives both from {@code pipeline.job.root}.
	 */
	private void produce(File root, File code, ThrowingConsumer<File[]> consumer, String... extraProperties)
			throws Exception {
		var properties = new java.util.ArrayList<>(java.util.List.of(//
				"publication.book-name=A Book", //
				"publication.root=" + root.getAbsolutePath(), //
				"publication.code=" + code.getAbsolutePath(), //
				"publication.target=" + new File("target/publication").getAbsolutePath(), //
				"publication.markdown.enabled=true", //
				"publication.markdown.pandoc=" + pandoc().getAbsolutePath(), //
				// nothing else needs rendering to prove the Markdown out
				"publication.html.enabled=false", //
				"publication.epub.enabled=false", //
				"publication.mobi.enabled=false", //
				"publication.pdf.prepress.enabled=false", //
				"publication.pdf.screen.enabled=false"));
		properties.addAll(java.util.List.of(extraProperties));
		var thrown = new RuntimeException[1];
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(AsciidoctorPublicationAutoConfiguration.class))
			.withPropertyValues(properties.toArray(new String[0]))
			.run(context -> {
				assertThat(context).hasNotFailed();
				var producer = context.getBean("markdownProducer", DocumentProducer.class);
				assertThat(producer).isInstanceOf(MarkdownProducer.class);
				try {
					consumer.accept(producer.produce());
				}
				catch (RuntimeException re) {
					// the ApplicationContextRunner would wrap this in an
					// IllegalStateException of its own, which the tests then can't tell
					// apart from the producer's
					thrown[0] = re;
				}
			});
		if (thrown[0] != null) {
			throw thrown[0];
		}
	}

	@FunctionalInterface
	private interface ThrowingConsumer<T> {

		void accept(T t) throws Exception;

	}

}
