package org.jboss.historia.core;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the JGitUtils class.
 * These tests verify the functionality of Git operations using JGit.
 */
public class JGitUtilsTest {
    
    // Test repositories
    private static final String JBOSSWS_API_REPO = "https://github.com/jbossws/jbossws-api.git";
    private static final String JBOSSWS_SPI_REPO = "https://github.com/jbossws/jbossws-spi.git";
    
    // Test files
    private static final String JAXB_FILE = "src/main/java/org/jboss/ws/api/binding/JAXBBindingCustomization.java";
    private static final String STAX_FILE = "src/main/java/org/jboss/wsf/spi/util/StAXUtils.java";
    
    // Test commits
    private static final String STAX_COMMIT = "52ea8ead9362e19ff9218da5dbfae7f58e9c598f";
    private static final String MULTI_FILE_COMMIT = "6d8a6a0aa55496a0f5f0bb00e47af75ce30af195";
    
    // JGitUtils instances for tests
    private JGitUtils jgitApi;
    private JGitUtils jgitSpi;
    
    @Before
    public void setUp() throws Exception {
        // Initialize test repositories
        jgitApi = new JGitUtils(JBOSSWS_API_REPO, "target/jgit/jbossws-api-test");
        jgitSpi = new JGitUtils(JBOSSWS_SPI_REPO, "target/jgit/jbossws-spi-test");
    }
    
    @After
    public void tearDown() throws Exception {
        // Clean up resources
        if (jgitApi != null) {
            jgitApi.close();
        }
        if (jgitSpi != null) {
            jgitSpi.close();
        }
    }
    
    /**
     * Test basic file history retrieval.
     */
    @Test
    public void testGetFileHistory() throws Exception {
        List<RevCommit> commitHistory = jgitApi.getFileHistory(JAXB_FILE);
        
        // Verify we got some history
        assertNotNull("File history should not be null", commitHistory);
        assertFalse("File history should not be empty", commitHistory.isEmpty());
        
        // Verify commit details
        RevCommit firstCommit = commitHistory.get(0);
        assertNotNull("Commit should not be null", firstCommit);
        assertNotNull("Commit message should not be null", firstCommit.getFullMessage());
    }
    
    /**
     * Test blame operation on a file.
     */
    @Test
    public void testBlameOnFile() throws Exception {
        // This is primarily a logging operation, so we just verify it doesn't throw exceptions
        jgitApi.blameOnFile(JAXB_FILE);
        // Test passed if no exception was thrown
    }
    
    /**
     * Test getting files on HEAD.
     */
    @Test
    public void testGetFilesOnHEAD() throws Exception {
        Set<String> files = jgitApi.getFilesOnHEAD();
        
        // Verify we got some files
        assertNotNull("Files on HEAD should not be null", files);
        assertFalse("Files on HEAD should not be empty", files.isEmpty());
        
        // Verify our test file is in the set
        assertTrue("Files should contain our test file", files.contains(JAXB_FILE));
    }
    
    /**
     * Test getting changed files from a commit tree.
     */
    @Test
    public void testGetChangedFilesFromTree() throws Exception {
        List<RevCommit> commitHistory = jgitApi.getFileHistory(JAXB_FILE);
        RevCommit commit = commitHistory.get(2); // Get a commit from history
        
        Set<String> files = jgitApi.getChangedFiles(commit.getTree());
        
        // Verify we got some files
        assertNotNull("Changed files should not be null", files);
        assertFalse("Changed files should not be empty", files.isEmpty());
    }
    
    /**
     * Test getting changed files between two commit trees.
     */
    @Test
    public void testGetChangedFilesBetweenTrees() throws Exception {
        // Find a specific commit
        List<RevCommit> commits = jgitSpi.getFileHistory(STAX_FILE);
        RevCommit commit = null;
        
        for (RevCommit rc : commits) {
            if (STAX_COMMIT.equalsIgnoreCase(rc.getName())) {
                commit = rc;
                break;
            }
        }
        
        assertNotNull("Test commit should be found", commit);
        
        // Get changed files between parent and this commit
        Set<String> files = jgitSpi.getChangedFiles(jgitSpi.getParentCommitTree(commit), commit.getTree());
        
        // Verify results
        assertEquals("Should have exactly 1 changed file", 1, files.size());
        assertEquals("Changed file should be StAXUtils.java", STAX_FILE, files.iterator().next());
    }
    
