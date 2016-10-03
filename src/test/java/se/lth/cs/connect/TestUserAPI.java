package se.lth.cs.connect;

import org.junit.Rule;
import org.junit.Test;

import com.jayway.restassured.response.Response;

import ro.pippo.test.PippoRule;
import ro.pippo.test.PippoTest;

public class TestUserAPI extends PippoTest {

	@Rule
	public PippoRule pippoRule = new PippoRule(new Connect());
	
	@Test
	public void testAccessDenied() {
		// These routes require an active session => they should fail (with status 401)
		when().get("/v1/account/login").then().statusCode(401);
		when().get("/v1/account/collections").then().statusCode(401);
		when().get("/v1/account/self").then().statusCode(401);
		when().post("/v1/account/logout").then().statusCode(401);
		when().post("/v1/account/delete").then().statusCode(401);
		when().post("/v1/account/change-password").then().statusCode(401);
		when().get("/v1/account/invites").then().statusCode(401);
		when().get("/v1/account/dat12asm@student.lu.se").then().statusCode(401);
	}
	
	// Register --> Logout --> Login --> Delete
	@Test
	public void testLifecycle() {
		Response reg = given().
				param("email", "test@serptest.test").
				param("passw", "hejsanhoppsan").
			post("/v1/account/register");
		
		reg.then().
			statusCode(200);
		
		given().
			cookie("JSESSIONID", reg.getCookie("JSESSIONID")).
		when().
			post("/v1/account/logout").
		then().
			statusCode(200);
		
		Response login = given().
				param("email", "test@serptest.test").
				param("passw", "hejsanhoppsan").
			post("/v1/account/login");
		
		reg.then().
			statusCode(200);
		
		given().
			cookie("JSESSIONID", login.getCookie("JSESSIONID")).
		when().
			post("/v1/account/delete").
		then().
			statusCode(200);
	}
}
