package org.jboss.historia.core;

import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.historia.core.UntestedCommitDetectionStrategy.FileUpdates;
import org.junit.Test;

public class UntestedCommitDetectionStrategyTest {
	
	@Test
	public void testProcessing() throws Exception {
		Logger LOGGER = Logger.getLogger(this.getClass());
		UntestedCommitDetectionStrategy s = new UntestedCommitDetectionStrategy("src/main/java");
		List<FileUpdates> list = s.process("https://github.com/jbossws/jbossws-spi.git", "target/jgit/jbossws-spi");
		for (FileUpdates f : list) {
			LOGGER.info(f);
		}
	}
}
