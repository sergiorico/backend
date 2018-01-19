package se.lth.cs.connect;

import com.jayway.restassured.filter.session.SessionFilter;

import org.junit.Before;
import org.junit.Test;

import se.lth.cs.connect.events.DetachEntryEvent;
import se.lth.cs.connect.events.LeaveCollectionEvent;


public class ITEvents extends APITest {
    private String basePath;

    @Before
	public void setUp() {
        super.setUp();
        basePath = "/v1/collection/" + collectionId;
    }

    @Test
    public void entryWithoutCollectionShouldDie() {
        String entryJson =  "{ \"entryType\": \"challenge\", " +
            "\"description\": \"test\", " +
            "\"serpClassification\": { }, " +
            "\"project\": \"" + project + "\", " +
            "\"collection\": " + collectionId +
        " }";
		long entryId = submitEntry(userSession, collectionId, entryJson);

        new DetachEntryEvent(collectionId, entryId).execute();;

        expect().statusCode(400).when().get("/v1/entry/" + entryId);
    }

    @Test
    public void testCollectionNuke() {
		long entryID = submitEntry(userSession, collectionId);

        String user2 = APITest.getRandomString();
        SessionFilter sf2 = new SessionFilter();
        setupUser(sf2, user2, "3");

        app.useMailClient(new Mailbox());
        given().
            filter(userSession).
            param("email", user2).
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