package se.lth.cs.connect;

import com.jayway.restassured.filter.session.SessionFilter;

import org.junit.Before;
import org.junit.Test;

import se.lth.cs.connect.modules.AccountSystem;
import se.lth.cs.connect.modules.TaxonomyDB;

public class ITProjectAPI extends APITest {

	private SessionFilter adminSession;

    @Before
	public void setUp() {
        super.setUp();

    	adminSession = new SessionFilter();
		given().
			filter(adminSession).
			param("email", adminEmail).
			param("passw", adminPassw).
		expect().
			statusCode(200).
		when().
			post("/v1/account/login");
	}

    @Test
	public void testAccess() {
		given().
			filter(adminSession).
			param("name", "xyztest").
			param("link", "link").
		expect().statusCode(200).when().post("v1/project");

		// Anyone should be able to access information & taxonomy
        get("v1/project/" + project).then().statusCode(200);
        get("v1/project/" + project + "/taxonomy").then().statusCode(200);

        get("v1/project/xyztest").then().statusCode(200);
        get("v1/project/xyztest/taxonomy").then().statusCode(200);

        // Must either be owner or admin to access these endpoints
        given().filter(userSession).
        	post("v1/project").
        then().statusCode(403);

        post("v1/project/" + project + "/delete").then().statusCode(401);
        post("v1/project/xyztest/delete").then().statusCode(401);

		given().filter(userSession).expect().statusCode(403).when().
			put("v1/project/xyztest/taxonomy");

		given().filter(userSession).expect().statusCode(403).when().
			post("v1/project/xyztest/delete");
	}

	@Test
	public void testFoundNotFound(){
		expect().statusCode(200).when().get("v1/project/" + project + "/taxonomy");
		expect().statusCode(404).when().get("v1/project/shiftyproject/taxonomy");
		expect().statusCode(404).when().get("v1/project/hej/taxonomy");
	}

	@Test
	public void testTaxonomy() {
		final String url = "v1/project/" + project + "/taxonomy";

		int version = get(url).andReturn().jsonPath().getInt("version");

		TaxonomyDB.Taxonomy testTaxonomy = new TaxonomyDB.Taxonomy();
		testTaxonomy.version = version + 1;

		given().
			filter(adminSession).
			contentType("application/json").
			body(testTaxonomy).
		expect().statusCode(200).when().put(url);
	}

	@Test
	public void testRename() {
		given().
			filter(adminSession).
			param("name", "name").
			param("link", "link").
		expect().statusCode(200).when().post("v1/project");

		given().
			filter(adminSession).
			param("name", "newname").
		expect().
			statusCode(200).
		when().put("/v1/project/name");

		get("/v1/project/name").then().statusCode(404);
		get("/v1/project/newname").then().statusCode(200);

		// Cannot hijack by renaming
		given().
			filter(adminSession).
			param("name", project).
		expect().
			statusCode(400).
		when().put("/v1/project/newname");
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
