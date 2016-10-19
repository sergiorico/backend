package se.lth.cs.connect;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.junit.Rule;
import org.junit.Test;

import com.jayway.restassured.filter.session.SessionFilter;
import com.jayway.restassured.response.Response;

import ro.pippo.core.PippoConstants;
import ro.pippo.test.PippoRule;
import ro.pippo.test.PippoTest;
import se.lth.cs.connect.Mailbox.Mail;
import utils.URLParser;

public class ITUserAPI extends PippoTest {

	// This rule ensures that we have a server running before doing the tests
	public Connect app = new Connect();
	@Rule
	public PippoRule pippoRule = new PippoRule(app);

	@Test
	public void testAccessDenied() {
		// These routes require an active session => requests should be denied
		// Status code 401 => all is ok (expected)
		// Any other code means that we have an error somewhere.
		when().get("/v1/account/login").then().statusCode(401);
		when().get("/v1/account/collections").then().statusCode(401);
		when().get("/v1/account/self").then().statusCode(401);
		when().post("/v1/account/logout").then().statusCode(401);
		when().post("/v1/account/delete").then().statusCode(401);
		when().post("/v1/account/change-password").then().statusCode(401);
		when().get("/v1/account/invites").then().statusCode(401);
		when().get("/v1/account/dat12asm@student.lu.se").then().statusCode(401);
	}

	private void register(String email, String passw, SessionFilter creds) {
		Response reg = given().
				filter(creds).
				param("email", email).
				param("passw", passw).
			post("/v1/account/register");

		String body = reg.getBody().prettyPrint();
		
		reg.then().statusCode(200);
	}

	private void delete(SessionFilter creds) {
		given().
			filter(creds).
		when().
			post("/v1/account/delete").
		then().
			statusCode(200);
	}

	/**
	 * Verify that the /login route returns 200 when logged in.
	 */
	@Test
	public void testLogin() {
		SessionFilter session = new SessionFilter();
		register("test-login@serp", "1234", session);

		given().
			filter(session).
		when().
			get("/v1/account/login").
		then().
			statusCode(200);

		delete(session);
	}

	/**
	 * Verify that 1) an email is sent after signup, and 2) that the email
	 * is sent to the correct email address.
	 */
	@Test
	public void testRegistration() throws UnsupportedEncodingException {
		// Use mailbox to capture emails instead of sending them
		Mailbox mailbox = new Mailbox();
		app.useMailClient(mailbox);
		
		SessionFilter session = new SessionFilter();
		
		register("test-reg@serptest.test", "hejsanhoppsan", session);
	
		assertThat("Must send registration email", mailbox.getInbox().size(), is(1));
		assertThat("Recipient must match registered email",
				mailbox.top().recipient, is(equalTo("test-reg@serptest.test")));
		
		String verify = URLParser.find(mailbox.top().content);
		verify = verify.substring(verify.indexOf("token=") + 6);
		verify = URLDecoder.decode(verify, PippoConstants.UTF8);
		assertThat("Must find link in email", verify, not(equalTo("")));		

		given().
			//filter(sessionFilter).
			param("token", verify).
		expect().
			statusCode(200).
		when().
			get("/v1/account/verify");
		
		// Cleanup
		delete(session);
	}

	@Test
	public void testLifecycle() {
		// This test case is not very important, but it showcases how to
		// (re)use a session for multiple queries and how to create post
		// requests with data.
		SessionFilter session = new SessionFilter();
		register("test@serptest.test", "hejsanhoppsan", session);

		given().
			filter(session).
		expect().
			statusCode(200).
		when().
			post("/v1/account/logout");

		given().
			filter(session).
			param("email", "test@serptest.test").
			param("passw", "hejsanhoppsan").
		expect().
			statusCode(200).
		when().
			post("/v1/account/login");

		delete(session);
	}
	
	//this testcase might break if format of the reset password email is changed
	@Test
	public void testResetPassword() throws UnsupportedEncodingException{
		Mailbox mailbox = new Mailbox();
		app.useMailClient(mailbox);
		
		SessionFilter session = new SessionFilter();
		
		// Register
		register("test-reset@serptest.test", "hejsanhoppsan",session);
		
		
		//logout
		given().
			filter(session).
		when().
			post("/v1/account/logout").
		then().
			statusCode(200);
		
		//ask for reset email
		Response reset = given().
				param("email", "test-reset@serptest.test").
				expect().statusCode(200).when().
			post("/v1/account/reset-password");
		
		//filter mail for the token
		Mail test = mailbox.top();
		String[] split = test.content.split("token=");
		String[] token = split[1].split("\n");
		String verify = URLDecoder.decode(token[0], PippoConstants.UTF8);

		//verify token
		//stop redirect to avoid breaking the session
		String sessionId = given().redirects().follow(false).
				param("token", verify).
				expect().statusCode(302).when().
				get("/v1/account/reset-password").andReturn().sessionId();
		
		//System.err.println("after: " + sessionId);

		//set new password
		given().
			cookie("JSESSIONID", sessionId).
			param("passw","hej123").
		expect().statusCode(200).when().
			post("/v1/account/reset-password-confirm");
	
		//session gets wrecked by redirect no need to logout

		//login with new password
		given().
		filter(session).
		param("email", "test-reset@serptest.test").
		param("passw", "hej123").
		when().
		post("/v1/account/login").
		then().
		statusCode(200);

		delete(session);
	}
	
	
}
