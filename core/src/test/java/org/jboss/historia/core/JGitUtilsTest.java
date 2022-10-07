package org.jboss.historia.core;

import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

public class JGitUtilsTest {
	
	@Test
	public void testFoo() throws Exception {
		JGitUtils jgit = new JGitUtils("https://github.com/jbossws/jbossws-api.git", "target/jgit/jbossws-api");
		List<RevCommit> commitHistory = jgit.getFileHistory("src/main/java/org/jboss/ws/api/binding/JAXBBindingCustomization.java");
		System.out.println("-----------------");
		jgit.blameOnFile("src/main/java/org/jboss/ws/api/binding/JAXBBindingCustomization.java");
		System.out.println("-----------------");
		jgit.getChangedFiles(commitHistory.get(1), commitHistory.get(0));
		System.out.println("-----------------");
		jgit.getChangedFiles(commitHistory.get(2));
		System.out.println("-----------------");
		jgit.getFilesOnHEAD();
		jgit.close();
	}
}
