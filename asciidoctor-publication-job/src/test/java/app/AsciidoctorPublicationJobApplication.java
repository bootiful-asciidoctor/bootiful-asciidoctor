package app;

import bootiful.asciidoctor.DocumentsPublishedEvent;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.JobExecutionEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.List;

@Slf4j
@SpringBootApplication
class AsciidoctorPublicationJobApplication {

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
				log.info(entry.getKey() + '=' + entry.getValue());
			}
		};
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> applicationReadyListener(Environment environment) {
		return event -> List.of("pipeline.job.root", "publication.root", "publication.code")
				.forEach(propertyName -> log.debug(propertyName + '=' + environment.getProperty(propertyName)));
	}

	@Bean
	ApplicationListener<JobExecutionEvent> batchListener() {
		return event -> {
			var jobExecution = event.getJobExecution();
			var createTime = jobExecution.getCreateTime();
			var endTime = jobExecution.getEndTime();
			var jobName = jobExecution.getJobInstance().getJobName();
			log.info("job (" + jobName + ") start time: " + createTime.toString());
			log.info("job (" + jobName + ") stop time: " + endTime.toString());
		};
	}

}
