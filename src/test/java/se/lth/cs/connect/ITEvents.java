package se.lth.cs.connect;

import org.junit.Test;

import java.util.List;


import org.junit.Before;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonParseException;
import com.jayway.restassured.filter.session.SessionFilter;

import se.lth.cs.connect.events.DeleteAccountEvent;
import se.lth.cs.connect.events.LeaveCollectionEvent;



import utils.URLParser;


public class ITEvents extends APITest {
    private String basePath;

    @Before
	public void setUp() {
        super.setUp();
        basePath = "/v1/collection/" + collectionId;
	}

    @Test
    public void testCollectionNuke() {
		long entryID = ITCollectionAPI.submitEntry(userSession, collectionId);

        String user2 = APITest.getRandomString();
        SessionFilter sf2 = new SessionFilter();
        setupUser(sf2, user2, "3");

        app.useMailClient(new Mailbox());
        given().
            filter(userSession).
            param("email", new String[]{user2}).
        when().
            post(basePath + "/invite").
        then().
            statusCode(200);

        given().
            filter(sf2).
        expect().
            statusCode(200).
        when().
            post(basePath + "/accept");

        new LeaveCollectionEvent(collectionId, email).execute();

        expect().statusCode(400).when().get(basePath + "/stats");
        expect().statusCode(400).when().get("/v1/entry/" + entryID);
    }

}