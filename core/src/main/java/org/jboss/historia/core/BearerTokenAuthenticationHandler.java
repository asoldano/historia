package org.jboss.historia.core;

//import com.atlassian.httpclient.api.Request;
//import com.atlassian.jira.rest.client.api.AuthenticationHandler;

public class BearerTokenAuthenticationHandler //implements AuthenticationHandler
{

	private final String token;

	public BearerTokenAuthenticationHandler(final String token) {
		this.token = token;
	}

//	@Override
//	public void configure(Request.Builder builder) {
//		builder.setHeader("Authorization", "Bearer " + token);
//	}
}
