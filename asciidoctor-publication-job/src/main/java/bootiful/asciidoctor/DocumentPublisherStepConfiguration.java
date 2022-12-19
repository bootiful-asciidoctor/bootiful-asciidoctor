package bootiful.asciidoctor;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
class DocumentPublisherStepConfiguration {

	private final ObjectProvider<DocumentPublisher> publishers;

	private final JobRepository repository;

	private final PlatformTransactionManager transactionManager;

	@Bean
	Step documentPublisherStep() {
		return new StepBuilder("documentPublisherStep", this.repository)//
				.tasklet(this.documentPublisherTasklet(), this.transactionManager) //
				.build();
	}

	@Bean
	Flow publicationFlow() {
		return new FlowBuilder<Flow>("documentPublisherFlow").start(documentPublisherStep()).build();
	}

	@Bean
	DocumentPublisherTasklet documentPublisherTasklet() {
		var cdp = new CompositeDocumentPublisher(this.publishers.orderedStream().toArray(DocumentPublisher[]::new));
		return new DocumentPublisherTasklet(cdp);
	}

}
