package bootiful.asciidoctor;

import bootiful.asciidoctor.files.FileUtils;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import java.io.File;
import java.util.Map;
import java.util.Objects;

/**
 * map one property to the other so that we need to configure it but once.
 */
@Slf4j
class ForwardingEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

		var pipelineRoot = new File(Objects.requireNonNull(environment.getProperty("pipeline.job.root")));

		var docsDirectory = FileUtils.getDocsDirectory(pipelineRoot);

		var codeDirectory = FileUtils.getCodeDirectory(pipelineRoot);

		var mappings = Map.of(//
				"publication.book-name", environment.getProperty("pipeline.job.book-name"), //
				"publication.pdf.fonts", new File(docsDirectory, "/styles/pdf/fonts").getAbsolutePath(), //
				"publication.pdf.styles", new File(docsDirectory, "/styles/pdf").getAbsolutePath(), //
				"publication.root", docsDirectory.getAbsolutePath(), //
				"publication.code", codeDirectory.getAbsolutePath(), //
				"publication.target", environment.getProperty("pipeline.job.target") //
		); //

		var propertySource = new PropertySource<String>("bootiful-asciidoctor") {

			@Override
			public Object getProperty(String name) {

				if (mappings.containsKey(name)) {
					log.debug("mapping " + name + " to " + mappings.get(name));
					return mappings.get(name);
				}
				return null;
			}
		};
		environment.getPropertySources().addLast(propertySource);
	}

}
