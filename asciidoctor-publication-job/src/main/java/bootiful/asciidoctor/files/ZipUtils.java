package bootiful.asciidoctor.files;

import org.springframework.util.Assert;

import java.io.*;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Zip files are a common way to communicate the aggregation of the output of the
 * pipeline. This class supports easy archival.
 */
public abstract class ZipUtils {

	public static void buildZipFileFromDirectory(File zipFile, File directory) {
		zip(zipFile, directory, Objects.requireNonNull(directory.listFiles()));
	}

	public static void buildZipFileFromFiles(File zipFile, File[] fileList) {
		zip(zipFile, getCommonDirectoryFor(fileList), fileList);
	}

	private static void zip(File zipFile, File base, File[] fileList) {
		var buffer = new byte[1024];
		try (var fos = new FileOutputStream(zipFile); var zos = new ZipOutputStream(fos)) {
			for (var file : fileList) {
				var path = file.getAbsolutePath();
				var basePath = base.getAbsolutePath();
				Assert.state(path.startsWith(basePath),
						() -> String.format("the basePath (%s) should be a part of the path (%s)", basePath, path));
				var ze = new ZipEntry(path.substring(basePath.length()));
				zos.putNextEntry(ze);
				try (var in = new FileInputStream(file)) {
					var len = -1;
					while ((len = in.read(buffer)) > 0) {
						zos.write(buffer, 0, len);
					}
					zos.flush();
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static File getCommonDirectoryFor(File[] files) {
		var stringsArray = Stream//
				.of(files)//
				.map(File::getAbsolutePath)//
				.toArray(String[]::new);
		var commonRoot = getLongestCommonPrefix(stringsArray);
		var root = new File(commonRoot);
		while (!root.isDirectory()) {
			root = root.getParentFile();
		}
		return root;
	}

	private static String getLongestCommonPrefix(String[] s) {
		var k = s[0].length();
		for (var i = 1; i < s.length; i++) {
			k = Math.min(k, s[i].length());
			for (var j = 0; j < k; j++)
				if (s[i].charAt(j) != s[0].charAt(j)) {
					k = j;
					break;
				}
		}
		return s[0].substring(0, k);
	}

}
