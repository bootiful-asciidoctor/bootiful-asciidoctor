package bootiful.asciidoctor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({ GitCloneCodeStepConfiguration.class, GitCloneDocsStepConfiguration.class,
		DocumentProducerStepConfiguration.class, DocumentPublisherStepConfiguration.class, JobConfiguration.class })
@EnableConfigurationProperties(PipelineJobProperties.class)
class AsciidoctorPublicationJobAutoConfiguration {

}
