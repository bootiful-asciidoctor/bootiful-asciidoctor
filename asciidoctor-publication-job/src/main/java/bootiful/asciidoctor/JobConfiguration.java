package bootiful.asciidoctor;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

import java.util.Arrays;
import java.util.Collection;

@Log4j2
@Configuration
class JobConfiguration {

	private final static String SPLIT_FLOW_ID = "splitFlow";

	private final GitCloneCodeStepConfiguration gitCloneCodeStepConfiguration;

	private final GitCloneDocsStepConfiguration gitCloneDocsStepConfiguration;

	private final DocumentPublisherStepConfiguration documentPublisherStepConfiguration;

	private final JobBuilderFactory jobBuilderFactory;

	private final TaskExecutor executor;

	JobConfiguration(GitCloneCodeStepConfiguration gitCloneCodeStepConfiguration,
			GitCloneDocsStepConfiguration gitCloneDocsStepConfiguration,
			DocumentPublisherStepConfiguration documentPublisherStepConfiguration, JobBuilderFactory jobBuilderFactory,
			TaskExecutor executor) {
		this.gitCloneDocsStepConfiguration = gitCloneDocsStepConfiguration;
		this.gitCloneCodeStepConfiguration = gitCloneCodeStepConfiguration;
		this.jobBuilderFactory = jobBuilderFactory;
		this.documentPublisherStepConfiguration = documentPublisherStepConfiguration;
		this.executor = executor;
	}

	@Bean
	@Qualifier(SPLIT_FLOW_ID)
	Flow splitFlow(@DocumentProducerFlow Flow[] flowList) {
		Collection<Flow> values = Arrays.asList(flowList);
		Flow[] flows = values.toArray(new Flow[0]);
		return new FlowBuilder<Flow>(SPLIT_FLOW_ID).split(this.executor).add(flows).build();
	}

	@Bean
	Job job(@Qualifier(SPLIT_FLOW_ID) Flow flow) {
		return this.jobBuilderFactory//
				.get("asciidoctor-publication-job")//
				.incrementer(new RunIdIncrementer()) //
				.start(this.gitCloneDocsStepConfiguration.gitCloneDocsFlow()) //
				.next(this.gitCloneCodeStepConfiguration.gitCloneCodeFlow()) //
				.next(flow) //
				.next(this.documentPublisherStepConfiguration.documentPublisherStep()).build() //
				.build();
	}

}
