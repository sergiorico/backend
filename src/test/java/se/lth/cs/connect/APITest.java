package se.lth.cs.connect;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Random;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder ;
import com.jayway.restassured.filter.session.SessionFilter;
import com.jayway.restassured.specification.RequestSpecification;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;

import iot.jcypher.database.IDBAccess;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.CREATE;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.NATIVE;
import iot.jcypher.query.values.JcNode;
import ro.pippo.core.PippoConstants;
import ro.pippo.test.PippoRule;
import ro.pippo.test.PippoTest;
import se.lth.cs.connect.modules.Database;
import se.lth.cs.connect.modules.MailClient;
import utils.URLParser;

public class APITest extends PippoTest {
    // Adds required parameters; use only for non-json requests!
    public RequestSpecification paramReqSpec;

	public SessionFilter userSession = new SessionFilter();
	public String email = "";
	public String passw = "";

    // Each test has a different, randomised project name
    public String project = "";

    public String collection = "x";
    public long collectionId = -1;

    // This rule ensures that we have a server running before doing the tests
	public Connect app = new Connect();

	@Rule
	public PippoRule pippoRule = new PippoRule(app, 8080);

    public long submitEntry(SessionFilter user, long collectionId, String entry) {
        String id = given().
            contentType("application/json").
            filter(user).
            body(entry).
        expect().
            statusCode(200).
            contentType("application/json").
        when().
            post("v1/entry/new").
        andReturn().
            jsonPath().getString("id");
        return Long.parseLong(id);
    }
    public long submitEntry(SessionFilter user, long collectionId) {
        // Add entry. json String can be whatever as long as it is a valid json String for an entry.
        String facet = APITest.getRandomString();

        String json = "{ \"entryType\": \"challenge\", " +
            "\"description\": \"test\", " +
            "\"serpClassification\": { \"improving\": [\"" + facet + "\"] }, " +
            "\"collection\": " + collectionId + ", " +
            "\"project\": \"" + project + "\" " +
        " }";
        return submitEntry(user, collectionId, json);
    }

    public SessionFilter registerUser(SessionFilter sf, String email, String passw) {
        given().
            filter(sf).
            param("email", email).
            param("passw", passw).
            spec(paramReqSpec).
        when().
            post("/v1/account/register").
        then().
            statusCode(200);

        return sf;
    }
    public SessionFilter registerUser(String email, String passw) {
        return registerUser(new SessionFilter(), email, passw);
    }

	public String verifyUser(Mailbox mailbox) throws UnsupportedEncodingException{
        String verify = URLParser.find(mailbox.top().content);
		verify = verify.substring(verify.indexOf("token=") + 6);
		verify = URLDecoder.decode(verify, PippoConstants.UTF8);
        
        given().
            param("token", verify).
        when().
            redirects().follow(false).get("/v1/account/verify");
		return verify;
	}

	public static String getRandomString() {
        final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 10) {
            int index = (int) (rnd.nextFloat() * CHARS.length());
            salt.append(CHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }

    public void setupUser(SessionFilter sf, String email, String passw) {
        MailClient client = app.getMailClient();
		Mailbox mailbox = new Mailbox();
		app.useMailClient(mailbox);
		registerUser(sf, email, passw);
        try {
			verifyUser(mailbox);
		} catch (UnsupportedEncodingException uee) {
			org.junit.Assert.fail("Failed to verify user during setUp");
		}
		app.useMailClient(client);
    }

    public long setupCollection(SessionFilter sf, String name) {
        final String id = given().
            filter(sf).
            spec(paramReqSpec).
            param("name", name).
        expect().
            statusCode(200).
        when().
            post("/v1/collection/").
        andReturn().
            jsonPath().getString("id");
        return Long.parseLong(id);
    }

    public void setupProject(String name, String link) {
        JcNode user = new JcNode("u");
        Database.query(Database.access(), new IClause[]{
            MATCH.node(user).label("user").property("email").value(email),
            CREATE.node().label("project")
                .property("name").value(name)
                .property("link").value(link)
                .relation().out().type("CREATED_BY").node(user)
        });
    }

    private void setupConstraints() {
        Database.query(Database.access(), new IClause[]{
            NATIVE.cypher("CREATE CONSTRAINT ON (p:project) ASSERT p.name IS UNIQUE")
        });
    }

	@Before
	public void setUp() {
        app.useMailClient(new Mailbox());
        setupConstraints();

        project = getRandomString();

        paramReqSpec = new RequestSpecBuilder()

            .addParam("project", project).build();

        email = getRandomString() + "@" + getRandomString() + ".com";
        passw = getRandomString();
        setupUser(userSession, email, passw);

        setupProject(project, "http://serpconnect.cs.lth.se");

        collection = getRandomString();
        collectionId = setupCollection(userSession, collection);
	}

	@AfterClass
	public static void afterClass() {
		IDBAccess access = Database.access();
		access.clearDatabase();
		access.close();
	}

}