package bootiful.asciidoctor.publishers;

import bootiful.asciidoctor.files.ZipUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;

class ZipUtilsTest {

	@Test
	void zip() throws Exception {
		var epub = fileFromClassPathPath("files/epub/index.epub");
		var html = fileFromClassPathPath("files/html/index.html");
		var files = new File[] { epub, html };
		for (var f : files) {
			Assertions.assertTrue(f.exists(), "the file " + f.getAbsolutePath() + " does not exist!");
		}
		var zip = new File(new File(System.getenv("HOME"), "Desktop"), "files.zip");
		if (!zip.getParentFile().exists())
			zip.getParentFile().mkdirs();
		Assertions.assertFalse(zip.exists());
		ZipUtils.buildZipFileFromFiles(zip, files);
		Assertions.assertTrue(zip.exists());
		Assertions.assertTrue(zip.length() > 0);
		zip.delete();
		Assertions.assertFalse(zip.exists());

	}

	@SneakyThrows
	private File fileFromClassPathPath(String path) {
		return new ClassPathResource(path).getFile();
	}

}