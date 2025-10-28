package bootiful.asciidoctor.autoconfigure;

import org.asciidoctor.Asciidoctor;

import java.io.File;

class EpubProducer implements DocumentProducer {

	private final PublicationProperties properties;

	private final Asciidoctor asciidoctor;

	EpubProducer(PublicationProperties properties, Asciidoctor asciidoctor) {
		this.properties = properties;
		this.asciidoctor = asciidoctor;
	}

	@Override
	public File[] produce() {
		var attributesBuilder = this.buildCommonAttributes(this.properties.bookName(), this.properties.epub().isbn(),
				this.properties.code());
		var optionsBuilder = this.buildCommonOptions("epub3", attributesBuilder.build());
		var index = this.getIndexAdoc(this.properties.root());
		asciidoctor.convertFile(index, optionsBuilder.build());
		return new File[] { new File(index.getParentFile(), "index.epub") };
	}

}
