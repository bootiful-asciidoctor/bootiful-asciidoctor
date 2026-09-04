package bootiful.asciidoctor.autoconfigure;

import org.springframework.util.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Works out where a chapter's DocBook went wrong and says so in terms you can act on.
 * <p>
 * {@code pandoc} will tell you it found something wrong at line 5, column 11, but line 5
 * of a machine-generated XML file is not somewhere you can fix anything. What you need is
 * the line of AsciiDoc that produced it, and that's what this tries hard to give you: the
 * XML is checked for well-formedness before {@code pandoc} ever sees it, and the text
 * around the break is matched back against the {@code .adoc} sources to name the file and
 * line you actually have to edit.
 * <p>
 * The matching is a best effort - it's text, not a real source map, because the DocBook
 * backend doesn't emit one - so the report always includes the raw XML excerpt too. When
 * the guess is wrong, the excerpt is still enough to work from.
 */
abstract class DocbookDiagnostics {

	/**
	 * how much of a line we're willing to print before windowing it around the column
	 * that matters.
	 */
	private static final int MAX_LINE_WIDTH = 160;

	/**
	 * how many words of a line have to line up before we'll believe we've found the
	 * AsciiDoc that produced it. Tried longest-first, so a confident match wins over a
	 * vague one.
	 */
	private static final int[] ANCHOR_LENGTHS = { 8, 6, 5, 4, 3 };

	private static final Pattern TAG = Pattern.compile("<[^>]*>");

	private static final Pattern ENTITY = Pattern.compile("&#?\\w+;");

	private static final Pattern NOT_A_WORD = Pattern.compile("[^a-z0-9]+");

	/** {@code 5:11 (274)-5:21 (284): ...}, which is how pandoc puts it */
	private static final Pattern PANDOC_COORDINATES = Pattern.compile("(\\d+):(\\d+)\\s*\\(\\d+\\)");

	/** {@code ... (line 5, column 11)}, which is how it used to */
	private static final Pattern PROSE_COORDINATES = Pattern.compile("line (\\d+), column (\\d+)");

	/**
	 * something wrong at a point in the DocBook: a one-based line and column, and whoever
	 * noticed it saying what it was. A column of zero means "somewhere on this line".
	 */
	record Problem(int line, int column, String message) {
	}

	/**
	 * parses the DocBook the way pandoc is about to and reports the first thing that
	 * isn't well formed, or null if it's clean. Worth doing even though pandoc checks
	 * too: this runs before anything is handed off, and the messages name the element
	 * that was left open rather than the one that tripped over it.
	 */
	static Problem wellFormednessOf(String docbook) {
		try {
			var parser = saxParserFactory().newSAXParser();
			var source = new InputSource(new StringReader(docbook));
			parser.parse(source, new DefaultHandler() {
				@Override
				public InputSource resolveEntity(String publicId, String systemId) {
					// a DOCTYPE shouldn't send us to the network to find out the XML is
					// broken
					return new InputSource(new StringReader(""));
				}
			});
			return null;
		}
		catch (SAXParseException spe) {
			return new Problem(Math.max(spe.getLineNumber(), 1), Math.max(spe.getColumnNumber(), 0),
					spe.getMessage() == null ? spe.toString() : spe.getMessage());
		}
		catch (Exception e) {
			// not a well-formedness problem, so we have no coordinates to offer; say so
			// rather than pretending to point at line 1
			return new Problem(0, 0, e.getMessage() == null ? e.toString() : e.getMessage());
		}
	}

	/**
	 * whatever pandoc said, reduced to coordinates we can point at. Null when it
	 * complained about something other than the XML and there's nothing to point at.
	 */
	static Problem fromPandoc(String output) {
		if (!StringUtils.hasText(output)) {
			return null;
		}
		for (var pattern : List.of(PANDOC_COORDINATES, PROSE_COORDINATES)) {
			var matcher = pattern.matcher(output);
			if (matcher.find()) {
				return new Problem(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
						output.trim());
			}
		}
		return null;
	}

