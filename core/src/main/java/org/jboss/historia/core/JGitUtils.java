package org.jboss.historia.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RenameCallback;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * https://www.codeaffine.com/2015/12/15/getting-started-with-jgit/ 
 * 
 * @author alessio
 *
 */
public class JGitUtils {

	//https://github.com/jbossws/jbossws-api.git
	
	public static Git cloneRepo(String repositoryUri, String localRepoCloneURI) throws Exception {
		File file = new File(localRepoCloneURI);
		if (file.exists()) {
			return Git.open(file);
		} else {
			return Git.cloneRepository().setURI(repositoryUri).setDirectory(file).call();
		}
	}

	/* Better implementation following renamed files in showFileHistory
	public static void printFileHistory(String repositoryUri, String localRepoCloneURI, String filePath) throws Exception {
		Git git = cloneRepo(repositoryUri, localRepoCloneURI);
		for (Iterator<RevCommit> iterator = git.log().addPath(filePath).call().iterator(); iterator.hasNext(); ) {
			RevCommit rc = iterator.next();
			System.out.println(rc.getName() + " " + rc.getShortMessage());
		}
		git.close();
	} */
	
	
	private static class DiffCollector extends RenameCallback {
		List<DiffEntry> diffs = new ArrayList<DiffEntry>();

		@Override
		public void renamed(DiffEntry diff) {
			diffs.add(diff);
		}
	}

	public static void showFileHistory(String repositoryUri, String localRepoCloneURI, String filepath) throws Exception {
		Git git = cloneRepo(repositoryUri, localRepoCloneURI);
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

		for (RevCommit rc : rw) {
			System.out.println(rc.getName() + " " + rc.getShortMessage());
		}
		rw.close();
		git.close();
	}
	
	public static void blameOnFile(String repositoryUri, String localRepoCloneURI, String filepath) throws Exception {
		Git git = cloneRepo(repositoryUri, localRepoCloneURI);
		
        final BlameResult result = git.blame().setFilePath(filepath)
          .setTextComparator(RawTextComparator.WS_IGNORE_ALL).call();
        final RawText rawText = result.getResultContents();
        for (int i = 0; i < rawText.size(); i++) {
          final RevCommit sc = result.getSourceCommit(i);
          System.out.println(result.getSourceAuthor(i).getName() +
              (sc != null ? " " + sc.getCommitTime() + " " + sc.getName() : "") + ": " + rawText.getString(i));
        }
		git.close();
		
	}
}
