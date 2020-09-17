package bootiful.asciidoctor.autoconfigure;

import org.asciidoctor.Asciidoctor;

@FunctionalInterface
public interface AsciidoctorCustomizer {

	void customize(Asciidoctor a);

}
