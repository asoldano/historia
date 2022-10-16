package org.jboss.historia.core;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import org.junit.Test;

public class UntestedCommitDetectionStrategyTest {
	
	@Test
	public void testProcessing() throws Exception {
		final String filename = "target/test-processing-jbossws-spi-" + System.currentTimeMillis() + ".csv";
		UntestedCommitDetectionStrategy s = new UntestedCommitDetectionStrategy("src/main/java", true);
		try (FileWriter writer = new FileWriter(filename, true); BufferedWriter bw = new BufferedWriter(writer)) {
			s.process("https://github.com/jbossws/jbossws-spi.git", "target/jgit/jbossws-spi", bw);
		}
		boolean found = false;
		try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
			String line = reader.readLine();
			while (line != null && !found) {
				found = reader.readLine().contains("src/main/java/org/jboss/wsf/spi/deployment/Extensible.java,5,5");
			}
		}
		assertTrue(found);
	}

//	@Test
//	public void testProcessing2() throws Exception {
//		final String filename = "target/test-processing-resteasy-" + System.currentTimeMillis() + ".csv";
//		UntestedCommitDetectionStrategy s = new UntestedCommitDetectionStrategy("src/main/java", false);
//		try (FileWriter writer = new FileWriter(filename, true); BufferedWriter bw = new BufferedWriter(writer)) {
//			s.process("https://github.com/resteasy/resteasy.git", "target/jgit/resteasy", bw);
//		}
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
