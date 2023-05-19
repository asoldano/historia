package org.jboss.historia.core;

import java.io.BufferedWriter;
import java.io.FileWriter;

import org.jboss.logging.Logger;

public final class Runner {

	public static void main(String[] args) {
		Logger logger = Logger.getLogger(Runner.class);
		if (args == null || args.length != 5) {
			logger.info("Usage: org.jboss.historia.core.Runner <strategy class> <output file> <path filter> <git repo uri> <local repo clone uri>\n"
					+ "Example: org.jboss.historia.core.Runner org.jboss.historia.core.UntestedCommitDetectionStrategy ./output.csv src/main/java https://github.com/jbossws/jbossws-spi.git target/jgit/jbossws-spi");
			return;
		}
		String strategy = args[0];
		String filename = args[1];
		String pathFilter = args[2];
		String gitRepoUri = args[3];
		String localRepoCloneUri = args[4];
		try {
			run(strategy, filename, pathFilter, gitRepoUri, localRepoCloneUri);
			logger.info("Process completed, see results in " + filename);
		} catch (Exception e) {
			Logger.getLogger(Runner.class).error("Exception running " + strategy, e);
		}
	}

	public static void run(String strategy, String filename, String pathFilter, String gitRepoUri, String localRepoCloneUri) throws Exception {
		if (UntestedCommitDetectionStrategy.class.getName().equals(strategy)) {
			UntestedCommitDetectionStrategy s = new UntestedCommitDetectionStrategy(pathFilter);
			try (FileWriter writer = new FileWriter(filename, true); BufferedWriter bw = new BufferedWriter(writer)) {
				s.process(gitRepoUri, localRepoCloneUri, bw);
			}
		} else {
			throw new Exception("Strategy not found!");
		}
	}
}
