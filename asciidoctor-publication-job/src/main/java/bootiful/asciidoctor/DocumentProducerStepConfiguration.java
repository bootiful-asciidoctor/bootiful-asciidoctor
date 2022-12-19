package bootiful.asciidoctor;

import bootiful.asciidoctor.autoconfigure.DocumentProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
class DocumentProducerStepConfiguration {

	// todo this will probably need to be revisited in the light of the AOT engine.
	// could i use a
	@Bean
	static BeanFactoryPostProcessor flowRegisteringBeanFactoryPostProcessor() {
		return beans -> {
			if (beans instanceof BeanDefinitionRegistry bdr) {
				var beanNamesForType = beans.getBeanNamesForType(DocumentProducer.class);
				for (var beanName : beanNamesForType) {
					var beanDefinition = BeanDefinitionBuilder//
							.genericBeanDefinition(Flow.class, () -> buildFlow(beans, beanName))//
							.getBeanDefinition();
					beanDefinition.addQualifier(new AutowireCandidateQualifier(DocumentProducerFlow.class));
					bdr.registerBeanDefinition(beanName + "FlowRegistration", beanDefinition);
				}
			} //
			else {
				log.error("the BeanFactory is not an instance of " + BeanDefinitionRegistry.class.getName() + ". This "
						+ BeanFactoryPostProcessor.class.getName() + " can not be used.");
			}
		};
	}

	// this method is called in the supplier for the object, which is why its ok to work
	// with references to the other beans
	private static Flow buildFlow(BeanFactory beans, String beanName) {
		var props = beans.getBean(PipelineJobProperties.class);
		var documentProducer = beans.getBean(beanName, DocumentProducer.class);
		var jr = beans.getBean(JobRepository.class);
		var platformTransactionManager = beans.getBean(PlatformTransactionManager.class);
		var dpt = new DocumentProducerTasklet(documentProducer, props.target());
		return new FlowBuilder<Flow>(beanName + "Flow")//
				.start(new StepBuilder(beanName + DocumentProducer.class.getSimpleName() + "Step", jr)//
						.tasklet(dpt, platformTransactionManager) //
						.build() //
				) //
				.build();
	}

}
