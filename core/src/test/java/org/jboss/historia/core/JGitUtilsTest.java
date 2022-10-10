package org.jboss.historia.core;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Test;

public class JGitUtilsTest {
	
	@Test
	public void testFoo() throws Exception {
		JGitUtils jgit = new JGitUtils("https://github.com/jbossws/jbossws-api.git", "target/jgit/jbossws-api");
		List<RevCommit> commitHistory = jgit.getFileHistory("src/main/java/org/jboss/ws/api/binding/JAXBBindingCustomization.java");
		jgit.blameOnFile("src/main/java/org/jboss/ws/api/binding/JAXBBindingCustomization.java");
		jgit.getChangedFiles(commitHistory.get(2).getTree());
		jgit.getFilesOnHEAD();
		jgit.close();
	}
	
	@Test
	public void testCommitDiffsAfterHistory() throws Exception {
		JGitUtils jgit = new JGitUtils("https://github.com/jbossws/jbossws-spi.git", "target/jgit/jbossws-spi");
		List<RevCommit> commits = jgit.getFileHistory("src/main/java/org/jboss/wsf/spi/util/StAXUtils.java");
		RevCommit commit = null;
		Iterator<RevCommit> it = commits.iterator();
		while (commit == null && it.hasNext()) {
			RevCommit rc = it.next();
			//See https://github.com/jbossws/jbossws-spi/commit/52ea8ead9362e19ff9218da5dbfae7f58e9c598f
			if ("52ea8ead9362e19ff9218da5dbfae7f58e9c598f".equalsIgnoreCase(rc.getName())) {
				commit = rc;
			}
		}
		Set<String> files = jgit.getChangedFiles(jgit.getParentCommitTree(commit), commit.getTree());
		Assert.assertEquals(1, files.size());
		Assert.assertEquals("src/main/java/org/jboss/wsf/spi/util/StAXUtils.java", files.iterator().next());
		jgit.close();
	}
	
	@Test
	public void testCommitDiffs() throws Exception {
		JGitUtils jgit = new JGitUtils("https://github.com/jbossws/jbossws-spi.git", "target/jgit/jbossws-spi");
		//https://github.com/jbossws/jbossws-spi/commit/6d8a6a0aa55496a0f5f0bb00e47af75ce30af195
		RevCommit commit = jgit.getCommit("6d8a6a0aa55496a0f5f0bb00e47af75ce30af195");
		Set<String> files = jgit.getChangedFiles(jgit.getParentCommitTree(commit), commit.getTree());
		Assert.assertEquals(14, files.size());
		Assert.assertTrue(files.contains("pom.xml"));
		Assert.assertTrue(files.contains("src/main/java/org/jboss/wsf/spi/metadata/ParserConstants.java"));
		Assert.assertTrue(files.contains("src/main/java/org/jboss/wsf/spi/metadata/config/ConfigMetaDataParser.java"));
		Assert.assertTrue(files.contains("src/test/resources/metadata/config/test-jaxws-config-jakarta.xml"));
		jgit.close();
	}

}