	/**
	 * the whole account of one broken chapter: what's wrong, where it is in the XML,
	 * where we think it is in the AsciiDoc, both excerpts, and what usually causes it.
	 * @param problem what went wrong, possibly null if the reporter gave us nothing to go
	 * on
	 * @param blame who noticed - the XML parser or pandoc - so the reader knows how much
	 * to trust the coordinates
	 */
	static String report(MarkdownProducer.Chapter chapter, Problem problem, File docbookFile, File root, String blame) {

		var report = new StringBuilder();
		var docbookLines = linesOf(chapter.docbook());
		report.append("the chapter '")
			.append(chapter.title())
			.append("' is not well-formed DocBook, ")
			.append("so pandoc can't read it.\n\n");

		var docbookLine = problem == null ? 0 : problem.line();
		var located = locateInAsciidoc(docbookLines, docbookLine, chapter.source(), root);

		// the facts, one to a line and aligned on the colon, because this is the part
		// people skim
		var facts = new ArrayList<String[]>();
		facts.add(new String[] { "what " + blame + " said",
				problem == null ? "(nothing we could place)" : collapse(problem.message()) });
		var where = new StringBuilder(docbookFile.getAbsolutePath());
		if (problem != null && problem.line() > 0) {
			where.append(", line ").append(problem.line());
			if (problem.column() > 0) {
				where.append(", column ").append(problem.column());
			}
		}
		facts.add(new String[] { "the DocBook", where.toString() });
		// the chapter's own beginning: always known, and the fallback for when the text
		// match came up empty
		if (chapter.source() != null) {
			facts.add(new String[] { "the chapter begins at",
					chapter.source().getAbsolutePath() + ", line " + Math.max(chapter.sourceLine(), 1) });
		}
		for (var i = 0; i < located.size(); i++) {
			facts.add(new String[] { i == 0 ? "and the AsciiDoc to fix" : "or perhaps", describe(located.get(i)) });
		}
		var width = facts.stream().mapToInt(fact -> fact[0].length()).max().orElse(0);
		for (var fact : facts) {
			report.append("  ")
				.append(fact[0])
				.append(" ".repeat(width - fact[0].length()))
				.append(" : ")
				.append(fact[1])
				.append('\n');
		}

		if (problem != null && problem.line() > 0) {
			report.append("\n  the DocBook pandoc read:\n");
			report.append(excerpt(docbookLines, problem.line(), problem.column(), 2));
		}

		if (!located.isEmpty()) {
			var best = located.get(0);
			var sourceLines = linesOf(best.file());
			if (!sourceLines.isEmpty()) {
				report.append("\n  the AsciiDoc that produced it:\n");
				report.append(excerpt(sourceLines, best.line(), 0, 2));
			}
		}

		var hint = hintFor(problem == null ? "" : problem.message());
		if (hint != null) {
			report.append("\n  ").append(hint).append('\n');
		}
		return report.toString();
	}

	private static String describe(Location location) {
		return location.file().getAbsolutePath() + ", line " + location.line();
	}

	/**
	 * what usually causes a given complaint. Only the ones we've actually seen come out
	 * of this pipeline - a guess that's wrong more often than right is worse than none.
	 */
	private static String hintFor(String message) {
		var lowered = message.toLowerCase(Locale.ROOT);
		if (lowered.contains("must be terminated") || lowered.contains("expected end element")
				|| lowered.contains("must be matched") || lowered.contains("end-tag")) {
			return """
					this is nearly always overlapping inline markup: a `, * or _ that opens inside one \
					span and closes inside another, so the tags interleave instead of nesting. HTML and \
					PDF render it anyway; XML can't express it. Look for a stray ` or * or _ on the \
					lines above.""";
		}
		if (lowered.contains("not declared") || lowered.contains("entity")) {
			return """
					an entity XML doesn't know. A bare & in the AsciiDoc, or an HTML-only name like \
					&nbsp;, has been passed straight through - write it as \\& or use the numeric form.""";
		}
		if (lowered.contains("prolog")) {
			return """
					there's content before the root element, which usually means the chapter's first \
					line isn't what Asciidoctor expected a chapter to start with.""";
		}
		if (lowered.contains("invalid xml character") || lowered.contains("character reference")) {
			return """
					a control character that XML won't carry has made it into the source, most often \
					pasted in from a terminal transcript.""";
		}
		return null;
	}

	/**
	 * the AsciiDoc line, or lines, that most likely produced the DocBook we broke on.
	 * <p>
	 * The DocBook keeps the prose of the source more or less intact, so we take the text
	 * of the offending line, throw away everything that isn't a word - tags, entities,
	 * and the punctuation Asciidoctor rewrites on the way through, like {@code '}
	 * becoming {@code &#8217;} - and look for a run of those words in the {@code .adoc}
	 * files. The markup that actually broke is exactly what won't match, so we match on
	 * the prose around it.
	 * <p>
	 * Everything that matched at the longest run we tried comes back. Two candidates
	 * named honestly beat one picked at random.
	 */
	private static List<Location> locateInAsciidoc(List<String> docbookLines, int docbookLine, File chapterSource,
			File root) {

		if (docbookLine <= 0 || docbookLine > docbookLines.size()) {
			return List.of();
		}
		var files = asciidocFiles(chapterSource, root);
		if (files.isEmpty()) {
			return List.of();
		}
		// the reported line first, then the one above it: a parser notices an unclosed
		// span
		// at the point it gives up on it, which can be a line later than the one at fault
		for (var offset : new int[] { 0, -1 }) {
			var candidate = docbookLine + offset;
			if (candidate < 1 || candidate > docbookLines.size()) {
				continue;
			}
			var words = wordsOf(docbookLines.get(candidate - 1));
			for (var length : ANCHOR_LENGTHS) {
				if (words.size() < length) {
					continue;
				}
				var anchor = String.join(" ", words.subList(0, length));
				var found = search(files, anchor);
				if (!found.isEmpty()) {
					return found.size() > 3 ? found.subList(0, 3) : found;
				}
			}
		}
		return List.of();
	}

