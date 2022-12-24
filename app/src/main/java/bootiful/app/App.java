package bootiful.app;

import bootiful.asciidoctor.DocumentsPublishedEvent;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jruby.util.log.LoggerFactory;
import org.jruby.util.log.StandardErrorLogger;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.JobExecutionEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.env.Environment;

import java.util.List;

@Slf4j
@ImportRuntimeHints(App.Hints.class)
@SpringBootApplication
public class App {

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Bean
	UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider(@Value("${GIT_USERNAME}") String user,
			@Value("${GIT_PASSWORD}") String pw) {
		return new UsernamePasswordCredentialsProvider(user, pw);
	}

	@Bean
	ApplicationListener<DocumentsPublishedEvent> documentsPublishedListener() {
		return event -> event.getSource().forEach((key, value) -> log.info("file " + key + " is ready: " + value));
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
			log.info("job (" + jobName + ") start time: " + createTime);
			log.info("job (" + jobName + ") stop time: " + endTime);
		};
	}

	static class Hints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

			// var natives = new String[] { "java.lang.invoke.MethodHandleNatives" };
			// for (var c : natives)
			// hints.jni().registerType(TypeReference.of(c), MemberCategory.values());

			var reflection = new Class[] { LoggerFactory.class, StandardErrorLogger.class, };
			for (var c : reflection)
				hints.reflection().registerType(c, MemberCategory.values());

			var serialization = new String[] { "java.io.File[]", "java.lang.String", "java.util.Arrays$ArrayList",
					"java.util.HashMap", "java.util.concurrent.ConcurrentHashMap",
					"java.util.concurrent.ConcurrentHashMap$Segment",
					"java.util.concurrent.ConcurrentHashMap$Segment[]",
					"java.util.concurrent.locks.AbstractOwnableSynchronizer",
					"java.util.concurrent.locks.AbstractQueuedSynchronizer", "java.util.concurrent.locks.ReentrantLock",
					"java.util.concurrent.locks.ReentrantLock$NonfairSync",
					"java.util.concurrent.locks.ReentrantLock$Sync.clas" };
			for (var c : serialization)
				hints.serialization().registerType(TypeReference.of(c));
		}

	}

}
