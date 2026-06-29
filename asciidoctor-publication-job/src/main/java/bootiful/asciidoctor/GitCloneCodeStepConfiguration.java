package bootiful.asciidoctor;

import bootiful.asciidoctor.files.FileUtils;
import bootiful.asciidoctor.git.GitCloneCallback;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This step clones all the Git repositories required to satisfy any includes in a `.adoc`
 * file into the root folder.
 * <p>
 * Todo: rework this so that the cloning supports SSH as well as HTTPS Todo: rewrite this
 * so that there's special handling for the docs repository which needs to be under a
 * well-known folder
 */
@Configuration
class GitCloneCodeStepConfiguration {

	private static final Logger log = LoggerFactory.getLogger(GitCloneCodeStepConfiguration.class);

	private final AsyncTaskExecutor executor;

	private final File root;

	private final Function<URI, File> cloneFunction;

	private final Collection<URI> repositories;

	private final JobRepository jobRepository;

	GitCloneCodeStepConfiguration(PipelineJobProperties pipelineJobProperties,
			@Qualifier("applicationTaskExecutor") AsyncTaskExecutor executor, JobRepository jobRepository) {
		this.executor = executor;
		this.root = pipelineJobProperties.root();
		this.jobRepository = jobRepository;
		this.cloneFunction = uri -> buildLocalCodeDirectoryFromGitUri(this.root, uri);
		FileUtils.resetOrRecreateDirectory(this.root);
		this.repositories = Stream //
			.of(pipelineJobProperties.codeRepositories()) //
			.map(String::trim) //
			.map(URI::create)//
			.collect(Collectors.toCollection(ConcurrentSkipListSet::new));
	}

	protected File buildLocalCodeDirectoryFromGitUri(File root, URI uri) {
		var child = uri.getPath().split("\\.")[0];
		var chopPoint = child.lastIndexOf('/');
		if (chopPoint > 0) {
			child = child.substring(chopPoint + 1);
		}
		return new File(FileUtils.getCodeDirectory(root), child);
	}

	@Bean
	ItemReader<URI> reader() {
		return new ConcurrentIteratorItemReader<>(this.repositories);
	}

	@Bean
	ItemWriter<URI> writer(@Nullable GitCloneCallback cloneCallback) {
		return items -> items.forEach(uri -> createLocalGitRepositoryFor(cloneCallback, uri));
	}

	@Bean
	Step gitCloneCodeStep(@Nullable GitCloneCallback gitCloneCallback) {
		// chunk size of 1 to ensure that we clone each repository on a separate thread.
		return new StepBuilder("clone-git-repositories", this.jobRepository)//
			.<URI, URI>chunk(1)//
			.reader(reader())//
			.writer(writer(gitCloneCallback))//
			.taskExecutor(this.executor)//
			.build();
	}

	@Bean
	Flow codeFlow(@Nullable GitCloneCallback gitCloneCallback) {
		return new FlowBuilder<Flow>("gitCloneRepositoriesFlow")//
			.start(gitCloneCodeStep(gitCloneCallback)) //
			.build();
	}

	private void createLocalGitRepositoryFor(GitCloneCallback cloneCallback, URI uri) {
		var newCloneDirectory = this.cloneFunction.apply(uri);
		CloneUtils.doClone(cloneCallback, uri, newCloneDirectory);
	}

}
