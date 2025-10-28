package bootiful.asciidoctor;

import bootiful.asciidoctor.files.FileUtils;
import bootiful.asciidoctor.git.GitCloneCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Configuration
class GitCloneDocsStepConfiguration {

	private static final Logger log = LoggerFactory.getLogger(GitCloneDocsStepConfiguration.class);

	private final PipelineJobProperties pipelineJobProperties;

	private final GitCloneCallback cloneCallback;

	private final JobRepository jobRepository;

	private final PlatformTransactionManager ptx;

	GitCloneDocsStepConfiguration(PipelineJobProperties pipelineJobProperties, GitCloneCallback cloneCallback,
			JobRepository jobRepository, PlatformTransactionManager ptx) {
		this.pipelineJobProperties = pipelineJobProperties;
		this.cloneCallback = cloneCallback;
		this.jobRepository = jobRepository;
		this.ptx = ptx;
	}

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
					var docs = FileUtils.getDocsDirectory(pipelineJobProperties.root());
					FileUtils.resetOrRecreateDirectory(docs);
					var docsUri = URI.create(pipelineJobProperties.documentRepository().trim());
					cloneCallback.clone(docsUri, docs);
					log.info("cloned {} to {}.", docsUri, docs.getAbsolutePath());
					return RepeatStatus.FINISHED;
				}, this.ptx) //
				.build();
	}

}
