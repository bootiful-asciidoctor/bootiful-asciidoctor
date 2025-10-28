package bootiful.asciidoctor;

import bootiful.asciidoctor.autoconfigure.DocumentProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
class DocumentProducerStepConfiguration {

	// todo this will probably need to be revisited in the light of the AOT engine.
	@Bean
	static BeanDefinitionRegistryPostProcessor flowRegisteringBeanDefinitionRegistryPostProcessor() {
		return new DocumentProducerBeanDefinitionRegistryPostProcessor();
	}

	// this method is called in the supplier for the object, which is why its ok to work
	// with references to the other beans
	private static SimpleFlow buildFlow(BeanFactory beans, String beanName) {
		var props = beans.getBean(PipelineJobProperties.class);
		var documentProducer = beans.getBean(beanName, DocumentProducer.class);
		var jr = beans.getBean(JobRepository.class);
		var platformTransactionManager = beans.getBean(PlatformTransactionManager.class);
		var dpt = new DocumentProducerTasklet(documentProducer, props.target());
		return new FlowBuilder<SimpleFlow>(beanName + "Flow")//
				.start(new StepBuilder(beanName + DocumentProducer.class.getSimpleName() + "Step", jr)//
						.tasklet(dpt, platformTransactionManager) //
						.build() //
				) //
				.build();
	}

	static class DocumentProducerBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

		private static final Logger log = LoggerFactory
				.getLogger(DocumentProducerBeanDefinitionRegistryPostProcessor.class);

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry bdr) throws BeansException {
			log.warn("postProcessBeanDefinitionRegistry");

			if (bdr instanceof ConfigurableListableBeanFactory beans) {
				log.warn("postProcessBeanDefinitionRegistry is a {}", ConfigurableListableBeanFactory.class.getName());
				var beanNamesForType = beans.getBeanNamesForType(DocumentProducer.class);
				for (var beanName : beanNamesForType) {
					var beanFlowRegistrationBeanName = beanName + "FlowRegistration";
					var bd = new RootBeanDefinition(SimpleFlow.class, () -> buildFlow(beans, beanName));
					bd.addQualifier(new AutowireCandidateQualifier(DocumentProducerFlow.class));
					if (!beans.containsBean(beanFlowRegistrationBeanName)) {
						bdr.registerBeanDefinition(beanFlowRegistrationBeanName, bd);
					}
				}
			}

		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			log.warn("postProcessBeanFactory");
		}

	}

}
