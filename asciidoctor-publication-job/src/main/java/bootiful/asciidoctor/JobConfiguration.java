package bootiful.asciidoctor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Slf4j
@Configuration
@RequiredArgsConstructor
class JobConfiguration {

	private final static String SPLIT_FLOW_ID = "splitFlow";

	@Bean
	@Qualifier(SPLIT_FLOW_ID)
	Flow splitFlow(TaskExecutor executor, @DocumentProducerFlow Flow[] flowList) {
		return new FlowBuilder<Flow>(SPLIT_FLOW_ID)//
				.split(executor) //
				.add(flowList) //
				.build();
	}

	@Bean
	Job publicationJob(JobRepository jobRepository, GitCloneCodeStepConfiguration code,
			GitCloneDocsStepConfiguration docs, DocumentPublisherStepConfiguration publishing,
			@Qualifier(SPLIT_FLOW_ID) Flow flow) {
		return new JobBuilder("publicationJob", jobRepository)//
				.incrementer(new RunIdIncrementer()) //
				.start(docs.docsFlow()) //
				.next(code.codeFlow()) //
				.next(flow) //
				.next(publishing.publicationFlow()) //
				.build() //
				.build();
	}

}
