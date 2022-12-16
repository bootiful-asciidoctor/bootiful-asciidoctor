package bootiful.asciidoctor;

import bootiful.asciidoctor.files.FileUtils;
import com.joshlong.git.GitUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

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
@Slf4j
@Configuration
class GitCloneCodeStepConfiguration {

	private final StepBuilder stepBuilder;

	private final TaskExecutor executor;

	private final File root;

	private final Function<URI, File> cloneFunction;

	private final PipelineJobProperties pipelineJobProperties;

	private final Collection<URI> repositories;

	private final JobRepository jobRepository;

	private final PlatformTransactionManager ptm;

	GitCloneCodeStepConfiguration(PipelineJobProperties pipelineJobProperties, StepBuilder stepBuilder,
			TaskExecutor executor, JobRepository jobRepository, PlatformTransactionManager ptm) {
		this.pipelineJobProperties = pipelineJobProperties;
		this.stepBuilder = stepBuilder;
		this.ptm = ptm;
		this.executor = executor;
		// this.maxConcurrency = pipelineJobProperties.getMaxThreadsInThreadpool();
		this.root = pipelineJobProperties.getRoot();
		this.jobRepository = jobRepository;
		this.cloneFunction = uri -> buildLocalCodeDirectoryFromGitUri(this.root, uri);
		FileUtils.resetOrRecreateDirectory(this.root);
		this.repositories = Stream //
				.of(this.pipelineJobProperties.getCodeRepositories()) //
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
	ItemWriter<URI> writer() {
		return items -> items.forEach(this::createLocalGitRepositoryFor);
	}

	@Bean
	Step gitCloneCodeStep() {
		// chunk size of 1 to ensure that we clone each repository on a separate thread.
		return new StepBuilder("clone-git-repositories", this.jobRepository)//
				.<URI, URI>chunk(1, this.ptm)//
				.reader(reader())//
				.writer(writer())//
				// .throttleLimit(this.maxConcurrency)//
				.taskExecutor(this.executor)//
				.build();
	}

	@Bean
	Flow gitCloneCodeFlow() {
		return new FlowBuilder<Flow>("gitCloneRepositoriesFlow")//
				.start(gitCloneCodeStep()) //
				.build();
	}

	@SneakyThrows
	private Git createLocalGitRepositoryFor(URI uri) {
		log.info("going to clone " + uri.toString());
		var newCloneDirectory = this.cloneFunction.apply(uri);
		log.info("the output directory will be " + newCloneDirectory.getAbsolutePath() + '.');
		FileUtils.resetOrRecreateDirectory(newCloneDirectory);
		return GitUtils.createLocalHttpGitRepository(uri, newCloneDirectory);
	}

}
