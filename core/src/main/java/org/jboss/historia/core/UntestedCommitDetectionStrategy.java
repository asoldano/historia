package org.jboss.historia.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.jboss.logging.Logger;

public class UntestedCommitDetectionStrategy {
	
	private static final Logger LOGGER = Logger.getLogger(UntestedCommitDetectionStrategy.class);

	public class FileUpdates {
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
	}
	
	private final String pathPrefixFilter;
	
	public UntestedCommitDetectionStrategy() {
		this.pathPrefixFilter = null;
	}
	
	public UntestedCommitDetectionStrategy(String pathPrefixFilter) {
		this.pathPrefixFilter = pathPrefixFilter;
	}
	
	public List<FileUpdates> process(String repositoryUri, String localRepoCloneURI) throws Exception {
		JGitUtils jgit = new JGitUtils(repositoryUri, localRepoCloneURI);
		Set<String> files = jgit.getFilesOnHEAD();
		List<FileUpdates> list = new ArrayList<>();
		boolean debug = LOGGER.isDebugEnabled();
		for (String f : files) {
			if (pathPrefixFilter == null || f.startsWith(pathPrefixFilter)) {
				if (debug)
					LOGGER.debug("F: " + f);
				FileUpdates fu = new FileUpdates(f);
				list.add(fu);
				List<RevCommit> commitHistory = jgit.getFileHistory(f);
				int s = commitHistory.size();
				for (int i = 0; i < s - 1; i++) {
					RevCommit commit = commitHistory.get(i);
					if (debug)
						LOGGER.debug(" Commit #" + (i+1) + "- " + commit.getName() +": "+ commit.getShortMessage());
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
		}
		jgit.close();
		return list;
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
			if (cf.startsWith("src/test")) {
				return true;
			}
		}
		return false;
	}
}
