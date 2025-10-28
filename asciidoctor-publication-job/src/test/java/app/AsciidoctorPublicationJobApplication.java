package app;

import bootiful.asciidoctor.DocumentsPublishedEvent;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.JobExecutionEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.List;

@SpringBootApplication
class AsciidoctorPublicationJobApplication {

	private static final Logger log = LoggerFactory.getLogger(AsciidoctorPublicationJobApplication.class);

	@Bean
	UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider(@Value("${GIT_USERNAME}") String user,
			@Value("${GIT_PASSWORD}") String pw) {
		return new UsernamePasswordCredentialsProvider(user, pw);
	}

	@Bean
	ApplicationListener<DocumentsPublishedEvent> documentsPublishedListener() {
		return event -> {
			log.info("Ding! The files are ready!");
			for (var entry : event.getSource().entrySet()) {
				log.info("{}={}", entry.getKey(), entry.getValue());
			}
		};
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> applicationReadyListener(Environment environment) {
		return _ -> List.of("pipeline.job.root", "publication.root", "publication.code")
				.forEach(propertyName -> log.debug(propertyName + '=' + environment.getProperty(propertyName)));
	}

	@Bean
	ApplicationListener<JobExecutionEvent> batchListener() {
		return event -> {
			var jobExecution = event.getJobExecution();
			var createTime = jobExecution.getCreateTime();
			var endTime = jobExecution.getEndTime();
			var jobName = jobExecution.getJobInstance().getJobName();
			log.info("job ({}) start time: {}", jobName, createTime);
			log.info("job ({}) stop time: {}", jobName, endTime.toString());
		};
	}

}
