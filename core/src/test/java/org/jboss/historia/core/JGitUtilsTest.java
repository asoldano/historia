package org.jboss.historia.core;

import org.junit.Test;

public class JGitUtilsTest {
	
	@Test
	public void testFoo() throws Exception {
		JGitUtils.showFileHistory("https://github.com/jbossws/jbossws-api.git", "target/jgit/jbossws-api", "src/main/java/org/jboss/ws/api/binding/JAXBBindingCustomization.java");
		System.out.println("-----------------");
		JGitUtils.blameOnFile("https://github.com/jbossws/jbossws-api.git", "target/jgit/jbossws-api", "src/main/java/org/jboss/ws/api/binding/JAXBBindingCustomization.java");
	}
}
