package utils;
import static java.util.concurrent.TimeUnit.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import iot.jcypher.graph.GrNode;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.values.JcNode;
import se.lth.cs.connect.TrustLevel;
import se.lth.cs.connect.modules.AccountSystem;
import se.lth.cs.connect.modules.Database;



public class CleanupUsers {

	

	 
    private final ScheduledExecutorService scheduler =
       Executors.newScheduledThreadPool(1);

    public void beepForAnHour() {
        final Runnable beeper = new Runnable() {
                public void run() { 
                    JcNode usr = new JcNode("u");
                	
                	JcQueryResult res = Database.query(Database.access(), new IClause[]{
                            MATCH.node(usr).label("user").property("trust").value(TrustLevel.UNREGISTERED),
                            RETURN.value(usr)
                        });
                	
                	ZonedDateTime currentTime = ZonedDateTime.now(ZoneOffset.UTC);
                	
                	for(GrNode u: res.resultOf(usr)){
                		System.out.println(u.getProperty("trust"));
                		System.out.println(u.getProperty("email").getValue().toString());
                		ZonedDateTime userTime = ZonedDateTime.parse(u.getProperty("signupdate").getValue().toString());
                		System.out.println(userTime);
                		System.out.println(currentTime);
                		long minutes = ChronoUnit.MINUTES.between(userTime, currentTime);
                		System.out.println(minutes);
                		
                		//delete if the account is older than 1 week.
                		if(minutes>60*24*7){
                			AccountSystem.deleteAccount(u.getProperty("email").getValue().toString());
                		}
                	}
            	}
            };
        scheduler.scheduleAtFixedRate(beeper, 0, 12, HOURS);
    }
}
