package bootiful.asciidoctor.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.Asciidoctor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(PublicationProperties.class)
@ConditionalOnClass(Asciidoctor.class)
class AsciidoctorPublicationAutoConfiguration {

	@Bean
	@ConditionalOnProperty(value = "publication.epub.enabled", havingValue = "true", matchIfMissing = false)
	DocumentProducer epubProducer(PublicationProperties pp, Asciidoctor asciidoctor) {
		return new EpubProducer(pp, asciidoctor);
	}

	/**
	 * this only works if you've opted into it <em>and</em> you're running on Linux
	 */
	@ConditionalOnProperty(value = "publication.mobi.enabled", havingValue = "true", matchIfMissing = false)
	@Bean
	DocumentProducer mobiProducer(PublicationProperties pp, @Value("classpath:/kindlegen") Resource kindlegen,
			Asciidoctor asciidoctor) {
		Assert.isTrue(System.getProperty("os.name").toLowerCase().contains("linux"),
				"this needs to be running on Linux");
		return new MobiProducer(pp, asciidoctor, kindlegen);

	}

	@ConditionalOnProperty(value = "publication.html.enabled", havingValue = "true", matchIfMissing = true)
	@Bean
	DocumentProducer htmlProducer(PublicationProperties pp, Asciidoctor asciidoctor) {
		return new HtmlProducer(pp, asciidoctor);
	}

	@ConditionalOnProperty(value = "publication.pdf.screen.enabled", havingValue = "true", matchIfMissing = true)
	@Bean
	DocumentProducer screenPdfProducer(PublicationProperties pp, Asciidoctor asciidoctor) {
		return new ScreenPdfProducer(pp, asciidoctor);
	}

	@Bean
	@ConditionalOnProperty(value = "publication.pdf.prepress.enabled", havingValue = "true", matchIfMissing = true)
	DocumentProducer prepressPdfProducer(PublicationProperties pp, Asciidoctor asciidoctor) {
		return new PrepressPdfProducer(pp, asciidoctor);
	}

	@Bean
	Asciidoctor asciidoctor(ObjectProvider<AsciidoctorCustomizer> customizers) {
		var asciidoctor = Asciidoctor.Factory.create();
		customizers.orderedStream().forEach(ac -> ac.customize(asciidoctor));
		return asciidoctor;
	}

}
/*
 *
 * @Slf4j
 *
 * @RequiredArgsConstructor class EnabledDelegatingDocumentProducer implements
 * DocumentProducer {
 *
 * private final Supplier<DocumentProducer> dp;
 *
 * private final String name;
 *
 * private final boolean enabled;
 *
 * @Override public File[] produce() throws Exception { if (!this.enabled) { if
 * (log.isDebugEnabled()) log.debug("not running " + name + " as it is not enabled.");
 * return new File[0]; } log.info("running " + this.name); return this.dp.get().produce();
 * }
 *
 * }
 */
