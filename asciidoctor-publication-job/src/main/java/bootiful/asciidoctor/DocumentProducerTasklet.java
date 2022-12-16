package bootiful.asciidoctor;

import bootiful.asciidoctor.autoconfigure.DocumentProducer;
import bootiful.asciidoctor.autoconfigure.FileCopyUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
class DocumentProducerTasklet implements Tasklet {

	private final DocumentProducer documentProducer;

	private final File target;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext context) throws Exception {
		var simpleName = this.documentProducer.getClass().getSimpleName();
		log.info("starting tasklet for " + simpleName + '.');
		var fileArray = this.documentProducer.produce();
		log.info("stopping tasklet for " + simpleName + '.');
		for (var f : fileArray) {
			this.copyToOutputDirectory(simpleName, f);
		}
		this.contributeFilesToOutput(context, fileArray);
		return RepeatStatus.FINISHED;
	}

	@SuppressWarnings("unchecked")
	private void contributeFilesToOutput(ChunkContext context, File[] fileArray) {
		if (fileArray.length == 0)
			return;

		var executionContext = context.getStepContext() //
				.getStepExecution() //
				.getJobExecution()//
				.getExecutionContext();
		if (!executionContext.containsKey("files")) {
			executionContext.put("files", new ConcurrentHashMap<String, List<File>>());
		}
		var files = Objects.requireNonNull((Map<String, List<File>>) executionContext.get("files"));
		files.put(this.documentProducer.getClass().getSimpleName(), Arrays.asList(fileArray));
	}

	@SneakyThrows
	private void copyToOutputDirectory(String type, File file) {
		var newFile = new File(new File(this.target, type), file.getName());
		if (!newFile.exists()) {
			if (newFile.isDirectory()) {
				newFile.mkdirs();
			}
			else {
				if (!newFile.getParentFile().exists())
					newFile.getParentFile().mkdirs();
			}
		}

		FileCopyUtils.copy(file, newFile);
	}

}
