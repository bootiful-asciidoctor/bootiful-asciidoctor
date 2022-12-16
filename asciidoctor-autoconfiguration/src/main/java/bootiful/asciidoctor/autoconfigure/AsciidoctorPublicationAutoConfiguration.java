package bootiful.asciidoctor.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.Asciidoctor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.function.Supplier;

// todo wrap all of these {@link DocumentProducer} implementations in a delegating implementation that involves the real thing IF the '.enabled' property is true. otherwise, NO-OP.
// we cant use the @conditionalOnProperty anymore as the bean graph is fixed in an AOT situation
@Slf4j
@Configuration
@EnableConfigurationProperties(PublicationProperties.class)
@ConditionalOnClass(Asciidoctor.class)
class AsciidoctorPublicationAutoConfiguration {

	private static String nameFor(Class<? extends DocumentProducer> clzz) {
		return clzz.getName();
	}

	@Bean
	DocumentProducer epubProducer(PublicationProperties pp, Asciidoctor asciidoctor) {
		return new EnabledDelegatingDocumentProducer(() -> new EpubProducer(pp, asciidoctor),
				nameFor(EpubProducer.class), pp.epub() != null && pp.epub().enabled());
	}

	/**
	 * this only works if you've opted into it <em>and</em> you're running on Linux
	 */
	@Bean
	DocumentProducer mobiProducer(PublicationProperties pp, @Value("classpath:/kindlegen") Resource kindlegen,
			Asciidoctor asciidoctor) throws Exception {
		var linux = System.getProperty("os.name").toLowerCase().contains("linux");
		return new EnabledDelegatingDocumentProducer(() -> new MobiProducer(pp, asciidoctor, kindlegen),
				nameFor(MobiProducer.class), linux && pp.mobi() != null && pp.mobi().enabled());
	}

	@Bean
	DocumentProducer htmlProducer(PublicationProperties pp, Asciidoctor asciidoctor) {
		return new EnabledDelegatingDocumentProducer(() -> new HtmlProducer(pp, asciidoctor),
				nameFor(HtmlProducer.class), pp.html() != null && pp.html().enabled());
	}

	@Bean
	DocumentProducer screenPdfProducer(PublicationProperties pp, Asciidoctor asciidoctor) {
		return new EnabledDelegatingDocumentProducer(() -> new ScreenPdfProducer(pp, asciidoctor),
				nameFor(ScreenPdfProducer.class),
				pp.pdf() != null && pp.pdf().screen() != null && pp.pdf().screen().enabled());
	}

	@Bean
	DocumentProducer prepressPdfProducer(PublicationProperties pp, Asciidoctor asciidoctor) {
		return new EnabledDelegatingDocumentProducer(() -> new PrepressPdfProducer(pp, asciidoctor),
				nameFor(PrepressPdfProducer.class),
				pp.pdf() != null && pp.pdf().prepress() != null && pp.pdf().prepress().enabled());
	}

	@Bean
	DocumentProducerProcessor documentProducerProcessor(ObjectProvider<DocumentProducer> dps,
			PublicationProperties pp) {
		var array = dps.stream().toArray(DocumentProducer[]::new);
		return new DocumentProducerProcessor(array, pp);
	}

	@Bean
	Asciidoctor asciidoctor(ObjectProvider<AsciidoctorCustomizer> customizers) {
		var asciidoctor = Asciidoctor.Factory.create();
		customizers.orderedStream().forEach(ac -> ac.customize(asciidoctor));
		return asciidoctor;
	}

}

@Slf4j
@RequiredArgsConstructor
class EnabledDelegatingDocumentProducer implements DocumentProducer {

	private final Supplier<DocumentProducer> dp;

	private final String name;

	private final boolean enabled;

	@Override
	public File[] produce() throws Exception {
		if (!this.enabled) {
			if (log.isDebugEnabled())
				log.debug("not running " + name + " as it is not enabled.");
			return new File[0];
		}
		log.info("running " + this.name);
		return this.dp.get().produce();
	}

}