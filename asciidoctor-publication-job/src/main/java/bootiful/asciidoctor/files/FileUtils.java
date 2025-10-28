package bootiful.asciidoctor.files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;

import java.io.File;

public abstract class FileUtils {

	private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

	public static File getCodeDirectory(File root) {
		return new File(root, "code");
	}

	public static File getDocsDirectory(File root) {
		return new File(root, "docs");
	}

	public static void resetOrRecreateDirectory(File file) {
		Assert.state(!file.exists() || !file.isFile(),
				() -> "the directory " + file.getAbsolutePath() + " should not exist and it should be a directory");
		if (file.exists() && file.isDirectory()) {
			FileSystemUtils.deleteRecursively(file);
		}
		if (!file.exists()) {
			log.debug("trying to create {}.", file.getAbsolutePath());
			file.mkdirs();
		}
		Assert.state(file.exists(), () -> "the directory " + file.getAbsolutePath() + " does not exist");
	}

}
