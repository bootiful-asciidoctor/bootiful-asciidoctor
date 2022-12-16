package bootiful.asciidoctor.autoconfigure;

import lombok.RequiredArgsConstructor;
import org.asciidoctor.Asciidoctor;

import java.io.File;

@RequiredArgsConstructor
class EpubProducer implements DocumentProducer {

	private final PublicationProperties properties;

	private final Asciidoctor asciidoctor;

	@Override
	public File[] produce() {
		var attributesBuilder = this.buildCommonAttributes(this.properties.getBookName(),
				this.properties.getEpub().getIsbn(), this.properties.getCode());
		var optionsBuilder = this.buildCommonOptions("epub3", attributesBuilder.build());
		var index = this.getIndexAdoc(this.properties.getRoot());
		asciidoctor.convertFile(index, optionsBuilder.build());
		return new File[] { new File(index.getParentFile(), "index.epub") };
	}

}
