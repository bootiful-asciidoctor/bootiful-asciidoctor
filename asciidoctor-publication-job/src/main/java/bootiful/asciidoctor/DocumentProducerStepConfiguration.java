package bootiful.asciidoctor;

import bootiful.asciidoctor.autoconfigure.DocumentProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
@RequiredArgsConstructor
class DocumentProducerStepConfiguration {

	@Bean
	static BeanFactoryPostProcessor flowRegisteringBeanFactoryPostProcessor() {
		return beans -> {
			var listableBeanFactory = (DefaultListableBeanFactory) beans;
			var beanNamesForType = beans.getBeanNamesForType(DocumentProducer.class);
			for (var beanName : beanNamesForType) {
				var beanDefinition = BeanDefinitionBuilder
						.genericBeanDefinition(Flow.class, () -> buildFlow(listableBeanFactory, beanName))
						.getBeanDefinition();
				beanDefinition.addQualifier(new AutowireCandidateQualifier(DocumentProducerFlow.class));
				listableBeanFactory.registerBeanDefinition(beanName + "FlowRegistration", beanDefinition);
			}
		};
	}

	private static Flow buildFlow(DefaultListableBeanFactory listableBeanFactory, String beanName) {
		var sbf = listableBeanFactory.getBean(StepBuilderFactory.class);
		var props = listableBeanFactory.getBean(PipelineJobProperties.class);
		var documentProducer = listableBeanFactory.getBean(beanName, DocumentProducer.class);
		var dpt = new DocumentProducerTasklet(documentProducer, props.getTarget());
		return new FlowBuilder<Flow>(beanName + "Flow")//
				.start(sbf //
						.get(beanName + DocumentProducer.class.getSimpleName() + "Step")//
						.tasklet(dpt) //
						.build() //
				) //
				.build();
	}

}
