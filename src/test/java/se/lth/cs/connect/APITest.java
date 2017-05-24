package se.lth.cs.connect;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Random;

import org.junit.After;


import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;

import com.jayway.restassured.filter.session.SessionFilter;

import iot.jcypher.database.IDBAccess;
import ro.pippo.core.PippoConstants;
import ro.pippo.test.PippoRule;
import ro.pippo.test.PippoTest;
import se.lth.cs.connect.modules.Database;
import se.lth.cs.connect.modules.MailClient;
import utils.URLParser;

public class APITest extends PippoTest {
	public SessionFilter userSession = new SessionFilter();
	public String email = "";
	public String passw = "";

    public String collection = "x";
    public long collectionId = -1;
	
    // This rule ensures that we have a server running before doing the tests
	public Connect app = new Connect();

	@Rule
	public PippoRule pippoRule = new PippoRule(app);

    public SessionFilter registerUser(SessionFilter sf, String email, String passw) {
        given().
            filter(sf).
            param("email", email).
            param("passw", passw).
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
		expect().
			statusCode(200).
		when().
			get("/v1/account/verify");
		
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
            param("name", name).
        expect().
            statusCode(200).
        when().
            post("/v1/collection/").
        andReturn().
            jsonPath().getString("id");
        return Long.parseLong(id);
    }

	@Before
	public void setUp() {
        email = getRandomString() + "@" + getRandomString() + ".com";
		passw = getRandomString();
		setupUser(userSession, email, passw);
		
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