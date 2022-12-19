package bootiful.asciidoctor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.util.concurrent.Executors;

@AutoConfiguration
@Import({ GitCloneCodeStepConfiguration.class, GitCloneDocsStepConfiguration.class,
		DocumentProducerStepConfiguration.class, DocumentPublisherStepConfiguration.class,
		BootifulAsciidoctorJobConfiguration.class })
@EnableConfigurationProperties(PipelineJobProperties.class)
class AsciidoctorPublicationJobAutoConfiguration {

	@Bean
	TaskExecutor taskExecutor(PipelineJobProperties properties) {
		var nThreads = properties.maxThreadsInThreadpool() == 0 ? Runtime.getRuntime().availableProcessors()
				: properties.maxThreadsInThreadpool();
		var executor = Executors.newFixedThreadPool(nThreads);
		return new ConcurrentTaskExecutor(executor);
	}

}
