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
				.imagesDir("images") //
				.sourceHighlighter("coderay");
	}

	File[] produce() throws Exception;

}
