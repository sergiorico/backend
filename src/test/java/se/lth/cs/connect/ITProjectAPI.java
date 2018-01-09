package se.lth.cs.connect;

import org.junit.Test;

import java.util.List;
import se.lth.cs.connect.modules.Database;
import se.lth.cs.connect.modules.AccountSystem;
import iot.jcypher.query.factories.clause.NATIVE;
import iot.jcypher.query.api.IClause;

import org.junit.Before;
import static org.junit.Assert.assertTrue;

import com.jayway.restassured.filter.session.SessionFilter;

public class ITProjectAPI extends APITest {

	private SessionFilter adminSession;
	private static String adminEmail = "admin@project.test";
	private static String adminPassw = "admin-is-best";

    @Before
	public void setUp() {
        super.setUp();

        AccountSystem.createAccount(adminEmail, adminPassw, TrustLevel.ADMIN);

    	adminSession = new SessionFilter();
		given().
			filter(adminSession).
			param("email", adminEmail).
			param("passw", adminPassw).
			spec(paramReqSpec).
		expect().
			statusCode(200).
		when().
			post("/v1/account/login");
	}

    @Test
	public void testAccess() {
        SessionFilter sf = registerUser("shify-user@project.com", "1");

        // Must either be owner or admin to access these endpoints
        given().spec(paramReqSpec).filter(sf).
        	post("v1/project").
        then().statusCode(403);

		given().spec(paramReqSpec).filter(sf).
			delete("v1/project").
		then().statusCode(403);
	}

	@Test
	public void test404(){
		given().spec(paramReqSpec).
			get("v1/project/shiftyproject/taxonomy").
		then().statusCode(404);

		given().spec(paramReqSpec).
			get("v1/project/hej/taxonomy").
		then().statusCode(404);
	}

	@Test
	public void testHijack() {
		given()
			.contentType("application/json")
			.filter(adminSession)
			.body("{ \"name\":\"serp\", \"link\": \"http:\\rip\", \"project\": \"" + project + "\" }")
		.expect()
			.statusCode(400)
		.when()
			.post("/v1/project");
	}

}
