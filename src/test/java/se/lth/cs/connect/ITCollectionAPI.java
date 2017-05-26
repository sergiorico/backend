package se.lth.cs.connect;

import org.junit.Test;

import java.util.List;


import org.junit.Before;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonParseException;
import com.jayway.restassured.filter.session.SessionFilter;

import utils.URLParser;


public class ITCollectionAPI extends APITest {
	public String basePath;

    @Before
	public void setUp() {
        super.setUp();
		basePath = "/v1/collection/" + collectionId;
	}

    public static long submitEntry(SessionFilter user, long collectionId, String entry) {
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
    public static long submitEntry(SessionFilter user, long collectionId) {
        // Add entry. json String can be whatever as long as it is a valid json String for an entry.
        String facet = APITest.getRandomString();

	    String json = "{ \"entryType\": \"challenge\", " + 
            "\"description\": \"test\", " +
            "\"serpClassification\": { \"improving\": [\"" + facet + "\"] }, " +
            "\"collection\": " + collectionId + 
        " }";
        return submitEntry(user, collectionId, json);
    }


    @Test
	public void testAccessDenied() {
        // Must be logged in (+member) the access these endpoints
		get(basePath + "/invite").then().statusCode(401);
		get(basePath + "/leave").then().statusCode(401);
		get(basePath + "/removeEntry").then().statusCode(401);
		post(basePath + "/addEntry").then().statusCode(401);
		post(basePath + "/members").then().statusCode(401);
		
        // Must be a member of the collection to access these endpoints
        SessionFilter sf = registerUser("a.b@c.d", "1");
		given().filter(sf).get(basePath + "/leave").then().statusCode(403);
		given().filter(sf).post(basePath + "/members").then().statusCode(403);
		
        // Must be an owner of the colleciton to access these endpoints
        given().filter(sf).get(basePath + "/invite").then().statusCode(403);
		given().filter(sf).get(basePath + "/removeEntry").then().statusCode(403);
        given().filter(sf).post(basePath + "/addEntry").then().statusCode(403);
	}

	/**
	 * Tests that the collection check filter does not accept a string an id and that the id exist in the database.
	 */
	@Test
	public void testCollectionIdCheckExistanceFilter(){
		expect().
			statusCode(400).
		when().
			get("v1/collection/" + 19453324 + "/stats"); 
		
		expect().
			statusCode(400).
		when().
			get("v1/collection/" + "hej" + "/stats");
	}

    /**
     * When the owner leaves the collection, delete everything.
     */
    @Test
    public void testCollectionNuke() {
		long entryID = submitEntry(userSession, collectionId);

        String user2 = getRandomString();
        SessionFilter sf2 = new SessionFilter();
        setupUser(sf2, user2, "1");

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

        int friends = given().
                filter(sf2).
                param("email", user2).
            expect().
                statusCode(200).
                contentType("application/json").
            when().
                get("/v1/account/friends").
            andReturn().body().as(List.class).size();
        assertTrue("Should have 1 friend", friends > 0);
        
        
        given().
            filter(userSession).
        expect().
            statusCode(200).
        when().
            post(basePath + "/leave");

        expect().statusCode(400).when().get(basePath + "/stats");
        expect().statusCode(400).when().get("/v1/entry/" + entryID);
    }
	
	/**
	 * Tests that when the last person of a collections leaves the collection, the collection is deleted.
	 * Also tests that entries that are part of that collection are also deleted.
	 * @throws JsonParseException 
	 * @throws UnsupportedEncodingException 
	 */
	@Test
	public void testDeleteEmptyCollections() throws JsonParseException {       
		long entryID = submitEntry(userSession, collectionId);
		
        // Last user leaves collection
		given().
			filter(userSession).
		expect().
			statusCode(200).
		when().
			post(basePath + "/leave");
		
        // Verify that collection was deleted
		expect().
			statusCode(400).
		when().
			get(basePath + "/stats");
		
		// Test that entry was also automatically deleted.
		expect().
			statusCode(400).
		when().
			get("v1/entry/" + entryID);
	}
	
	/**
	 * Tests that a new collection is not created when the collection id is a string when making a new entry.
	 * @throws UnsupportedEncodingException 
	 */
	@Test
	public void testNoCollectionCreationOnNewEntryWhenCollectionIdIsString() {
		//Create entry. collection has a string value instead of an integer to test that it doesn't create a new collection.
		String json = "{ \"entryType\": \"challenge\", \"description\": \"test\", \"serpClassification\": {}, \"collection\": \"hejj\" }";
		given().
			contentType("application/json").
			filter(userSession).body(json).
		expect().
			statusCode(400).
		when().
			post("v1/entry/new");
	}

}
