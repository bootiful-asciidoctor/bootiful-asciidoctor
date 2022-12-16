package bootiful.asciidoctor;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@Import({ GitCloneCodeStepConfiguration.class, GitCloneDocsStepConfiguration.class,
		DocumentProducerStepConfiguration.class, DocumentPublisherStepConfiguration.class, JobConfiguration.class })
@EnableBatchProcessing
@ConditionalOnProperty(prefix = "pipeline.job", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PipelineJobProperties.class)
class AsciidoctorPublicationJobAutoConfiguration {

	@Bean
	TaskExecutor taskExecutor(PipelineJobProperties properties) {
		var executor = Executors.newFixedThreadPool(properties.getMaxThreadsInThreadpool());
		return new ConcurrentTaskExecutor(executor);
	}

}
