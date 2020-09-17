package bootiful.asciidoctor;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@RequiredArgsConstructor
class DocumentPublisherTasklet implements ApplicationEventPublisherAware, Tasklet {

	private final DocumentPublisher publisher;

	private final AtomicReference<ApplicationEventPublisher> applicationEventPublisher = new AtomicReference<>();

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext context) throws Exception {

		var executionContext = context.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
		var files = Objects.requireNonNull((Map<String, Collection<File>>) executionContext.get("files"));
		var msg = "using " + DocumentPublisher.class.getSimpleName() + " instance "
				+ this.publisher.getClass().getName() + '.' + " " + "There are " + files + " to process. " + files;
		log.info(msg);
		this.publisher.publish(files);
		this.applicationEventPublisher.get().publishEvent(new DocumentsPublishedEvent(files));
		return RepeatStatus.FINISHED;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher.set(applicationEventPublisher);
	}

}
