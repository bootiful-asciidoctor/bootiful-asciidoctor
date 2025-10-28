package bootiful.asciidoctor.publishers;

import bootiful.asciidoctor.DocumentPublisher;
import bootiful.asciidoctor.files.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Archives the published artifacts and writes them AWS S3
 */
class AwsS3DocumentPublisher implements DocumentPublisher {

	private static final Logger log = LoggerFactory.getLogger(AwsS3DocumentPublisher.class);

	private final S3Client s3;

	private final String bucketName;

	private final String contentType = "binary/octet-stream";

	AwsS3DocumentPublisher(S3Client s3, String bucketName) {
		this.s3 = s3;
		this.bucketName = bucketName;
	}

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

	private URI upload(String bucketName, String contentType, String nestedBucketFolder, File file) {
		if (file.length() > 0) {
			var key = (nestedBucketFolder == null ? "" : nestedBucketFolder + "/") + file.getName();
			var putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(key).contentType(contentType)
					.build();
			var putObjectResponse = this.s3.putObject(putObjectRequest, file.toPath());
			Assert.notNull(putObjectResponse, "the S3 file hasn't been uploaded");
			var uri = this.createS3Uri(bucketName, nestedBucketFolder, file.getName());
			log.info(
					"uploaded the file {} to bucket {} with content type {} and nested folder {}. The resulting URI is {}",
					file.getAbsolutePath(), bucketName, contentType, nestedBucketFolder, uri);
			return uri;
		}
		return null;
	}

	private URI createS3Uri(String bucketName, String nestedBucketFolder, String fileName) {
		var uri = this.s3FqnFor(bucketName, nestedBucketFolder, fileName);
		log.debug("the S3 FQN URI is {}", uri);
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
			var object = this.s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
			Assert.notNull(object, "the fetch of the object should not be null");
		}
		catch (Exception e) {
			log.warn("No object of this key name {} exists in this bucket, {}", key, bucket);
			return null;
		}
		return String.format("s3://%s/%s", bucket, key);
	}

}