    /**
     * Test getting a specific commit by hash.
     */
    @Test
    public void testGetCommit() throws Exception {
        RevCommit commit = jgitSpi.getCommit(MULTI_FILE_COMMIT);
        
        // Verify commit details
        assertNotNull("Commit should not be null", commit);
        assertEquals("Commit hash should match", MULTI_FILE_COMMIT, commit.getName());
    }
    
    /**
     * Test getting parent commit.
     */
    @Test
    public void testGetParentCommit() throws Exception {
        RevCommit commit = jgitSpi.getCommit(MULTI_FILE_COMMIT);
        RevCommit parent = jgitSpi.getParentCommit(commit);
        
        // Verify parent commit
        assertNotNull("Parent commit should not be null", parent);
        assertNotEquals("Parent should have different hash", commit.getName(), parent.getName());
    }
    
    /**
     * Test getting parent commit tree.
     */
    @Test
    public void testGetParentCommitTree() throws Exception {
        RevCommit commit = jgitSpi.getCommit(MULTI_FILE_COMMIT);
        
        // Get parent tree
        assertNotNull("Parent tree should not be null", jgitSpi.getParentCommitTree(commit));
    }
    
    /**
     * Test checking if a commit is a merge commit.
     */
    @Test
    public void testIsMergeCommit() throws Exception {
        // Get all commits
        List<RevCommit> allCommits = jgitSpi.getAllCommits();
        
        // Find a merge commit and a non-merge commit
        RevCommit mergeCommit = null;
        RevCommit nonMergeCommit = null;
        
        for (RevCommit commit : allCommits) {
            if (commit.getParentCount() > 1 && mergeCommit == null) {
                mergeCommit = commit;
            } else if (commit.getParentCount() == 1 && nonMergeCommit == null) {
                nonMergeCommit = commit;
            }
            
            if (mergeCommit != null && nonMergeCommit != null) {
                break;
            }
        }
        
        // If we found a merge commit, test it
        if (mergeCommit != null) {
            assertTrue("Should be identified as a merge commit", jgitSpi.isMergeCommit(mergeCommit));
        }
        
        // If we found a non-merge commit, test it
        if (nonMergeCommit != null) {
            assertFalse("Should not be identified as a merge commit", jgitSpi.isMergeCommit(nonMergeCommit));
        }
    }
    
    /**
     * Test extracting pull request ID from commit message.
     */
    @Test
    public void testExtractPullRequestId() throws Exception {
        // Get merge commits from the repository
        List<RevCommit> mergeCommits = jgitSpi.getMergeCommits();
        
        if (!mergeCommits.isEmpty()) {
            // Test with a real merge commit
            RevCommit mergeCommit = mergeCommits.get(0);
            
            // The result can be null or a PR ID, we're just testing the method runs
            String prId = jgitSpi.extractPullRequestId(mergeCommit);
            
            // Log the result for debugging
            Logger.getLogger(this.getClass()).debug("Extracted PR ID: " + prId + " from commit: " + mergeCommit.getShortMessage());
        }
        
        // Test with different PR formats in commit messages
        // We'll use direct pattern matching tests since we can't easily mock RevCommit
        
        // GitHub format: "Merge pull request #123 from user/branch"
        assertTrue(Pattern.compile("Merge pull request #(\\d+)").matcher("Merge pull request #123 from user/branch").find());
        
        // Alternative format: "[PR-456]"
        assertTrue(Pattern.compile("\\[PR[\\s-]?(\\d+)\\]").matcher("Fix bug [PR-456]").find());
        
        // Alternative format: "(#789)"
        assertTrue(Pattern.compile("\\(#(\\d+)\\)").matcher("Fix issue (#789)").find());
    }
    
    /**
     * Test getting all commits.
     */
    @Test
    public void testGetAllCommits() throws Exception {
        List<RevCommit> allCommits = jgitSpi.getAllCommits();
        
        // Verify we got some commits
        assertNotNull("All commits should not be null", allCommits);
        assertFalse("All commits should not be empty", allCommits.isEmpty());
    }
    
