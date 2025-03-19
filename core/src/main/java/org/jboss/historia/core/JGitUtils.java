package org.jboss.historia.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
	
	// Cache fields
	private List<RevCommit> allCommitsCache;
	private List<RevCommit> mergeCommitsCache;
	private Map<String, List<RevCommit>> pullRequestsCache;
	private Map<String, List<RevCommit>> fileHistoryCache = new HashMap<>();
	
	public JGitUtils(String repositoryUri, String localRepoCloneURI) {
		git = cloneRepo(repositoryUri, localRepoCloneURI);
	}
	
	/**
	 * Get the underlying Git instance.
	 * 
	 * @return The Git instance
	 */
	public Git getGit() {
		return git;
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
	
	/**
	 * Clear all caches.
	 * This should be called if the repository state changes.
	 */
	public void clearCaches() {
		allCommitsCache = null;
		mergeCommitsCache = null;
		pullRequestsCache = null;
		fileHistoryCache.clear();
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

	/**
	 * Get the commit history for a specific file.
	 * Uses caching to improve performance for repeated calls.
	 * 
	 * @param filepath Path to the file
	 * @return List of commits that modified the file
	 */
	public List<RevCommit> getFileHistory(String filepath) throws Exception {
		// Check cache first
		if (fileHistoryCache.containsKey(filepath)) {
			// Return a copy to prevent modification of the cache
			return new ArrayList<>(fileHistoryCache.get(filepath));
		}
		
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
			
			// Cache the result
			fileHistoryCache.put(filepath, list);
			
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
	 * Check if a commit is a merge commit (has more than one parent).
	 * 
	 * @param commit The commit to check
	 * @return true if the commit is a merge commit
	 */
	public boolean isMergeCommit(RevCommit commit) {
		return commit.getParentCount() > 1;
	}
	
	/**
	 * Extract pull request ID from a merge commit message.
	 * Common formats include:
	 * - "Merge pull request #123 from..."
	 * 
	 * @param commit The merge commit to analyze
	 * @return The pull request ID or null if not found
	 */
	public String extractPullRequestId(RevCommit commit) {
		if (!isMergeCommit(commit)) {
			return null;
		}
		
		String message = commit.getFullMessage();
		
		// Pattern for GitHub PR merge commits
		Pattern prPattern = Pattern.compile("Merge pull request #(\\d+)");
		Matcher matcher = prPattern.matcher(message);
		
		if (matcher.find()) {
			return matcher.group(1);
		}
		
		// Fallback patterns for other PR references
		Pattern[] fallbackPatterns = {
			Pattern.compile("\\[PR[\\s-]?(\\d+)\\]"),
			Pattern.compile("\\(#(\\d+)\\)"),
			Pattern.compile("Merge PR #(\\d+)"),
		};
		
		for (Pattern pattern : fallbackPatterns) {
			matcher = pattern.matcher(message);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}
		
		return null;
	}
	
	/**
	 * Get all commits in the repository.
	 * Uses caching to improve performance for repeated calls.
	 * 
	 * @return List of all commits in the repository
	 */
	public List<RevCommit> getAllCommits() throws Exception {
		if (allCommitsCache == null) {
			allCommitsCache = new ArrayList<>();
			
			try {
				Iterable<RevCommit> commits = git.log().all().call();
				for (RevCommit commit : commits) {
					allCommitsCache.add(commit);
				}
			} catch (Exception e) {
				LOGGER.error("Error getting all commits", e);
				throw e;
			}
		}
		
		// Return a copy to prevent modification of the cache
		return new ArrayList<>(allCommitsCache);
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
	 * Get all merge commits in the repository.
	 * Uses caching to improve performance for repeated calls.
	 * 
	 * @return List of merge commits
	 */
	public List<RevCommit> getMergeCommits() throws Exception {
		if (mergeCommitsCache == null) {
			List<RevCommit> allCommits = getAllCommits();
			mergeCommitsCache = new ArrayList<>();
			
			for (RevCommit commit : allCommits) {
				if (isMergeCommit(commit)) {
					mergeCommitsCache.add(commit);
				}
			}
		}
		
		// Return a copy to prevent modification of the cache
		return new ArrayList<>(mergeCommitsCache);
	}
	
	/**
	 * Get all commits that were merged in through a merge commit.
	 * This traces all commits that are reachable from the merge commit's second parent
	 * but not from the first parent.
	 * 
	 * @param mergeCommit The merge commit
	 * @return List of commits that were merged in
	 */
	public List<RevCommit> getMergedCommits(RevCommit mergeCommit) throws Exception {
		if (!isMergeCommit(mergeCommit)) {
			return new ArrayList<>();
		}
		
		Repository repo = git.getRepository();
		List<RevCommit> mergedCommits = new ArrayList<>();
		
		try (RevWalk revWalk = new RevWalk(repo)) {
			// Get the merge commit's parents
			RevCommit firstParent = revWalk.parseCommit(mergeCommit.getParent(0).getId());
			RevCommit secondParent = revWalk.parseCommit(mergeCommit.getParent(1).getId());
			
			// Get all commits reachable from the second parent
			revWalk.reset();
			revWalk.markStart(secondParent);
			
			// Exclude commits reachable from the first parent
			revWalk.markUninteresting(firstParent);
			
			// Collect all commits that are in the second parent but not in the first
			for (RevCommit commit : revWalk) {
				mergedCommits.add(commit);
			}
		}
		
		return mergedCommits;
	}
	
	/**
	 * Build a map of all pull requests in the repository, including all commits
	 * that were part of each PR.
	 * Uses caching to improve performance for repeated calls.
	 * 
	 * @return Map of PR ID to list of commits in that PR
	 */
	public Map<String, List<RevCommit>> getAllPullRequests() throws Exception {
		if (pullRequestsCache == null) {
			List<RevCommit> mergeCommits = getMergeCommits();
			pullRequestsCache = new HashMap<>();
			
			for (RevCommit mergeCommit : mergeCommits) {
				String prId = extractPullRequestId(mergeCommit);
				
				if (prId != null) {
					// Get all commits that were merged in this PR
					List<RevCommit> prCommits = getMergedCommits(mergeCommit);
					
					// Add the merge commit itself
					prCommits.add(mergeCommit);
					
					pullRequestsCache.put(prId, prCommits);
				}
			}
		}
		
		// Return a deep copy to prevent modification of the cache
		Map<String, List<RevCommit>> result = new HashMap<>();
		for (Map.Entry<String, List<RevCommit>> entry : pullRequestsCache.entrySet()) {
			result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		
		return result;
	}
	
	/**
	 * Find all pull requests that include changes to a specific file.
	 * 
	 * @param filepath Path to the file
	 * @return Map of PR ID to list of commits in that PR that modify the file
	 */
	public Map<String, List<RevCommit>> getPullRequestsForFile(String filepath) throws Exception {
		// Get all PRs in the repository
		Map<String, List<RevCommit>> allPRs = getAllPullRequests();
		
		// Get the file's commit history
		List<RevCommit> fileHistory = getFileHistory(filepath);
		Set<String> fileCommitHashes = new HashSet<>();
		for (RevCommit commit : fileHistory) {
			fileCommitHashes.add(commit.getName());
		}
		
		// Find PRs that include commits modifying this file
		Map<String, List<RevCommit>> fileCommitsByPR = new HashMap<>();
		
		// First, check PRs identified through merge commits
		for (Map.Entry<String, List<RevCommit>> entry : allPRs.entrySet()) {
			String prId = entry.getKey();
			List<RevCommit> prCommits = entry.getValue();
			
			// Find commits in this PR that modified the file
			List<RevCommit> fileCommitsInPR = new ArrayList<>();
			for (RevCommit commit : prCommits) {
				if (fileCommitHashes.contains(commit.getName())) {
					fileCommitsInPR.add(commit);
				}
			}
			
			// If this PR includes commits that modified the file, add it to the result
			if (!fileCommitsInPR.isEmpty()) {
				fileCommitsByPR.put(prId, fileCommitsInPR);
			}
		}
		
		// Then, handle individual commits that aren't part of an identified PR
		for (RevCommit commit : fileHistory) {
			boolean isInPR = false;
			
			// Check if this commit is already included in a PR
			for (List<RevCommit> prCommits : fileCommitsByPR.values()) {
				for (RevCommit prCommit : prCommits) {
					if (prCommit.getName().equals(commit.getName())) {
						isInPR = true;
						break;
					}
				}
				if (isInPR) {
					break;
				}
			}
			
			// If not in a PR, add it as an individual commit
			if (!isInPR) {
				List<RevCommit> individualCommit = new ArrayList<>();
				individualCommit.add(commit);
				fileCommitsByPR.put(commit.getName(), individualCommit);
			}
		}
		
		return fileCommitsByPR;
	}
	
	/**
	 * Get all commits in a specific pull request.
	 * 
	 * @param prId Pull request ID
	 * @return List of all commits in the PR
	 */
	public List<RevCommit> getCommitsInPullRequest(String prId) throws Exception {
		Map<String, List<RevCommit>> allPRs = getAllPullRequests();
		
		// If we have this PR in our map, return its commits
		if (allPRs.containsKey(prId)) {
			return allPRs.get(prId);
		}
		
		// If not found in PRs identified through merge commits, try the old approach
		List<RevCommit> allCommits = getAllCommits();
		List<RevCommit> prCommits = new ArrayList<>();
		
		for (RevCommit commit : allCommits) {
			String commitPrId = extractPullRequestId(commit);
			
			if (prId.equals(commitPrId)) {
				prCommits.add(commit);
			}
		}
		
		return prCommits;
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
