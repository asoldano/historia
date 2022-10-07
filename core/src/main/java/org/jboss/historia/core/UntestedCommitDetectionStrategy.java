package org.jboss.historia.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.revwalk.RevCommit;

public class UntestedCommitDetectionStrategy {

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
			return path + "(U:" + untestedUpdates + ",T:" + updates + ")";
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
		for (String f : files) {
			if (pathPrefixFilter == null | f.startsWith(pathPrefixFilter)) {
				FileUpdates fu = new FileUpdates(f);
				list.add(fu);
				fu.incrementUpdates();
				List<RevCommit> commitHistory = jgit.getFileHistory(f);
				int s = commitHistory.size();
				for (int i = 0; i < s - 1; i++) {
					if (!affectsTests(jgit, fu, commitHistory.get(i + 1), commitHistory.get(i))) {
						fu.incrementUntestedUpdates();
					}
				}
				if (!affectsTests(jgit, fu, null, commitHistory.get(s - 1))) {
					fu.incrementUntestedUpdates();
				}
			}
		}
		jgit.close();
		return list;
	}
	
	private boolean affectsTests(JGitUtils jgit, FileUpdates fu, RevCommit parentCommit, RevCommit commit) throws Exception {
		Set<String> cfs;
		if (parentCommit == null) {
			cfs = jgit.getChangedFiles(commit);
		} else {
			cfs = jgit.getChangedFiles(parentCommit, commit);
		}
		for (String cf : cfs) {
			if (cf.startsWith("src/main/test")) {
				return true;
			}
		}
		return false;
	}
}
