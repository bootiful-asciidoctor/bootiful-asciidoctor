package bootiful.asciidoctor.autoconfigure;

import lombok.extern.log4j.Log4j2;
import org.asciidoctor.Asciidoctor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Log4j2
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PublicationProperties.class)
@ConditionalOnClass(Asciidoctor.class)
class AsciidoctorPublicationAutoConfiguration {

	@Bean
	@ConditionalOnProperty(name = "publication.epub.enabled", havingValue = "true", matchIfMissing = true)
	EpubProducer epubProducer(PublicationProperties pp, Asciidoctor asciidoctor) {
		return new EpubProducer(pp, asciidoctor);
	}

	/**
	 * this only works if you've opted into it <em>and</em> you're running on Linux
	 */
	@Bean
	@Conditional(LinuxCondition.class)
	@ConditionalOnProperty(name = "publication.mobi.enabled", havingValue = "true", matchIfMissing = false)
	MobiProducer mobiProducer(PublicationProperties pp, @Value("classpath:/kindlegen") Resource kindlegen,
			Asciidoctor asciidoctor) throws Exception {
		return new MobiProducer(pp, asciidoctor, kindlegen);
	}

	@Bean
	@ConditionalOnProperty(name = "publication.html.enabled", havingValue = "true", matchIfMissing = true)
	HtmlProducer htmlProducer(PublicationProperties pp, Asciidoctor asciidoctor) {
		return new HtmlProducer(pp, asciidoctor);
	}

	@Bean
	@ConditionalOnProperty(name = "publication.pdf.screen.enabled", havingValue = "true", matchIfMissing = true)
	ScreenPdfProducer screenPdfProducer(PublicationProperties pp, Asciidoctor asciidoctor) {
		return new ScreenPdfProducer(pp, asciidoctor);
	}

	@Bean
	@ConditionalOnProperty(name = "publication.runner.enabled", havingValue = "true", matchIfMissing = false)
	DocumentProducerProcessor documentProducerProcessor(Asciidoctor ad, ObjectProvider<DocumentProducer> dps,
			PublicationProperties pp) {
		var array = dps.stream().toArray(DocumentProducer[]::new);
		return new DocumentProducerProcessor(ad, array, pp);
	}

	@Bean
	Asciidoctor asciidoctor(ObjectProvider<AsciidoctorCustomizer> customizers) {
		var asciidoctor = Asciidoctor.Factory.create();
		customizers.orderedStream().forEach(ac -> ac.customize(asciidoctor));
		return asciidoctor;
	}

	@Bean
	@ConditionalOnProperty(name = "publication.pdf.prepress.enabled", havingValue = "true", matchIfMissing = true)
	PrepressPdfProducer prepressPdfProducer(PublicationProperties pp, Asciidoctor asciidoctor) {
		return new PrepressPdfProducer(pp, asciidoctor);
	}

}

@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class LinuxCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return System.getProperty("os.name").toLowerCase().contains("linux");
	}

}
