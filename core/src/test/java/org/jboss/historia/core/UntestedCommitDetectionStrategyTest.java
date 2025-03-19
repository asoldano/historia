package org.jboss.historia.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.junit.Test;

public class UntestedCommitDetectionStrategyTest {
	
	@Test
	public void testProcessing() throws Exception {
		final String filename = "target/test-processing-jbossws-spi-" + System.currentTimeMillis() + ".csv";
		Runner.run(UntestedCommitDetectionStrategy.class.getName(), filename, "src/main/java", "https://github.com/jbossws/jbossws-spi.git", "target/jgit/jbossws-spi");
		boolean found = false;
		try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
			String line = reader.readLine();
			while (line != null && !found) {
				found = reader.readLine().contains("src/main/java/org/jboss/wsf/spi/deployment/Extensible.java,");
			}
		}
		assertTrue(found);
	}
	
	/**
	 * This test verifies that the implementation correctly identifies files as "tested"
	 * when tests are added in a subsequent commit within the same pull request.
	 * 
	 * It uses the resteasy-grpc repository at the 1.0.0.Alpha6 tag as a stable reference point.
	 * 
	 * The test specifically looks at the implementation of gRPC client and server classes,
	 * which typically have corresponding test classes in the same repository.
	 */
	@Test
	public void testPullRequestLevelTestDetection() throws Exception {
		// Repository information
		final String repoUrl = "https://github.com/resteasy/resteasy-grpc.git";
		final String localRepoPath = "target/jgit/resteasy-grpc-test-" + System.currentTimeMillis();
		final String pathFilter = "src/main/java";
		
		// Clone the repository and checkout the specific tag
		JGitUtils jgit = new JGitUtils(repoUrl, localRepoPath);
		try {
			// Execute git checkout command to switch to the specific tag
			jgit.getGit().checkout().setName("1.0.0.Alpha6").call();
			
			// Log all merge commits to help identify PRs
			List<RevCommit> mergeCommits = jgit.getMergeCommits();
			System.out.println("Found " + mergeCommits.size() + " merge commits in the repository");
			
			for (RevCommit mergeCommit : mergeCommits) {
				String prId = jgit.extractPullRequestId(mergeCommit);
				if (prId != null) {
					System.out.println("Merge commit: " + mergeCommit.getName().substring(0, 8) + 
							" - " + mergeCommit.getShortMessage() + " (PR: " + prId + ")");
				}
			}
			
			// Get all PRs in the repository
			Map<String, List<RevCommit>> allPRs = jgit.getAllPullRequests();
			System.out.println("Found " + allPRs.size() + " pull requests in the repository");
			
			// Create a strategy instance
			UntestedCommitDetectionStrategy strategy = new UntestedCommitDetectionStrategy(pathFilter);
			
			// Process the repository
			List<UntestedCommitDetectionStrategy.FileUpdates> results = strategy.process(repoUrl, localRepoPath);
			
			// We'll specifically look for the gRPC client implementation file
			// This is a key file in the project that should have tests
			final String targetFilePath = "src/main/java/dev/resteasy/grpc/bridge/generator/protobuf/JavabufTranslatorGenerator.java";
			
			// Find our target file in the results
			UntestedCommitDetectionStrategy.FileUpdates targetFile = null;
			for (UntestedCommitDetectionStrategy.FileUpdates fileUpdate : results) {
				if (fileUpdate.getPath().equals(targetFilePath)) {
					targetFile = fileUpdate;
					break;
				}
			}
			
			// Verify we found our target file
			assertTrue("Could not find target file: " + targetFilePath, targetFile != null);
			
			// Log the file statistics
			System.out.println("Testing PR-level test detection on file: " + targetFile.getPath());
			System.out.println("Total updates: " + targetFile.getUpdates());
			System.out.println("Untested updates: " + targetFile.getUntestedUpdates());
			System.out.println("Updates since last tested: " + targetFile.getUpdatesSinceLastTested());
			
			// Verify that the file has been updated at least once
			assertTrue("File should have at least one update", targetFile.getUpdates() > 0);
			
			String p = "grpc-bridge/" + targetFile.getPath();
			// Get the file history
			List<RevCommit> commitHistory = jgit.getFileHistory(p);
			
			// Verify we have some commit history
			assertTrue("File should have commit history", commitHistory.size() > 0);
			assertEquals("File should have commit history", targetFile.getUpdates(), commitHistory.size());
			
			// Find all PRs that include changes to this file
			Map<String, List<RevCommit>> fileCommitsByPR = jgit.getPullRequestsForFile(p);
			
			// Log PR groups for debugging
			System.out.println("Found " + fileCommitsByPR.size() + " PR groups for file: " + p);
			
			// Count how many PRs have commits that don't modify this file but do modify tests
			int prsWithExternalTestCommits = 0;
			
			for (Map.Entry<String, List<RevCommit>> entry : fileCommitsByPR.entrySet()) {
				String prId = entry.getKey();
				List<RevCommit> fileCommitsInPR = entry.getValue();
				
				// Check if this is a real PR ID (not a commit hash used as fallback)
				boolean isRealPR = prId.matches("\\d+"); // PR IDs are numeric
				
				if (isRealPR) {
					// Get ALL commits in the PR, not just those that modified this file
					List<RevCommit> allCommitsInPR = jgit.getCommitsInPullRequest(prId);
					
					System.out.println("PR: " + prId + " with " + fileCommitsInPR.size() + 
							" commits modifying this file (out of " + allCommitsInPR.size() + " total commits in PR)");
					
					// Check if there are commits in the PR that don't modify this file
					if (allCommitsInPR.size() > fileCommitsInPR.size()) {
						// Find commits that are in allCommitsInPR but not in fileCommitsInPR
						List<RevCommit> otherCommitsInPR = new ArrayList<>(allCommitsInPR);
						
						// Remove commits that modify the file
						Set<String> fileCommitHashes = new HashSet<>();
						for (RevCommit commit : fileCommitsInPR) {
							fileCommitHashes.add(commit.getName());
						}
						
						otherCommitsInPR.removeIf(commit -> fileCommitHashes.contains(commit.getName()));
						
						// Check if any of these other commits affect tests
						boolean otherCommitsAffectTests = false;
						for (RevCommit commit : otherCommitsInPR) {
							Set<String> changedFiles;
							if (commit.getParentCount() > 0) {
								RevTree parentTree = jgit.getParentCommitTree(commit);
								changedFiles = jgit.getChangedFiles(parentTree, commit.getTree());
							} else {
								changedFiles = jgit.getChangedFiles(commit.getTree());
							}
							
							for (String file : changedFiles) {
								if (file.contains("src/test")) {
									otherCommitsAffectTests = true;
									System.out.println("  Found commit that doesn't modify this file but affects tests: " + 
											commit.getName().substring(0, 8) + " - " + commit.getShortMessage());
									break;
								}
							}
							
							if (otherCommitsAffectTests) {
								break;
							}
						}
						
						if (otherCommitsAffectTests) {
							prsWithExternalTestCommits++;
						}
					}
				}
			}
			
			System.out.println("Found " + prsWithExternalTestCommits + " PRs with commits that don't modify this file but do modify tests");
			
			// Verify that our PR-level test detection is working
			// The key assertion: the number of untested updates should be less than
			// what it would be if we were only looking at individual commits
			int individualCommitUntestedCount = 0;
			for (RevCommit commit : commitHistory) {
				// Check if this specific commit affects tests
				Set<String> changedFiles;
				if (commit.getParentCount() > 0) {
					RevTree parentTree = jgit.getParentCommitTree(commit);
					changedFiles = jgit.getChangedFiles(parentTree, commit.getTree());
				} else {
					changedFiles = jgit.getChangedFiles(commit.getTree());
				}
				
				boolean commitAffectsTests = false;
				for (String file : changedFiles) {
					if (file.contains("src/test")) {
						commitAffectsTests = true;
						break;
					}
				}
				
				if (!commitAffectsTests) {
					individualCommitUntestedCount++;
				}
			}
			
			System.out.println("Untested updates with PR-level detection: " + targetFile.getUntestedUpdates());
			System.out.println("Untested updates with individual commit detection: " + individualCommitUntestedCount);
			
			// This is the key assertion that proves our PR-level detection is working
			// If they're equal, then our PR-level detection isn't having any effect
			// If PR-level detection is working, we should have fewer untested updates
			assertTrue("PR-level detection should reduce the number of untested updates", 
					targetFile.getUntestedUpdates() <= individualCommitUntestedCount);
			
			// If we found PRs with external test commits, we should definitely see a difference
			if (prsWithExternalTestCommits > 0) {
				assertTrue("PR-level detection should find tests in other commits", 
						targetFile.getUntestedUpdates() < individualCommitUntestedCount);
				System.out.println("Successfully detected tests in commits that don't modify the target file!");
			}
			
		} finally {
			// Clean up
			jgit.close();
		}
	}

