package se.lth.cs.connect;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.filter.session.SessionFilter;

import ro.pippo.core.PippoConstants;
import se.lth.cs.connect.Mailbox;
import se.lth.cs.connect.modules.MailClient;

public class ITUserAPI extends APITest {

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

	/**
	 * Verify that the /login route returns 200 when logged in.
	 */
	@Test
	public void testLogin() {
		given().
			filter(userSession).
		when().
			get("/v1/account/login").
		then().
			statusCode(200);
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
		
		registerUser("test-reg@a.b", "1234");
	
		assertThat("Must send registration email", mailbox.getInbox().size(), is(1));
		assertThat("Recipient must match registered email",
				mailbox.top().recipient, is(equalTo("test-reg@a.b")));
		
		String verify = verifyUser(mailbox);
		assertThat("Must find link in email", verify, not(equalTo("")));

		expect().
			statusCode(400).
		given().
			param("email", "test-reg@a.b").
			param("passw", "1").
		when().
			post("/v1/account/register");
	}

	@Test
	public void testLifecycle() {
		given().
			filter(userSession).
		expect().
			statusCode(200).
		when().
			post("/v1/account/logout");

		given().
			filter(userSession).
			param("email", email).
			param("passw", passw).
		expect().
			statusCode(200).
		when().
			post("/v1/account/login");

		given().
			filter(userSession).
		when().
			post("/v1/account/delete").
		then().
			statusCode(200);
	}
	
	// This testcase might break if format of the reset password email is changed
	@Test
	public void testResetPassword() throws UnsupportedEncodingException{
		Mailbox mailbox = new Mailbox();
		app.useMailClient(mailbox);
				
		//logout
		given().
			filter(userSession).
		when().
			post("/v1/account/logout").
		then().
			statusCode(200);
		
		//ask for reset email
		given().
			param("email", email).
		expect().
			statusCode(200).
		when().
			post("/v1/account/reset-password");
		
		//filter mail for the token
		Mailbox.Mail test = mailbox.top();
		String[] split = test.content.split("token=");
		String[] token = split[1].split("\n");
		String verify = URLDecoder.decode(token[0], PippoConstants.UTF8);

		//verify token
		//stop redirect to avoid breaking the session
		String sessionId = given().
				redirects().follow(false).
				param("token", verify).
			expect().
				statusCode(302).
			when().
				get("/v1/account/reset-password").
			andReturn().
				sessionId();

		//set new password
		given().
			cookie("JSESSIONID", sessionId).
			param("passw", "hej123").
		expect().
			statusCode(200).
		when().
			post("/v1/account/reset-password-confirm");
	
		//session gets wrecked by redirect no need to logout

		//login with new password
		given().
			filter(userSession).
			param("email", email).
			param("passw", "hej123").
		when().
			post("/v1/account/login").
		then().
			statusCode(200);
	}
	
	
}
