package org.jboss.historia.core;

import java.net.URI;

//import com.atlassian.jira.rest.client.api.JiraRestClient;
//import com.atlassian.jira.rest.client.api.domain.Issue;
//import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

/**
 * 
 * @author alessio
 *
 */
public class JiraUtils {
	
	public static void test(String uri, String authToken) throws Exception {
		
//		final AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
//		final JiraRestClient restClient = factory.create(new URI(uri), new BearerTokenAuthenticationHandler(authToken));
//		try {
//			final Issue issue = restClient.getIssueClient().getIssue("JBWS-2000").claim();
//			System.out.println("id: " + issue.getId() + ", key: " + issue.getKey() + ", type: " + issue.getIssueType().getName() + ", summary: " + issue.getSummary() + ", description: " + issue.getDescription());
//		}
//		finally {
//			restClient.close();
//		}
	}
}
