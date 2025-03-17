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
