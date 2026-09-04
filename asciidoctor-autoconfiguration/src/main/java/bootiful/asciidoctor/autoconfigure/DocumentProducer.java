package bootiful.asciidoctor.autoconfigure;

import org.asciidoctor.*;

import java.io.File;

public interface DocumentProducer {

	default OptionsBuilder buildCommonOptions(String backend) {
		return Options.builder().safe(SafeMode.UNSAFE).backend(backend).inPlace(false);
	}

	default OptionsBuilder buildCommonOptions(String backend, Attributes attributes) {
		return this.buildCommonOptions(backend).attributes(attributes);
	}

	default File getIndexAdoc(File root) {
		return new File(root, "index.adoc");
	}

	/**
	 * Note that {@code imagesdir} is deliberately not set here. Attributes handed to
	 * Asciidoctor through the API are locked, so setting it would both prepend
	 * {@code images/} to every image a document already spells out in full - giving
	 * {@code images/images/...}, which resolves to nothing - and leave the document no
	 * way to say otherwise. A document that wants one declares {@code :imagesdir:} in its
	 * own header.
	 */
	default AttributesBuilder buildCommonAttributes(String bookName, String isbn, File source) {

		return Attributes.builder()//
			.title(bookName)//
			.attribute("doctitle", bookName) //
			.tableOfContents(true) //
			.attribute("isbn", isbn) //
			.attribute("book-name", bookName) //
			.sectionNumbers(true) //
			.attribute("code", source.getAbsolutePath()) //
			.tableOfContents(true) //
			.sectionNumbers(true) //
			.sourceHighlighter("coderay");
	}

	File[] produce() throws Exception;

}
