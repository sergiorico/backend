package utils;
import static java.util.concurrent.TimeUnit.*;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import iot.jcypher.database.IDBAccess;
import iot.jcypher.graph.GrNode;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcNumber;
import se.lth.cs.connect.Connect;
import se.lth.cs.connect.TrustLevel;
import se.lth.cs.connect.modules.AccountSystem;
import se.lth.cs.connect.modules.Database;
import se.lth.cs.connect.routes.Collection;



public class CleanupUsers {
	private Connect app;
	
	public CleanupUsers(Connect app){
		this.app = app;
	}
	 
    private final ScheduledExecutorService scheduler =
       Executors.newScheduledThreadPool(1);

    public void everyTwelveHours() {
        final Runnable cleaner = new Runnable() {
			public void run() { 
				IDBAccess db = Database.access();
				
				JcNode usr = new JcNode("u");
				JcQueryResult res = Database.query(db, new IClause[]{
					MATCH.node(usr).label("user").property("trust").value(TrustLevel.UNREGISTERED),
					RETURN.value(usr)
				});
				
				ZonedDateTime currentTime = ZonedDateTime.now(ZoneOffset.UTC);
				
				for (GrNode u: res.resultOf(usr)) {
					//get time difference
					String email = u.getProperty("email").getValue().toString();
					ZonedDateTime userTime = ZonedDateTime.parse(u.getProperty("signupdate").getValue().toString());
					long minutes = ChronoUnit.MINUTES.between(userTime, currentTime);
					
					//delete if the account is older than 1 week.
					if (minutes > 60*24*7) {
						JcNode user = new JcNode("usr");
						JcNode coll = new JcNode("coll");
						JcNumber id = new JcNumber("id");
						JcQueryResult res2 = Database.query(db, new IClause[]{
							MATCH.node(user).label("user").property("email").value(email)
								.relation().type("INVITE")
								.node(coll).label("collection"),
							RETURN.value(coll.id()).AS(id)
						});

						//inform all persons who invited the user that he rejected the invitation
						for (BigDecimal c: res2.resultOf(id)) {
							Collection.handleInvitation(db, email, c.intValue(), "rejected", app);
						}

						AccountSystem.deleteAccount(email,db);
					}
				}

				db.close();
			}
		};
        scheduler.scheduleAtFixedRate(cleaner, 0, 12, HOURS);
    }
}
