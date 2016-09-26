package se.lth.cs.connect;

import org.junit.Rule;
import org.junit.Test;

import ro.pippo.test.PippoRule;
import ro.pippo.test.PippoTest;

public class TestWebFramework extends PippoTest {
	
	@Rule
	public PippoRule pippoRule = new PippoRule(new Connect());
	
	@Test
    public void testIndex() {
		// Server responds with 404 because we have no default index page
        when().
            get("/").
        then()
            .statusCode(404)
            .headers("Content-Type", "HTML");
    }

}