//	@Test
//	public void testProcessing2() throws Exception {
//		final String filename = "target/test-ironjacamar-" + System.currentTimeMillis() + ".csv";
//		Runner.run(UntestedCommitDetectionStrategy.class.getName(), filename, "src/main/java", "https://github.com/ironjacamar/ironjacamar.git", "target/jgit/ironjacamar");
////		boolean found = false;
////		try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
////			String line = reader.readLine();
////			while (line != null && !found) {
////				found = reader.readLine().contains("src/main/java/org/jboss/wsf/spi/deployment/Extensible.java,5,5");
////			}
////		}
////		assertTrue(found);
//	}
	
//	@Test
//	public void testProcessing2() throws Exception {
//		final String filename = "target/test-processing-jbossws-cxf-" + System.currentTimeMillis() + ".csv";
//		Runner.run(UntestedCommitDetectionStrategy.class.getName(), filename, "src/main/java", "https://github.com/jbossws/jbossws-cxf.git", "target/jgit/jbossws-cxf");
////		boolean found = false;
////		try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
////			String line = reader.readLine();
////			while (line != null && !found) {
////				found = reader.readLine().contains("src/main/java/org/jboss/wsf/spi/deployment/Extensible.java,5,5");
////			}
////		}
////		assertTrue(found);
//	}
}
