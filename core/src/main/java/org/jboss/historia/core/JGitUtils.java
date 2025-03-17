package org.jboss.historia.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RenameCallback;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.jboss.logging.Logger;

/**
 * https://www.codeaffine.com/2015/12/15/getting-started-with-jgit/ 
 * 
 * @author alessio
 *
 */
public class JGitUtils implements AutoCloseable {

	private static Logger LOGGER = Logger.getLogger(JGitUtils.class);
	private final Git git;
	
	public JGitUtils(String repositoryUri, String localRepoCloneURI) {
		git = cloneRepo(repositoryUri, localRepoCloneURI);
	}
	
	private static Git cloneRepo(String repositoryUri, String localRepoCloneURI) {
		File file = new File(localRepoCloneURI);
		try {
			if (file.exists()) {
				return Git.open(file);
			} else {
				return Git.cloneRepository().setURI(repositoryUri).setDirectory(file).call();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void close() throws Exception {
		if (git != null) {
			git.close();
		}
	}
	
	public Set<String> getFilesOnHEAD() throws Exception
	{
		try (TreeWalk walk = new TreeWalk(git.getRepository())) {
		    walk.setRecursive(true);
		    Set<String> files = new TreeSet<>(); // Uses natural ordering
			RevCommit commit = git.log().call().iterator().next();
			walk.reset(commit.getTree());
			while (walk.next()) {
				files.add(walk.getPathString());
			}
			return files;
		}
	}

	public Set<String> getChangedFiles(RevTree commitTree) throws Exception
	{
	    try (TreeWalk walk = new TreeWalk(git.getRepository())) {
		    walk.setRecursive(true);
		    Set<String> files = new TreeSet<>(); // Uses natural ordering
			walk.reset(commitTree);
			while (walk.next()) {
				files.add(walk.getPathString());
			}
			return files;
	    }
	}
	
	public Set<String> getChangedFiles(RevTree parentCommitTree, RevTree commitTree) throws Exception
	{
	    try (TreeWalk walk = new TreeWalk(git.getRepository())) {
		    walk.setRecursive(true);
		    walk.setFilter(TreeFilter.ANY_DIFF);
	        walk.reset(parentCommitTree.getId(), commitTree.getId());
	        List<DiffEntry>changes = DiffEntry.scan(walk);
		    Set<String> files = new TreeSet<>(); // Uses natural ordering
	        changes.forEach(de->
	        {
	            files.add(de.getNewPath());
	        });
		    return files;
	    }
	}
	
	
	public static class DiffCollector extends RenameCallback {
		List<DiffEntry> diffs = new ArrayList<DiffEntry>();

		@Override
		public void renamed(DiffEntry diff) {
			diffs.add(diff);
		}
	}

	public List<RevCommit> getFileHistory(String filepath) throws Exception {
		Repository repo = git.getRepository();
		Config config = repo.getConfig();
		config.setBoolean("diff", null, "renames", true);

		try (RevWalk rw = new RevWalk(repo)) {
			DiffCollector diffCollector = new DiffCollector();
	
			org.eclipse.jgit.diff.DiffConfig dc = config.get(org.eclipse.jgit.diff.DiffConfig.KEY);
			FollowFilter followFilter = FollowFilter.create(filepath, dc);
			followFilter.setRenameCallback(diffCollector);
			rw.setTreeFilter(followFilter);
			rw.markStart(rw.parseCommit(repo.resolve(Constants.HEAD)));
	
			List<RevCommit> list = new ArrayList<>();
			for (RevCommit rc : rw) {
				list.add(rc);
			}
			return list;
		}
	}
	
	public void blameOnFile(String filepath) throws Exception {
        final BlameResult result = git.blame().setFilePath(filepath)
          .setTextComparator(RawTextComparator.WS_IGNORE_ALL).call();
        final RawText rawText = result.getResultContents();
        for (int i = 0; i < rawText.size(); i++) {
          final RevCommit sc = result.getSourceCommit(i);
          LOGGER.debug(result.getSourceAuthor(i).getName() +
              (sc != null ? " " + sc.getCommitTime() + " " + sc.getName() : "") + ": " + rawText.getString(i));
        }
		
	}
	
	public RevCommit getCommit(String hash) throws Exception {
		ObjectId oid = git.getRepository().resolve(hash);
		return git.getRepository().parseCommit(oid);
	}
	
	public RevCommit getParentCommit(RevCommit commit) throws Exception {
		try (RevWalk revWalk = new RevWalk(git.getRepository())) {
		    RevCommit c = revWalk.parseCommit(commit);
		    return c.getParent(0);
		}
	}
	
	public RevTree getParentCommitTree(RevCommit commit) throws Exception {
		try (RevWalk revWalk = new RevWalk(git.getRepository())) {
		    RevCommit c = revWalk.parseCommit(commit);
		    RevCommit p = c.getParent(0);
		    return revWalk.parseTree(p);
		}
	}
	
	/**
	 * Extract pull request ID from commit message.
	 * Common formats include:
	 * - "Merge pull request #123 from..."
	 * - "[PR-123] Fix bug..."
	 * - "Fix bug (#123)"
	 * 
	 * @param commit The commit to analyze
	 * @return The pull request ID or null if not found
	 */
	public String extractPullRequestId(RevCommit commit) {
		String message = commit.getFullMessage();
		
		// Common patterns for PR references in commit messages
		Pattern[] patterns = {
			Pattern.compile("Merge pull request #(\\d+)"),
			Pattern.compile("\\[PR[\\s-]?(\\d+)\\]"),
			Pattern.compile("\\(#(\\d+)\\)"),
			Pattern.compile("#(\\d+)"),
		};
		
		for (Pattern pattern : patterns) {
			Matcher matcher = pattern.matcher(message);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}
		
		return null;
	}
	
	/**
	 * Group commits by pull request ID.
	 * Commits that don't have a PR ID will be mapped to their own commit hash.
	 * 
	 * @param commits List of commits to group
	 * @return Map of PR ID (or commit hash) to list of commits
	 */
	public Map<String, List<RevCommit>> groupCommitsByPullRequest(List<RevCommit> commits) {
		Map<String, List<RevCommit>> prGroups = new HashMap<>();
		
		for (RevCommit commit : commits) {
			String prId = extractPullRequestId(commit);
			
			// If no PR ID found, use commit hash as the key
			if (prId == null) {
				prId = commit.getName();
			}
			
			prGroups.computeIfAbsent(prId, k -> new ArrayList<>()).add(commit);
		}
		
		return prGroups;
	}
	
	/**
	 * Check if any commit in a pull request affects test files.
	 * 
	 * @param prCommits List of commits in a pull request
	 * @return true if any commit in the PR affects test files
	 */
	public boolean pullRequestAffectsTests(List<RevCommit> prCommits) throws Exception {
		for (RevCommit commit : prCommits) {
			Set<String> changedFiles;
			if (commit.getParentCount() > 0) {
				RevTree parentTree = getParentCommitTree(commit);
				changedFiles = getChangedFiles(parentTree, commit.getTree());
			} else {
				changedFiles = getChangedFiles(commit.getTree());
			}
			
			for (String file : changedFiles) {
				if (file.contains("src/test")) {
					return true;
				}
			}
		}
		
		return false;
	}
}
