package se.lth.cs.connect.routes;

import java.util.List;

// required for building queries and interpreting query results
import iot.jcypher.graph.GrNode;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.factories.clause.WHERE;
import iot.jcypher.query.factories.xpression.X;
import iot.jcypher.query.values.JcNode;
import ro.pippo.core.PippoSettings;
import se.lth.cs.connect.Connect;
import se.lth.cs.connect.Graph;
import se.lth.cs.connect.RequestException;
import se.lth.cs.connect.TrustLevel;
import se.lth.cs.connect.modules.AccountSystem;
import se.lth.cs.connect.modules.Database;

/**
 * Handles some admin-only routes, like trust modification.
 */
public class Admin extends BackendRouter {
    public Admin(Connect app) {
        super(app);
    }

    protected void setup(PippoSettings conf) {
        // Require login and admin status on all routes
        ALL(".*", (rc) -> {
            String email = rc.getSession("email");
            if (email == null)
                throw new RequestException(401, "Not logged in.");

            AccountSystem.Account acc = AccountSystem.findByEmail(email);
            if (!TrustLevel.authorize(acc.trust, TrustLevel.ADMIN))
                throw new RequestException(403, "Only admins may proceed.");

            rc.setLocal("acc", acc);
            rc.next();
        });

        // GET api.serp.se/v1/admin HTTP1/1
        GET("", (rc) -> {
            rc.status(200).text().send("You have admin access.");
        });

        // GET api.serp.se/v1/admin/pending --> [entry, entry, ..., entry]
        GET("/pending", (rc) -> {
            final JcNode e = new JcNode("e");
            final JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(e).label("entry").property("pending").value(true),
                RETURN.value(e)
            });
            final List<GrNode> pending = res.resultOf(e);
            rc.json().send(Graph.Node.fromList(pending));
        });

        // POST api.serp.se/v1/admin/accept-entry entry=id--> [entry, entry, ..., entry]
        POST("/accept-entry", (rc) -> {
            if (rc.getParameter("entry").isEmpty())
                throw new RequestException("Must provide entry parameter");

            int entry = rc.getParameter("entry").toInt();

            final JcNode e = new JcNode("e");
            final JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(e).label("entry"),
                WHERE.valueOf(e.id()).EQUALS(entry),
                DO.REMOVE(e.property("pending"))
            });
            rc.getResponse().ok();
        });

        POST("/reject-entry", (rc) -> {
            if (rc.getParameter("entry").isEmpty())
                throw new RequestException("Must provide entry parameter");

            int entry = rc.getParameter("entry").toInt();

            final JcNode e = new JcNode("e");
            final JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(e).label("entry"),
                WHERE.valueOf(e.id()).EQUALS(entry),
                DO.DETACH_DELETE(e)
            });
            rc.getResponse().ok();
        });
        
        
        POST("/delete-collection", (rc) -> {
            if (rc.getParameter("id").isEmpty())
                throw new RequestException("Must provide entry parameter");
            
            int id = rc.getParameter("id").toInt();
            final JcNode c = new JcNode("c");
            Database.query(rc.getLocal("db"), new IClause[]{
            	MATCH.node(c).label("collection"),
            	WHERE.valueOf(c.id()).EQUALS(id),
            	DO.DETACH_DELETE(c)
            });
            rc.getResponse().ok();
        });

        // PUT api.serp.se/v1/admin/set-trust HTTP/1.1
        // email=...&trust=...
        PUT("/set-trust", (rc) -> {
            String email = rc.getParameter("email").toString();
            String trust = rc.getParameter("trust").toString();
            int level = TrustLevel.fromString(trust);
            
            if (level == TrustLevel.UNKNOWN){
            	throw new RequestException("Invalid trust level.");
            }
            
            if (email == null)
                throw new RequestException("Invalid email.");

            AccountSystem.changeTrust(email, level);
            rc.status(200).text().send("Success");
        });

        GET("/users", (rc) -> {
            final JcNode u = new JcNode("u");
            final JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(u).label("user"),
                RETURN.value(u)
            });
            final List<GrNode> everyone = res.resultOf(u);
            rc.json().send(Graph.User.fromList(everyone));
        });
        
        GET("/collections", (rc) -> {
        	final JcNode c = new JcNode("c");
        	final String email = rc.getSession("email");
        	final JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
        		MATCH.node(c).label("collection"),
        		WHERE.NOT().existsPattern(
                        X.node().label("user").property("email").value(email)
                        .relation().type("MEMBER_OF")
                        .node(c)),
        		RETURN.value(c)
        	});
        	
        	final List<GrNode> allColls = res.resultOf(c);
        	rc.json().send(Graph.Collection.fromList(allColls));
        });
        
        GET("/{id}/is-owner", (rc)->{
   		 rc.status(200).json().send(Collection.isOwner(rc));
       });

    }

    public String getPrefix() { return "/v1/admin"; }
}