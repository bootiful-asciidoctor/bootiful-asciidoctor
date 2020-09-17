package bootiful.asciidoctor.autoconfigure;

import lombok.extern.log4j.Log4j2;
import org.asciidoctor.Asciidoctor;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * todo bless this as a real test for CI
 */
@Log4j2
class PrepressPdfProducerTest {

	@Test
	@Ignore
	public void prepress() throws Exception {

		var tws = System.getenv("HOME") + "/Desktop/root";
		var file = new File(tws);
		if (!file.exists()) {
			log.info("no directory found " + file.getAbsolutePath() + '.');
			return;
		}

		var prepress = new PublicationProperties.Pdf.Prepress();
		prepress.setOptimize(true);

		var publicationProperties = new PublicationProperties();
		publicationProperties.getPdf().setEnabled(true);
		publicationProperties.getPdf().setPrepress(prepress);
		publicationProperties.setRoot(new File(tws, "/docs"));
		publicationProperties.setCode(new File(tws, "/code"));
		publicationProperties.setTarget(new File(tws, "/target"));
		publicationProperties.getPdf().setStyles(new File(tws, "/docs/styles/pdf/"));
		publicationProperties.getPdf().setFonts(new File(tws, "/docs/styles/pdf/fonts"));

		var asciidoctor = Asciidoctor.Factory.create();
		PrepressPdfProducer prepressPdfProducer = new PrepressPdfProducer(publicationProperties, asciidoctor);
		var produce = prepressPdfProducer.produce();
		Assert.assertEquals(2, produce.length);
		Assert.assertTrue(produce[1].getName().contains("optimized"));
		for (var f : produce) {
			log.info("\tpath = " + f.getAbsolutePath());
		}

	}

}