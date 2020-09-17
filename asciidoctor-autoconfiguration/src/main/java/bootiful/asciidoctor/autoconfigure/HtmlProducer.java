package bootiful.asciidoctor.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.asciidoctor.Asciidoctor;

import java.io.File;

@Log4j2
@RequiredArgsConstructor
class HtmlProducer implements DocumentProducer {

	private final PublicationProperties properties;

	private final Asciidoctor asciidoctor;

	@Override
	public File[] produce() {
		var builder = this.buildCommonAttributes(this.properties.getBookName(), "(No ISBN required)",
				this.properties.getCode());
		var index = this.getIndexAdoc(this.properties.getRoot());
		var rootTarget = index.getParentFile();// new File(this.properties.getTarget(),
		// "html-build-output");
		if (!rootTarget.exists()) {
			rootTarget.mkdirs();
		}
		log.info("being asked to generate the HTML to " + rootTarget.getAbsolutePath());
		var html = this.buildCommonOptions("html", builder) //
				.toFile(new File(rootTarget, "index.html"));
		asciidoctor.convertFile(index, html);
		var images = new File(rootTarget, "images");
		var indexHtml = new File(rootTarget, "index.html");
		return new File[] { indexHtml, images };
	}

}
