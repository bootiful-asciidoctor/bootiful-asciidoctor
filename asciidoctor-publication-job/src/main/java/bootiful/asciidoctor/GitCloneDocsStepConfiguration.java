package bootiful.asciidoctor;

import bootiful.asciidoctor.files.FileUtils;
import bootiful.asciidoctor.git.GitCloneCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.URI;

@Slf4j
@Configuration
@RequiredArgsConstructor
class GitCloneDocsStepConfiguration {

	private final PipelineJobProperties pipelineJobProperties;

	private final GitCloneCallback cloneCallback;

	private final JobRepository jobRepository;

	private final PlatformTransactionManager ptx;

	@Bean
	Flow docsFlow() {
		return new FlowBuilder<Flow>("gitCloneDocsFlow")//
				.start(gitCloneDocsStep()) //
				.build();
	}

	@Bean
	Step gitCloneDocsStep() {
		return new StepBuilder("gitCloneDocsStep", this.jobRepository) //
				.tasklet((stepContribution, chunkContext) -> {
					log.info("going to clone");
					var docs = FileUtils.getDocsDirectory(pipelineJobProperties.root());
					FileUtils.resetOrRecreateDirectory(docs);
					var docsUri = URI.create(pipelineJobProperties.documentRepository().trim());
					cloneCallback.clone(docsUri, docs);
					log.info("cloned " + docsUri + " to " + docs.getAbsolutePath() + '.');
					return RepeatStatus.FINISHED;
				}, this.ptx) //
				.build();
	}

}
