package bootiful.asciidoctor;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
class DocumentPublisherStepConfiguration {

	private final StepBuilderFactory stepBuilderFactory;

	private final ObjectProvider<DocumentPublisher> documentPublishers;

	@Bean
	Step documentPublisherStep() {
		return this.stepBuilderFactory //
				.get("documentPublisherStep")//
				.tasklet(documentPublisherTasklet()) //
				.build();
	}

	@Bean
	DocumentPublisherTasklet documentPublisherTasklet() {
		var cdp = new CompositeDocumentPublisher(
				this.documentPublishers.orderedStream().toArray(DocumentPublisher[]::new));
		return new DocumentPublisherTasklet(cdp);
	}

}
