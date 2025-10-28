package bootiful.asciidoctor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

class DocumentPublisherTasklet implements ApplicationEventPublisherAware, Tasklet {

	private static final Logger log = LoggerFactory.getLogger(DocumentPublisherTasklet.class);

	private final DocumentPublisher publisher;

	private final AtomicReference<ApplicationEventPublisher> applicationEventPublisher = new AtomicReference<>();

	DocumentPublisherTasklet(DocumentPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	@SuppressWarnings("unchecked")
	public RepeatStatus execute(StepContribution contribution, ChunkContext context) throws Exception {
		Assert.notNull(this.applicationEventPublisher.get(), "the applicationEventPublisher must be non-null");
		var executionContext = context.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
		var files = Objects.requireNonNull((Map<String, Collection<File>>) executionContext.get("files"));
		log.info("using {} instance {}. There are {} to process. {}", DocumentPublisher.class.getSimpleName(),
				this.publisher.getClass().getName(), files, files);
		this.publisher.publish(files);
		this.applicationEventPublisher.get().publishEvent(new DocumentsPublishedEvent(files));
		return RepeatStatus.FINISHED;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher.set(applicationEventPublisher);
	}

}
