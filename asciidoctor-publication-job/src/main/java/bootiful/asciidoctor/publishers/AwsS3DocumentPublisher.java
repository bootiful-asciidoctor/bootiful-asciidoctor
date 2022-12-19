package bootiful.asciidoctor.publishers;

import bootiful.asciidoctor.DocumentPublisher;
import bootiful.asciidoctor.files.ZipUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Archives the published artifacts and writes them AWS S3
 */
@Slf4j
@RequiredArgsConstructor
class AwsS3DocumentPublisher implements DocumentPublisher {

	private final AmazonS3 s3;

	private final String bucketName;

	private final String contentType = "binary/octet-stream";

	@Override
	public void publish(Map<String, Collection<File>> files) throws Exception {
		var nestedFolder = timestring(new Date());
		log.debug("the time string will be {}", nestedFolder);
		var nl = new ArrayList<File>();
		for (var collectionOfFiles : files.values()) {
			for (var file : collectionOfFiles) {
				if (file.isFile()) {
					nl.add(file);
				}
				else if (file.isDirectory()) {
					recurse(file, nl);
				}
			}
		}
		var zipFileDir = File.createTempFile(nestedFolder, "");
		if (zipFileDir.exists())
			zipFileDir.delete();
		zipFileDir.mkdirs();
		var zipFile = new File(zipFileDir, "documents.zip");
		ZipUtils.buildZipFileFromFiles(zipFile, nl.toArray(File[]::new));
		upload(this.bucketName, this.contentType, nestedFolder, zipFile);
	}

	private void recurse(File root, List<File> output) {
		for (var f : Objects.requireNonNull(root.listFiles())) {
			if (f.isFile())
				output.add(f);
			else
				recurse(f, output);
		}
	}

	private String timestring(Date runtime) {
		var format = "yyyy_MM_dd_HH_mm_ss";
		var cal = Calendar.getInstance();
		cal.setTime(runtime);
		var sdf = new SimpleDateFormat(format);
		return sdf.format(cal.getTime());
	}

	@SneakyThrows
	private URI upload(String bucketName, String contentType, String nestedBucketFolder, File file) {
		if (file.length() > 0) {
			var objectMetadata = new ObjectMetadata();
			objectMetadata.setContentType(contentType);
			objectMetadata.setContentLength(file.length());
			var request = new PutObjectRequest(
					bucketName + (nestedBucketFolder == null ? "" : "/" + nestedBucketFolder), file.getName(), file);
			var putObjectResult = this.s3.putObject(request);
			Assert.notNull(putObjectResult, "the S3 file hasn't been uploaded");
			var uri = this.createS3Uri(bucketName, nestedBucketFolder, file.getName());
			log.info("uploaded the file " + file.getAbsolutePath() + " to bucket " + bucketName + " with content type "
					+ contentType + " and nested folder " + nestedBucketFolder + ". The resulting URI is " + uri);
			return uri;
		}
		return null;
	}

	private URI createS3Uri(String bucketName, String nestedBucketFolder, String fileName) {
		var uri = this.s3FqnFor(bucketName, nestedBucketFolder, fileName);
		log.debug("the S3 FQN URI is " + uri);
		if (null == uri) {
			log.debug("the URI is null; returning null");
			return null;
		}
		return URI.create(uri);
	}

	private String s3FqnFor(String bucket, String folder, String fn) {
		Assert.notNull(bucket, "the bucket name can't be null");
		Assert.notNull(fn, "the file name can't be null");
		if (StringUtils.hasText(folder)) {
			if (!folder.endsWith("/")) {
				folder = folder + "/";
			}
		}
		String key = folder + fn;
		try {
			var object = this.s3.getObject(new GetObjectRequest(bucket, key));
			Assert.notNull(object, "the fetch of the object should not be null");
		}
		catch (Exception e) {
			log.warn("No object of this key name " + key + " exists in this bucket, " + bucket);
			return null;
		}
		return String.format("s3://%s/%s", bucket, key);
	}

}
