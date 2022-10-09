package org.jboss.historia.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RenameCallback;
import org.eclipse.jgit.revwalk.RevCommit;
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
		TreeWalk walk = new TreeWalk(git.getRepository());
	    walk.setRecursive(true);
	    Set<String> files = new TreeSet<>(); // Uses natural ordering
		RevCommit commit = git.log().call().iterator().next();
		walk.reset(commit.getTree());
		while (walk.next()) {
			files.add(walk.getPathString());
		}
		walk.close();
		return files;
	}

	public Set<String> getChangedFiles(RevCommit commit) throws Exception
	{
	    TreeWalk walk = new TreeWalk(git.getRepository());
	    walk.setRecursive(true);
	    Set<String> files = new TreeSet<>(); // Uses natural ordering
		walk.reset(commit.getTree());
		while (walk.next()) {
			files.add(walk.getPathString());
		}
		walk.close();
		return files;
	}
	
	public Set<String> getChangedFiles(RevCommit parentCommit, RevCommit commit) throws Exception
	{
	    TreeWalk walk = new TreeWalk(git.getRepository());
	    walk.setRecursive(true);

	    walk.setFilter(TreeFilter.ANY_DIFF);

        walk.reset(parentCommit.getTree().getId(), commit.getTree().getId());
        List<DiffEntry>changes = DiffEntry.scan(walk);
	    Set<String> files = new TreeSet<>(); // Uses natural ordering
        changes.forEach(de->
        {
            files.add(de.getNewPath());
        });
	    return files;
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

		RevWalk rw = new RevWalk(repo);
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
		rw.close();
		return list;
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
}
