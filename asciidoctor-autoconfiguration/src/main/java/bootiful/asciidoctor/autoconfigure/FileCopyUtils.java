package bootiful.asciidoctor.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public abstract class FileCopyUtils {

	private static final Logger log = LoggerFactory.getLogger(FileCopyUtils.class);

	public static void copy(InputStream i, OutputStream o) {
		try {
			org.springframework.util.FileCopyUtils.copy(i, o);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void copy(File src, File dst) {
		log.debug("copying " + src.getAbsolutePath() + " to " + dst.getAbsolutePath());
		if (src.isDirectory()) {
			Assert.isTrue(dst.exists() || dst.mkdirs(),
					dst.getAbsolutePath() + " does not exist and couldn't be created");
			var files = src.list();
			if (null == files) {
				files = new String[0];
			}
			for (var file : files) {
				var srcFile = new File(src, file);
				var destFile = new File(dst, file);
				copy(srcFile, destFile);
			}
		}
		else {
			try {
				Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			catch (IOException e) {
				ReflectionUtils.rethrowRuntimeException(e);
			}
		}
	}

}
