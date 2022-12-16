package bootiful.asciidoctor.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.jruby.internal.AsciidoctorCoreException;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is _super_ fragile. In order for this to work you need to provide an environment
 * variable - ${code KINDLEGEN} - where the ${code kindlegen} binary can be found. The
 * environment variable should point to the location of the `kindlegen` binary itself.
 */
@Slf4j
class MobiProducer implements DocumentProducer {

	private final PublicationProperties properties;

	private final Resource kindlegenBinary;

	private final Asciidoctor asciidoctor;

	MobiProducer(PublicationProperties properties, Asciidoctor asciidoctor, Resource kindlegenBinary) throws Exception {
		this.properties = properties;
		this.asciidoctor = asciidoctor;
		this.kindlegenBinary = kindlegenBinary;
		this.installKindlegen();
	}

	@Override
	public File[] produce() {

		var indexAdoc = getIndexAdoc(this.properties.getRoot());
		var bookName = this.properties.getBookName();
		var attributesBuilder = this
				.buildCommonAttributes(bookName, this.properties.getMobi().getIsbn(), this.properties.getCode())
				.attribute("ebook-format", "kf8");
		var optionsBuilder = this.buildCommonOptions("epub3", attributesBuilder.build());
		try {
			asciidoctor.convertFile(indexAdoc, optionsBuilder.build());
		}
		catch (AsciidoctorCoreException ace) {
			log.warn("Exception when producing the .mobi. The cause is " + ace.getMessage()
					+ ". If the error says 'No child processes' but you " + "see a resulting .mobi in "
					+ this.properties.getRoot().getParentFile().getAbsolutePath()
					+ "then don't worry about the error.");
		}
		catch (Throwable t) {
			log.warn("something went wrong! ", t);
		}
		return new File[] { new File(this.properties.getRoot(), "index-kf8.epub"),
				new File(this.properties.getRoot(), "index.mobi") };
	}

	private void installKindlegen() throws Exception {
		var binaryLocation = this.properties.getMobi().getKindlegen().getBinaryLocation();
		var directoryForKindlegen = binaryLocation.getParentFile();
		if (!directoryForKindlegen.exists()) {
			directoryForKindlegen.mkdirs();
		}
		Assert.state(directoryForKindlegen.exists(),
				() -> "the directory " + directoryForKindlegen.getAbsolutePath() + " does not exist!");
		Assert.state(binaryLocation.exists() && binaryLocation.isFile() || !binaryLocation.exists(), () -> "the path "
				+ binaryLocation.getAbsolutePath() + " must not exist or it must be a (writable) file");
		try (var inputStream = this.kindlegenBinary.getInputStream();
				var outputStream = new FileOutputStream(binaryLocation)) {
			FileCopyUtils.copy(inputStream, outputStream);
		}
		Assert.state(0 == Runtime.getRuntime().exec("chmod a+x " + binaryLocation.getAbsolutePath()).waitFor(),
				"couldn't make the kindlegen binary executable");
	}

}
