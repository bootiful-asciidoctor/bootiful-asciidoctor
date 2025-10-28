package bootiful.asciidoctor.git;

import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration to customize how the pipeline handles interactions with Git when
 * needed.
 */
@AutoConfiguration
class GitAutoConfiguration {

	/**
	 * With what credentials do we clone the repositories? If any of them are private,
	 * you'll want to provide a bean of type {@link TransportConfigCallback} somewhere in
	 * your application context so that the client knows how to authenticate.
	 */
	@Configuration
	@ConditionalOnMissingBean(GitCloneCallback.class)
	static class GitCloneCallbackAutoConfiguration {

		@Bean
		@ConditionalOnBean(TransportConfigCallback.class)
		GitCloneCallback transportConfigCallbackGitCloneCallback(TransportConfigCallback transportConfigCallback) {
			return new TransportConfigCallbackGitCloneCallback(transportConfigCallback);
		}

		@Bean
		@ConditionalOnBean(CredentialsProvider.class)
		GitCloneCallback credentialsProviderGitCloneCallback(CredentialsProvider credentialsProvider) {
			return new CredentialsProviderGitCloneCallback(credentialsProvider);
		}

		@Bean
		@ConditionalOnMissingBean({ TransportConfigCallback.class, CredentialsProvider.class })
		GitCloneCallback publicGitRepositoryGitCloneCallback() {
			return new PublicGitCloneCallback();
		}

	}

	/**
	 * with what credentials will we push changes as, for example, we have to do when
	 * pushing changes in the an implementation of
	 * {@link bootiful.asciidoctor.DocumentPublisher} that talks to a Git repository?
	 */
	@Configuration
	@ConditionalOnMissingBean(GitPushCallback.class)
	static class GitPushCallbackAutoConfiguration {

		@Bean
		@ConditionalOnBean(TransportConfigCallback.class)
		GitPushCallback sshGitPushCallback(TransportConfigCallback configCallback) {
			return new TransportConfigCallbackGitPushCallback(configCallback);
		}

		@Bean
		@ConditionalOnBean(CredentialsProvider.class)
		GitPushCallback httpGitPushCallback(CredentialsProvider credentialsProvider) {
			return new CredentialsProviderGitPushCallback(credentialsProvider);
		}

	}

}
