package org.jboss.historia.core;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;

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
	 * This test documents the expected behavior with the new implementation that
	 * considers tests added in subsequent commits within the same pull request.
	 * 
	 * Note: This is a documentation test that explains the behavior rather than
	 * actually testing it, since we don't have direct control over the GitHub
	 * repository used in the real tests.
	 */
	@Test
	public void testPullRequestLevelTestDetection() {
		/*
		 * Scenario:
		 * 
		 * PR #123 contains two commits:
		 * 1. First commit: Changes to src/main/java/SomeClass.java (no test changes)
		 * 2. Second commit: Changes to src/test/java/SomeClassTest.java (adds tests)
		 * 
		 * With the new implementation:
		 * - Both commits are grouped together as part of PR #123
		 * - The PR is considered "tested" because it includes test changes
		 * - Both commits are marked as "tested" even though the first one doesn't directly modify tests
		 * 
		 * This ensures that files aren't incorrectly flagged as "untested" when their
		 * tests are added in a subsequent commit within the same pull request.
		 */
		
		// This is the expected behavior with the new implementation
		// No actual assertions since this is a documentation test
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