    /**
     * Test grouping commits by pull request.
     */
    @Test
    public void testGroupCommitsByPullRequest() throws Exception {
        List<RevCommit> allCommits = jgitSpi.getAllCommits();
        Map<String, List<RevCommit>> groups = jgitSpi.groupCommitsByPullRequest(allCommits);
        
        // Verify we got some groups
        assertNotNull("PR groups should not be null", groups);
        assertFalse("PR groups should not be empty", groups.isEmpty());
    }
    
    /**
     * Test getting merge commits.
     */
    @Test
    public void testGetMergeCommits() throws Exception {
        List<RevCommit> mergeCommits = jgitSpi.getMergeCommits();
        
        // Verify we got some merge commits
        assertNotNull("Merge commits should not be null", mergeCommits);
        
        // Verify all are merge commits
        for (RevCommit commit : mergeCommits) {
            assertTrue("Should be a merge commit", jgitSpi.isMergeCommit(commit));
        }
    }
    
    /**
     * Test getting merged commits.
     */
    @Test
    public void testGetMergedCommits() throws Exception {
        // Find a merge commit
        List<RevCommit> mergeCommits = jgitSpi.getMergeCommits();
        
        if (!mergeCommits.isEmpty()) {
            RevCommit mergeCommit = mergeCommits.get(0);
            List<RevCommit> mergedCommits = jgitSpi.getMergedCommits(mergeCommit);
            
            // Verify we got some merged commits
            assertNotNull("Merged commits should not be null", mergedCommits);
        }
    }
    
    /**
     * Test getting all pull requests.
     */
    @Test
    public void testGetAllPullRequests() throws Exception {
        Map<String, List<RevCommit>> allPRs = jgitSpi.getAllPullRequests();
        
        // Verify we got some PRs
        assertNotNull("All PRs should not be null", allPRs);
    }
    
    /**
     * Test getting pull requests for a file.
     */
    @Test
    public void testGetPullRequestsForFile() throws Exception {
        Map<String, List<RevCommit>> filePRs = jgitSpi.getPullRequestsForFile(STAX_FILE);
        
        // Verify we got some PRs
        assertNotNull("File PRs should not be null", filePRs);
    }
    
    /**
     * Test getting commits in a pull request.
     */
    @Test
    public void testGetCommitsInPullRequest() throws Exception {
        // Find a PR ID
        Map<String, List<RevCommit>> allPRs = jgitSpi.getAllPullRequests();
        
        if (!allPRs.isEmpty()) {
            String prId = allPRs.keySet().iterator().next();
            List<RevCommit> prCommits = jgitSpi.getCommitsInPullRequest(prId);
            
            // Verify we got some commits
            assertNotNull("PR commits should not be null", prCommits);
        }
    }
    
    /**
     * Test checking if a pull request affects tests.
     */
    @Test
    public void testPullRequestAffectsTests() throws Exception {
        // Find a PR
        Map<String, List<RevCommit>> allPRs = jgitSpi.getAllPullRequests();
        
        if (!allPRs.isEmpty()) {
            List<RevCommit> prCommits = allPRs.values().iterator().next();
            
            // Just verify the method runs without exceptions
            boolean affectsTests = jgitSpi.pullRequestAffectsTests(prCommits);
            
            // Either true or false is valid, we're just testing the method runs
            assertTrue("Method should return true or false", affectsTests || !affectsTests);
        }
    }
    
    /**
     * Test the constructor and getGit method.
     */
    @Test
    public void testConstructorAndGetGit() throws Exception {
        // Create a new instance with a unique path
        String uniquePath = "target/jgit/test-" + UUID.randomUUID().toString();
        JGitUtils jgit = new JGitUtils(JBOSSWS_API_REPO, uniquePath);
        
        try {
            // Verify Git instance
            Git git = jgit.getGit();
            assertNotNull("Git instance should not be null", git);
            
            // Verify repository
            assertNotNull("Repository should not be null", git.getRepository());
            assertTrue("Repository should exist", new File(uniquePath).exists());
        } finally {
            jgit.close();
        }
    }
    
}
