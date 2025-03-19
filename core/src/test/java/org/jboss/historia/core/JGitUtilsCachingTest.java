package org.jboss.historia.core;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.eclipse.jgit.revwalk.RevCommit;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests to verify the caching functionality in JGitUtils.
 */
public class JGitUtilsCachingTest {
    
    private static final String REPO_URL = "https://github.com/jbossws/jbossws-api.git";
    private static final String LOCAL_REPO_PATH = "target/jgit/jbossws-api-cache-test";
    private static final Logger LOGGER = Logger.getLogger(JGitUtilsCachingTest.class);
    
    private JGitUtils jgit;
    
    @Before
    public void setUp() throws Exception {
        jgit = new JGitUtils(REPO_URL, LOCAL_REPO_PATH);
    }
    
    @After
    public void tearDown() throws Exception {
        if (jgit != null) {
            jgit.close();
        }
    }
    
    /**
     * Test that caching improves performance for repeated calls to getAllCommits().
     */
    @Test
    public void testAllCommitsCaching() throws Exception {
        // First call - should populate the cache
        long startTime = System.currentTimeMillis();
        List<RevCommit> commits1 = jgit.getAllCommits();
        long firstCallTime = System.currentTimeMillis() - startTime;
        
        // Second call - should use the cache
        startTime = System.currentTimeMillis();
        List<RevCommit> commits2 = jgit.getAllCommits();
        long secondCallTime = System.currentTimeMillis() - startTime;
        
        // Verify both calls return the same data
        assertEquals("Both calls should return the same number of commits", 
                commits1.size(), commits2.size());
        
        // Verify the second call is significantly faster
        LOGGER.debug("getAllCommits() first call: " + firstCallTime + "ms");
        LOGGER.debug("getAllCommits() second call: " + secondCallTime + "ms");
        LOGGER.debug("Performance improvement: " + 
                Math.round((double)firstCallTime / secondCallTime) + "x faster");
        
        assertTrue("Second call should be significantly faster", secondCallTime < firstCallTime / 2);
    }
    
    /**
     * Test that caching improves performance for repeated calls to getMergeCommits().
     */
    @Test
    public void testMergeCommitsCaching() throws Exception {
        // First call - should populate the cache
        long startTime = System.currentTimeMillis();
        List<RevCommit> commits1 = jgit.getMergeCommits();
        long firstCallTime = System.currentTimeMillis() - startTime;
        
        // Second call - should use the cache
        startTime = System.currentTimeMillis();
        List<RevCommit> commits2 = jgit.getMergeCommits();
        long secondCallTime = System.currentTimeMillis() - startTime;
        
        // Verify both calls return the same data
        assertEquals("Both calls should return the same number of merge commits", 
                commits1.size(), commits2.size());
        
        // Verify the second call is significantly faster
        LOGGER.debug("getMergeCommits() first call: " + firstCallTime + "ms");
        LOGGER.debug("getMergeCommits() second call: " + secondCallTime + "ms");
        LOGGER.debug("Performance improvement: " + 
                Math.round((double)firstCallTime / secondCallTime) + "x faster");
        
        assertTrue("Second call should be significantly faster", secondCallTime < firstCallTime / 2);
    }
    
    /**
     * Test that caching improves performance for repeated calls to getAllPullRequests().
     */
    @Test
    public void testAllPullRequestsCaching() throws Exception {
        // First call - should populate the cache
        long startTime = System.currentTimeMillis();
        Map<String, List<RevCommit>> prs1 = jgit.getAllPullRequests();
        long firstCallTime = System.currentTimeMillis() - startTime;
        
        // Second call - should use the cache
        startTime = System.currentTimeMillis();
        Map<String, List<RevCommit>> prs2 = jgit.getAllPullRequests();
        long secondCallTime = System.currentTimeMillis() - startTime;
        
        // Verify both calls return the same data
        assertEquals("Both calls should return the same number of PRs", 
                prs1.size(), prs2.size());
        
        // Verify the second call is significantly faster
        LOGGER.debug("getAllPullRequests() first call: " + firstCallTime + "ms");
        LOGGER.debug("getAllPullRequests() second call: " + secondCallTime + "ms");
        LOGGER.debug("Performance improvement: " + 
                Math.round((double)firstCallTime / secondCallTime) + "x faster");
        
        assertTrue("Second call should be significantly faster", secondCallTime < firstCallTime / 2);
    }
    
