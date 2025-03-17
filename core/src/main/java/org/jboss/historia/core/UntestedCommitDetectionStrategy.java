package org.jboss.historia.core;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.jboss.logging.Logger;

public class UntestedCommitDetectionStrategy {
	
	private static final Logger LOGGER = Logger.getLogger(UntestedCommitDetectionStrategy.class);

	public static class FileUpdates {
		private final String prefix;
		private final String path;
		private long updates = 0;
		private long untestedUpdates = 0;
		private long updatesSinceLastTested = 0;
		
		public FileUpdates(String prefix, String filePath) {
			this.prefix = prefix;
			this.path = filePath;
		}
		
		public String getPath() {
			return path;
		}

		public String getPrefix() {
			return prefix;
		}

		public long getUpdates() {
			return updates;
		}

		public void incrementUpdates(boolean tested) {
			this.updates = this.updates +1;
			if (!tested) {
				incrementUntestedUpdates();
				if (this.updates == this.untestedUpdates)
					incrementUpdatesSinceLastTested();
			}
		}

		public long getUntestedUpdates() {
			return untestedUpdates;
		}

		public long getUpdatesSinceLastTested() {
			return updatesSinceLastTested;
		}

		public void incrementUntestedUpdates() {
			this.untestedUpdates = this.untestedUpdates + 1;
		}
		
		public void incrementUpdatesSinceLastTested() {
			this.updatesSinceLastTested = this.updatesSinceLastTested + 1;
		}
		
		public String toString() {
			return path + "(Untested: " + untestedUpdates + "/" + updates + ")";
		}
		
		public static void printHeader(Writer w) throws IOException {
			w.append("Module").append(",").append("File").append(",").append("# updates").append(",").append("# untested updates").append(",")
					.append("# untested updates %").append(",").append("# updates since last tested").append("\n");
		}
		
		public void print(Writer w) throws IOException {
			w.append(prefix).append(",").append(path).append(",").append(String.valueOf(updates)).append(",")
					.append(String.valueOf(untestedUpdates)).append(",")
					.append(String.valueOf(Math.round(100 * (double) untestedUpdates / (double) updates))).append(",")
					.append(String.valueOf(updatesSinceLastTested)).append("\n");
		}
	}
	
	private final String pathFilter;
	
	public UntestedCommitDetectionStrategy(String pathFilter) {
		this.pathFilter = pathFilter;
	}
	
	public List<FileUpdates> process(String repositoryUri, String localRepoCloneURI) throws Exception {
		JGitUtils jgit = new JGitUtils(repositoryUri, localRepoCloneURI);
		Set<String> files = jgit.getFilesOnHEAD();
		List<FileUpdates> list = new ArrayList<>();
		for (String f : files) {
			int pathFilterIndex = pathFilter == null ? 0 : f.indexOf(pathFilter);
			if (pathFilterIndex >= 0) {
				FileUpdates fu = new FileUpdates(f.substring(0, pathFilterIndex), f.substring(pathFilterIndex));
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
			int pathFilterIndex = pathFilter == null ? 0 : f.indexOf(pathFilter);
			if (pathFilterIndex >= 0) {
				FileUpdates fu = new FileUpdates(f.substring(0, pathFilterIndex), f.substring(pathFilterIndex));
				processFile(fu, jgit, f, LOGGER.isDebugEnabled());
				fu.print(w);
			}
		}
		jgit.close();
	}
	
	private void processFile(FileUpdates fu, JGitUtils jgit, String f, boolean debug) throws Exception {
		if (debug)
			LOGGER.debug("F: " + f);
		
		// Get file history
		List<RevCommit> commitHistory = jgit.getFileHistory(f);
		
		// Group commits by pull request
		Map<String, List<RevCommit>> prGroups = jgit.groupCommitsByPullRequest(commitHistory);
		
		if (debug)
			LOGGER.debug("Found " + prGroups.size() + " pull requests/individual commits for file " + f);
		
		// Process each pull request group
		for (Map.Entry<String, List<RevCommit>> entry : prGroups.entrySet()) {
			String prId = entry.getKey();
			List<RevCommit> prCommits = entry.getValue();
			
			if (debug)
				LOGGER.debug(" Processing PR/Commit group: " + prId + " with " + prCommits.size() + " commits");
			
			// Check if any commit in this PR affects tests
			boolean testedInPR = jgit.pullRequestAffectsTests(prCommits);
			
			// For each commit in the PR, increment updates with the same tested status
			for (RevCommit commit : prCommits) {
				if (debug)
					LOGGER.debug("  Commit: " + commit.getName() + ": " + commit.getShortMessage() + 
							" (Tested in PR: " + testedInPR + ")");
				
				fu.incrementUpdates(testedInPR);
			}
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
