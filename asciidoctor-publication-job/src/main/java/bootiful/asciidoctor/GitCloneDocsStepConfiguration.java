package bootiful.asciidoctor;

import bootiful.asciidoctor.files.FileUtils;
import bootiful.asciidoctor.git.GitCloneCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Log4j2
@Configuration
@RequiredArgsConstructor
class GitCloneDocsStepConfiguration {

	private final StepBuilderFactory stepBuilderFactory;

	private final PipelineJobProperties pipelineJobProperties;

	private final GitCloneCallback cloneCallback;

	@Bean
	Flow gitCloneDocsFlow() {
		return new FlowBuilder<Flow>("gitCloneDocsFlow")//
				.start(gitCloneDocsStep()) //
				.build();
	}

	@Bean
	Step gitCloneDocsStep() {
		return this.stepBuilderFactory //
				.get("clone-docs-step") //
				.tasklet((stepContribution, chunkContext) -> {
					var docs = FileUtils.getDocsDirectory(pipelineJobProperties.getRoot());
					FileUtils.resetOrRecreateDirectory(docs);
					var docsUri = URI.create(pipelineJobProperties.getDocumentRepository().trim());
					cloneCallback.clone(docsUri, docs);
					if (log.isDebugEnabled()) {
						log.debug("cloned " + docsUri.toString() + " to " + docs.getAbsolutePath() + '.');
					}
					return RepeatStatus.FINISHED;
				}) //
				.build();
	}

}
