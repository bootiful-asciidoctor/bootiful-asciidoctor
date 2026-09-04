package bootiful.asciidoctor.autoconfigure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * the reporting on its own, without pandoc: the point of it is that it explains a broken
 * chapter in terms of the AsciiDoc, so that's what gets asserted.
 */
class DocbookDiagnosticsTest {

	private static final Logger log = LoggerFactory.getLogger(DocbookDiagnosticsTest.class);

	/**
	 * the AsciiDoc from the {@literal Secure All The Things} manuscript that doesn't
	 * survive the trip through XML, and the DocBook Asciidoctor makes of it. The lone
	 * {@code *} on one line pairs up with the first of the next line's, so the
	 * {@code emphasis} it opens straddles two {@code literal} spans and the tags
	 * interleave rather than nest.
	 */
	private static final String BROKEN_ADOC = """
			= Ant Patterns

			Spring matches paths the same way Ant did.

			A single `*` matches within one path segment.
			A double `**` matches any number of segments, including none at all.
			""";

	private static final String BROKEN_DOCBOOK = """
			<?xml version="1.0" encoding="UTF-8"?>
			<chapter xmlns="http://docbook.org/ns/docbook" version="5.0" xml:id="_ant_patterns">
			<title>Ant Patterns</title>
			<simpara>Spring matches paths the same way Ant did.</simpara>
			<simpara>A single <literal><emphasis>` matches within one path segment.
			A double `</literal></emphasis>` matches any number of segments, including none at all.</simpara>
			</chapter>
			""";

	private static final String GOOD_DOCBOOK = """
			<?xml version="1.0" encoding="UTF-8"?>
			<chapter xmlns="http://docbook.org/ns/docbook" version="5.0" xml:id="_fine">
			<title>Fine</title>
			<simpara>Nothing wrong here.</simpara>
			</chapter>
			""";

	@Test
	void saysNothingAboutWellFormedDocbook() {
		assertThat(DocbookDiagnostics.wellFormednessOf(GOOD_DOCBOOK)).isNull();
	}

	@Test
	void findsTheLineAndColumnOfInterleavedMarkup() {
		var problem = DocbookDiagnostics.wellFormednessOf(BROKEN_DOCBOOK);
		assertThat(problem).isNotNull();
		assertThat(problem.line()).as("the line of the DocBook the parser gave up on").isEqualTo(6);
		assertThat(problem.column()).isGreaterThan(0);
		assertThat(problem.message()).contains("emphasis");
	}

	@Test
	void pointsAtTheAsciidocLineThatHasToChange(@TempDir File root) throws Exception {
		var source = new File(root, "ant-patterns.adoc");
		Files.writeString(source.toPath(), BROKEN_ADOC);
		var chapter = new MarkdownProducer.Chapter("Ant Patterns", "02-ant-patterns", BROKEN_DOCBOOK, source, 1);
		var docbookFile = new File(root, "02-ant-patterns.xml");

		var report = DocbookDiagnostics.report(chapter, DocbookDiagnostics.wellFormednessOf(BROKEN_DOCBOOK),
				docbookFile, root, "the XML parser");
		log.info("the report reads:\n{}", report);

		assertThat(report).as("the chapter is named").contains("Ant Patterns");
		assertThat(report).as("and so is the XML, for when the guess below is wrong")
			.contains(docbookFile.getAbsolutePath());
		assertThat(report).as("the AsciiDoc file, and the line within it that actually has the bad markup")
			.contains(source.getAbsolutePath() + ", line 6");
		assertThat(report).as("both excerpts are shown")
			.contains("the DocBook pandoc read")
			.contains("the AsciiDoc that produced it")
			.contains("A double `**` matches any number of segments");
		assertThat(report).as("with a caret under the column").contains("^");
		assertThat(report).as("and what usually causes it").contains("overlapping inline markup");
	}

	@Test
	void fallsBackToTheChaptersOwnLocationWhenNothingMatches(@TempDir File root) throws Exception {
		var source = new File(root, "elsewhere.adoc");
		Files.writeString(source.toPath(), "= Elsewhere\n\nNothing in here resembles the DocBook.\n");
		var chapter = new MarkdownProducer.Chapter("Ant Patterns", "02-ant-patterns", BROKEN_DOCBOOK, source, 12);

		var report = DocbookDiagnostics.report(chapter, DocbookDiagnostics.wellFormednessOf(BROKEN_DOCBOOK),
				new File(root, "02-ant-patterns.xml"), root, "the XML parser");

		assertThat(report).as("we still know where the chapter began, even having failed to place the line")
			.contains(source.getAbsolutePath() + ", line 12");
		assertThat(report).doesNotContain("the AsciiDoc that produced it");
	}

	@Test
	void readsTheCoordinatesOutOfPandocsOwnComplaint() {
		var problem = DocbookDiagnostics.fromPandoc("""
				Invalid XML:
				5:11 (274)-5:21 (284): Expected end element for: Name {nameLocalName = "emphasis"}, \
				but received: EventEndElement""");
		assertThat(problem).isNotNull();
		assertThat(problem.line()).isEqualTo(5);
		assertThat(problem.column()).isEqualTo(11);
	}

	@Test
	void hasNothingToPointAtWhenPandocFailedForSomeOtherReason() {
		assertThat(DocbookDiagnostics.fromPandoc("pandoc: cannot find the file")).isNull();
		assertThat(DocbookDiagnostics.fromPandoc("")).isNull();
	}

	/**
	 * an undeclared entity is the other way a chapter breaks here - a bare {@code &}, or
	 * an HTML-only name like {@code &nbsp;} - and it deserves its own explanation rather
	 * than the one about inline markup.
	 */
	@Test
	void explainsAnUndeclaredEntityAsSuch(@TempDir File root) {
		var docbook = """
				<?xml version="1.0" encoding="UTF-8"?>
				<chapter xmlns="http://docbook.org/ns/docbook" version="5.0">
				<title>Ampersands</title>
				<simpara>Research &amp; development, and&nbsp;then some.</simpara>
				</chapter>
				""";
		var chapter = new MarkdownProducer.Chapter("Ampersands", "01-ampersands", docbook, null, 0);

		var report = DocbookDiagnostics.report(chapter, DocbookDiagnostics.wellFormednessOf(docbook),
				new File(root, "01-ampersands.xml"), root, "the XML parser");
		log.info("the report reads:\n{}", report);

		assertThat(report).contains("nbsp");
		assertThat(report).as("named, not blamed on unbalanced backticks").contains("an entity XML doesn't know");
	}

}