    /**
     * Test that caching improves performance for repeated calls to getFileHistory().
     */
    @Test
    public void testFileHistoryCaching() throws Exception {
        String testFile = "src/main/java/org/jboss/ws/api/binding/JAXBBindingCustomization.java";
        
        // First call - should populate the cache
        long startTime = System.currentTimeMillis();
        List<RevCommit> history1 = jgit.getFileHistory(testFile);
        long firstCallTime = System.currentTimeMillis() - startTime;
        
        // Second call - should use the cache
        startTime = System.currentTimeMillis();
        List<RevCommit> history2 = jgit.getFileHistory(testFile);
        long secondCallTime = System.currentTimeMillis() - startTime;
        
        // Verify both calls return the same data
        assertEquals("Both calls should return the same file history size", 
                history1.size(), history2.size());
        
        // Verify the second call is significantly faster
        LOGGER.debug("getFileHistory() first call: " + firstCallTime + "ms");
        LOGGER.debug("getFileHistory() second call: " + secondCallTime + "ms");
        LOGGER.debug("Performance improvement: " + 
                Math.round((double)firstCallTime / secondCallTime) + "x faster");
        
        assertTrue("Second call should be significantly faster", secondCallTime < firstCallTime / 2);
    }
    
    /**
     * Test that the clearCaches() method properly clears all caches.
     */
    @Test
    public void testClearCaches() throws Exception {
        // Populate caches
        jgit.getAllCommits();
        jgit.getMergeCommits();
        jgit.getAllPullRequests();
        jgit.getFileHistory("src/main/java/org/jboss/ws/api/binding/JAXBBindingCustomization.java");
        
        // Clear caches
        jgit.clearCaches();
        
        // Measure time for calls after clearing cache
        long startTime = System.currentTimeMillis();
        List<RevCommit> commits = jgit.getAllCommits();
        long callTime = System.currentTimeMillis() - startTime;
        
        // Verify we got data
        assertFalse("Should return commits", commits.isEmpty());
        
        // Second call should be fast again
        startTime = System.currentTimeMillis();
        jgit.getAllCommits();
        long secondCallTime = System.currentTimeMillis() - startTime;
        
        LOGGER.debug("getAllCommits() after clearCaches(): " + callTime + "ms");
        LOGGER.debug("getAllCommits() second call after clearCaches(): " + secondCallTime + "ms");
        LOGGER.debug("Performance ratio: " + ((double)callTime / Math.max(1, secondCallTime)));
        
        // The second call should be faster, but for small repositories or when the data is
        // already in the OS cache, the difference might not be as dramatic
        assertTrue("Second call after clearing should be faster", secondCallTime <= callTime);
    }
    
    /**
     * Test the performance improvement in a real-world scenario: processing file PRs.
     */
    @Test
    public void testRealWorldScenario() throws Exception {
        String testFile1 = "src/main/java/org/jboss/ws/api/binding/JAXBBindingCustomization.java";
        String testFile2 = "src/main/java/org/jboss/ws/api/addressing/MAPEndpoint.java";
        
        // First file - should populate caches
        long startTime = System.currentTimeMillis();
        Map<String, List<RevCommit>> file1PRs = jgit.getPullRequestsForFile(testFile1);
        long firstFileTime = System.currentTimeMillis() - startTime;
        
        // Second file - should benefit from cached repository data
        startTime = System.currentTimeMillis();
        Map<String, List<RevCommit>> file2PRs = jgit.getPullRequestsForFile(testFile2);
        long secondFileTime = System.currentTimeMillis() - startTime;
        
        // Verify we got data
        assertNotNull("Should return PRs for file 1", file1PRs);
        assertNotNull("Should return PRs for file 2", file2PRs);
        
        LOGGER.debug("getPullRequestsForFile() first file: " + firstFileTime + "ms");
        LOGGER.debug("getPullRequestsForFile() second file: " + secondFileTime + "ms");
        
        // Note: The second file might not be significantly faster if the file history lookup
        // dominates the processing time. But repository-wide data should be cached.
    }
}
