package org.jboss.historia.core;

import java.util.List;

import org.jboss.historia.core.UntestedCommitDetectionStrategy.FileUpdates;
import org.junit.Test;

public class UntestedCommitDetectionStrategyTest {
	
	@Test
	public void testProcessing() throws Exception {
		UntestedCommitDetectionStrategy s = new UntestedCommitDetectionStrategy("src/main/java");
		List<FileUpdates> list = s.process("https://github.com/jbossws/jbossws-api.git", "target/jgit/jbossws-api");
		for (FileUpdates f : list) {
			System.out.println(f);
		}
	}
}
