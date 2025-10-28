package bootiful.asciidoctor.autoconfigure;

import org.asciidoctor.Asciidoctor;

import java.io.File;

class HtmlProducer implements DocumentProducer {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HtmlProducer.class);

	private final PublicationProperties properties;

	private final Asciidoctor asciidoctor;

	HtmlProducer(PublicationProperties properties, Asciidoctor asciidoctor) {
		this.properties = properties;
		this.asciidoctor = asciidoctor;
	}

	@Override
	public File[] produce() {
		var builder = this.buildCommonAttributes(this.properties.bookName(), "(No ISBN required)",
				this.properties.code());
		var index = this.getIndexAdoc(this.properties.root());
		var rootTarget = index.getParentFile();
		if (!rootTarget.exists())
			rootTarget.mkdirs();
		log.info("being asked to generate the HTML to {}", rootTarget.getAbsolutePath());
		var html = this.buildCommonOptions("html", builder.build()).toFile(new File(rootTarget, "index.html"));
		asciidoctor.convertFile(index, html.build());
		var images = new File(rootTarget, "images");
		var indexHtml = new File(rootTarget, "index.html");
		return new File[] { indexHtml, images };
	}

}
