package org.jboss.historia.core;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.jboss.logging.Logger;

public class UntestedCommitDetectionStrategy {
	
	private static final Logger LOGGER = Logger.getLogger(UntestedCommitDetectionStrategy.class);

	public static class FileUpdates {
		private final String path;
		private long updates = 0;
		private long untestedUpdates = 0;
		
		public FileUpdates(String filePath) {
			this.path = filePath;
		}
		
		public String getPath() {
			return path;
		}

		public long getUpdates() {
			return updates;
		}

		public void incrementUpdates() {
			this.updates = this.updates +1;
		}

		public long getUntestedUpdates() {
			return untestedUpdates;
		}

		public void incrementUntestedUpdates() {
			this.untestedUpdates = this.untestedUpdates + 1;
		}
		
		public String toString() {
			return path + "(Untested: " + untestedUpdates + "/" + updates + ")";
		}
		
		public static void printHeader(Writer w) throws IOException {
			w.append("File path").append(",").append("# updates").append(",").append("# untested updates").append("\n");
		}
		
		public void print(Writer w) throws IOException {
			w.append(path).append(",").append(String.valueOf(updates)).append(",").append(String.valueOf(untestedUpdates)).append("\n");
		}
	}
	
	private final String pathFilter;
	private final boolean isPrefix;
	
	public UntestedCommitDetectionStrategy(String pathFilter, boolean isPrefix) {
		this.pathFilter = pathFilter;
		this.isPrefix = isPrefix;
	}
	
	public List<FileUpdates> process(String repositoryUri, String localRepoCloneURI) throws Exception {
		JGitUtils jgit = new JGitUtils(repositoryUri, localRepoCloneURI);
		Set<String> files = jgit.getFilesOnHEAD();
		List<FileUpdates> list = new ArrayList<>();
		for (String f : files) {
			if (pathFilter == null || (isPrefix ? f.startsWith(pathFilter) : f.contains(pathFilter))) {
				FileUpdates fu = new FileUpdates(f);
				processFile(fu, jgit, f, LOGGER.isDebugEnabled());
				list.add(fu);
			}
		}
		jgit.close();
		return list;
	}
	
	public void process(String repositoryUri, String localRepoCloneURI, Writer w) throws Exception {
		JGitUtils jgit = new JGitUtils(repositoryUri, localRepoCloneURI);
		Set<String> files = jgit.getFilesOnHEAD();
		FileUpdates.printHeader(w);
		for (String f : files) {
			if (pathFilter == null || (isPrefix ? f.startsWith(pathFilter) : f.contains(pathFilter))) {
				FileUpdates fu = new FileUpdates(f);
				processFile(fu, jgit, f, LOGGER.isDebugEnabled());
				fu.print(w);
			}
		}
		jgit.close();
	}
	
	private void processFile(FileUpdates fu, JGitUtils jgit, String f, boolean debug) throws Exception {
		if (debug)
			LOGGER.debug("F: " + f);
		List<RevCommit> commitHistory = jgit.getFileHistory(f);
		int s = commitHistory.size();
		for (int i = 0; i < s - 1; i++) {
			RevCommit commit = commitHistory.get(i);
			if (debug)
				LOGGER.debug(" Commit #" + (i + 1) + "- " + commit.getName() + ": " + commit.getShortMessage());
			fu.incrementUpdates();
			if (!affectsTests(jgit, fu, jgit.getParentCommitTree(commit), commit.getTree())) {
				fu.incrementUntestedUpdates();
			}
		}
		fu.incrementUpdates();
		if (debug)
			LOGGER.debug(" Commit #" + s + ": " + commitHistory.get(s - 1).getShortMessage());
		if (!affectsTests(jgit, fu, null, commitHistory.get(s - 1).getTree())) {
			fu.incrementUntestedUpdates();
		}
	}
	
	private boolean affectsTests(JGitUtils jgit, FileUpdates fu, RevTree parentCommitTree, RevTree commitTree) throws Exception {
		Set<String> cfs;
		if (parentCommitTree == null) {
			cfs = jgit.getChangedFiles(commitTree);
		} else {
			cfs = jgit.getChangedFiles(parentCommitTree, commitTree);
		}
		boolean debug = LOGGER.isDebugEnabled();
		for (String cf : cfs) {
			if (debug)
				LOGGER.debug("    " + cf);
			if (cf.contains("src/test")) {
				return true;
			}
		}
		return false;
	}
}