	private static List<Location> search(List<File> files, String anchor) {
		var found = new ArrayList<Location>();
		for (var file : files) {
			var lines = linesOf(file);
			for (var i = 0; i < lines.size(); i++) {
				if (String.join(" ", wordsOf(lines.get(i))).contains(anchor)) {
					found.add(new Location(file, i + 1));
				}
			}
		}
		return found;
	}

	/**
	 * the chapter's own file first - the answer is usually there, and looking there first
	 * keeps an unlucky match in some other chapter from winning - and then everything
	 * else under the root, because a chapter is often several {@code include::}d files.
	 */
	private static List<File> asciidocFiles(File chapterSource, File root) {
		var files = new ArrayList<File>();
		if (chapterSource != null && chapterSource.isFile()) {
			files.add(chapterSource);
		}
		if (root != null && root.isDirectory()) {
			try (Stream<Path> walk = Files.walk(root.toPath())) {
				walk.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().endsWith(".adoc"))
					.map(Path::toFile)
					.filter(file -> !files.contains(file))
					.limit(500)
					.forEach(files::add);
			}
			catch (IOException | RuntimeException e) {
				// the excerpt alone still tells the reader plenty; don't let the search
				// for
				// something better be the thing that fails
			}
		}
		return files;
	}

	/**
	 * a line reduced to the words it has in common with whatever produced it: tags,
	 * entities and punctuation gone, because those are precisely what differs between the
	 * AsciiDoc and the XML.
	 */
	private static List<String> wordsOf(String line) {
		var text = TAG.matcher(line).replaceAll(" ");
		text = ENTITY.matcher(text).replaceAll(" ");
		text = NOT_A_WORD.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
		if (text.isEmpty()) {
			return List.of();
		}
		return List.of(text.split(" "));
	}

	/**
	 * numbered lines either side of the one that matters, with a caret under the column
	 * when we have one.
	 */
	private static String excerpt(List<String> lines, int line, int column, int context) {
		var from = Math.max(1, line - context);
		var to = Math.min(lines.size(), line + context);
		// a file's trailing newline gives a last, empty line; numbering it says nothing
		while (to > line && lines.get(to - 1).isBlank()) {
			to--;
		}
		var width = String.valueOf(to).length();
		var excerpt = new StringBuilder();
		for (var number = from; number <= to; number++) {
			var windowed = window(lines.get(number - 1), number == line ? column : 0);
			excerpt.append("    ")
				.append(String.format("%" + width + "d", number))
				.append(" | ")
				.append(windowed.text())
				.append('\n');
			if (number == line && windowed.caret() >= 0) {
				excerpt.append("    ")
					.append(" ".repeat(width))
					.append(" | ")
					.append(" ".repeat(windowed.caret()))
					.append("^\n");
			}
		}
		return excerpt.toString();
	}

	/**
	 * one line made printable: tabs expanded so the caret lands where it should, and a
	 * long line cut down to a window around the column that matters. Asciidoctor writes a
	 * whole paragraph as a single line, so this is the common case, not the exotic one.
	 */
	private static Windowed window(String line, int column) {
		var expanded = new StringBuilder();
		var caret = -1;
		for (var i = 0; i < line.length(); i++) {
			if (column > 0 && i == column - 1) {
				caret = expanded.length();
			}
			var character = line.charAt(i);
			expanded.append(character == '\t' ? "    " : character);
		}
		if (column > 0 && caret < 0) {
			// the column can be one past the end, which is where a missing end tag is
			caret = expanded.length();
		}
		var text = expanded.toString();
		if (text.length() <= MAX_LINE_WIDTH) {
			return new Windowed(text, caret);
		}
		var start = caret > MAX_LINE_WIDTH - 40 ? Math.max(0, caret - MAX_LINE_WIDTH / 2) : 0;
		var end = Math.min(text.length(), start + MAX_LINE_WIDTH);
		var prefix = start > 0 ? "..." : "";
		var suffix = end < text.length() ? "..." : "";
		return new Windowed(prefix + text.substring(start, end) + suffix,
				caret < 0 ? -1 : caret - start + prefix.length());
	}

	private static String collapse(String message) {
		return message == null ? "" : message.replaceAll("\\s+", " ").trim();
	}

	private static List<String> linesOf(String text) {
		return List.of(text.split("\r\n|\r|\n", -1));
	}

	private static List<String> linesOf(File file) {
		try {
			return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
		}
		catch (Exception e) {
			return List.of();
		}
	}

	private static SAXParserFactory saxParserFactory() {
		var factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		setQuietly(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
		setQuietly(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		setQuietly(factory, "http://xml.org/sax/features/external-general-entities", false);
		setQuietly(factory, "http://xml.org/sax/features/external-parameter-entities", false);
		return factory;
	}

	private static void setQuietly(SAXParserFactory factory, String feature, boolean value) {
		try {
			factory.setFeature(feature, value);
		}
		catch (Exception e) {
			// not every parser knows every feature, and none of them are load-bearing
			// here
		}
	}

	private record Windowed(String text, int caret) {
	}

	private record Location(File file, int line) {
	}

}
