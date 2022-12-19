package bootiful.asciidoctor.publishers;

import bootiful.asciidoctor.DocumentPublisher;
import bootiful.asciidoctor.autoconfigure.FileCopyUtils;
import bootiful.asciidoctor.files.FileUtils;
import bootiful.asciidoctor.git.GitCloneCallback;
import bootiful.asciidoctor.git.GitPushCallback;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.springframework.util.Assert;

import java.io.File;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;

/**
 * this {@link DocumentPublisher} clones a git repository, checks out a configured branch,
 * and then checks in the archive of the produced files.
 *
 * @author Josh Long
 * @author Trisha Gee
 *
 */
@Slf4j
class GitBranchDocumentPublisher implements DocumentPublisher {

	private final URI repository;

	private final String branch;

	private final GitPushCallback gitPushCallback;

	private final GitCloneCallback gitCloneCallback;

	GitBranchDocumentPublisher(URI repository, String branch, GitPushCallback creator,
			GitCloneCallback gitCloneCallback) {
		this.repository = repository;
		this.branch = branch;
		this.gitPushCallback = creator;
		this.gitCloneCallback = gitCloneCallback;
	}

	private void delete(File file) {
		if (file.exists()) {
			file.delete();
			FileUtils.resetOrRecreateDirectory(file);
			log.debug("deleting " + file.getAbsolutePath());
		}
	}

	@Override
	public void publish(Map<String, Collection<File>> files) throws Exception {
		var file = File.createTempFile("clone-directory", "");
		delete(file);
		file.mkdirs();
		log.debug("cloning remote repository {} to local directory {}  ", this.repository.toString(),
				file.getAbsolutePath());
		Assert.state(file.exists(), () -> "the directory " + file.getAbsolutePath() + " does not exist.");

		var git = gitCloneCallback.clone(this.repository, file);
		git.checkout().setCreateBranch(true).setName(this.branch)
				.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).setStartPoint("origin/" + this.branch)
				.call();

		Assert.state(file.exists(), () -> "there should exist a cloned directory");
		Assert.state(file.length() > 0, () -> "there should be more than one file (a .git directory if nothing else!)");
		for (var entry : files.entrySet()) {
			var key = entry.getKey();
			var filesForKey = entry.getValue();
			for (var f : filesForKey) {
				var newFile = new File(new File(file, key), f.getName());
				if (!newFile.getParentFile().exists())
					newFile.getParentFile().mkdirs();
				FileCopyUtils.copy(f, newFile);
				Assert.state(newFile.exists() && newFile.length() == f.length(),
						() -> "the copy of the new file to the git clone repository should have succeeded.");
				git.add().addFilepattern(key + "/" + newFile.getName()).call();
			}
		}
		git.commit().setMessage("adding new files on " + Instant.now()).call();
		gitPushCallback.push(git);
		delete(file);
	}

}
