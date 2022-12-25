package bootiful.asciidoctor.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.Asciidoctor;

import java.io.File;

@Slf4j
@RequiredArgsConstructor
class HtmlProducer implements DocumentProducer {

	@Override
	public String getType() {
		return "html";
	}

	private final PublicationProperties properties;

	private final Asciidoctor asciidoctor;

	@Override
	public File[] produce() {
		var builder = this.buildCommonAttributes(this.properties.bookName(), "(No ISBN required)",
				this.properties.code());
		var index = this.getIndexAdoc(this.properties.root());
		var rootTarget = index.getParentFile();
		if (!rootTarget.exists())
			rootTarget.mkdirs();
		log.info("being asked to generate the HTML to " + rootTarget.getAbsolutePath());
		var html = this.buildCommonOptions("html", builder.build()).toFile(new File(rootTarget, "index.html"));
		asciidoctor.convertFile(index, html.build());
		var images = new File(rootTarget, "images");
		var indexHtml = new File(rootTarget, "index.html");
		return new File[] { indexHtml, images };
	}

}
