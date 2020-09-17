package bootiful.asciidoctor;

import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.annotation.*;

@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Qualifier("documentProducerFlow")
@interface DocumentProducerFlow {

	String value() default "";

}
