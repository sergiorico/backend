package se.lth.cs.connect.routes;

import se.lth.cs.connect.Graph;
import se.lth.cs.connect.TrustLevel;
import se.lth.cs.connect.RequestException;

import se.lth.cs.connect.modules.Database;
import se.lth.cs.connect.modules.Mailman;

import java.util.List;

import ro.pippo.core.Application;
import ro.pippo.core.Messages;
import ro.pippo.core.PippoSettings;
import ro.pippo.core.route.RouteContext;
import iot.jcypher.graph.GrNode;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcRelation;
import iot.jcypher.query.values.JcBoolean;
import iot.jcypher.query.values.JcNumber;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.WHERE;
import iot.jcypher.query.factories.clause.MERGE;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.factories.clause.NATIVE;
import iot.jcypher.query.factories.clause.CREATE;
import iot.jcypher.query.factories.clause.OPTIONAL_MATCH;
import iot.jcypher.query.factories.xpression.X;

/**
 * Handles account related actions.
 */
public class Collection extends BackendRouter {
    public String getPrefix() { return "/v1/collection"; }

    private String collectionInviteTemplate;
    private String frontend;

    public Collection(Application app) {
        super(app);

        Messages msg = app.getMessages();
        collectionInviteTemplate = msg.get("pippo.collectioninvite", "en");

        frontend = app.getPippoSettings().getString("frontend", "http://localhost:8181");
    }

    protected void setup(PippoSettings conf) {

        // POST api.serpconnect.cs.lth.se/v1/collection HTTP/1.1
        // name=blabla
        POST("/", (rc) -> {
            String email = rc.getSession("email");
            if (email == null)
                throw new RequestException(401, "Must be logged in.");

            String name = rc.getParameter("name").toString();
            if (name == null || name.isEmpty())
                throw new RequestException("No name parameter");

            JcNode coll = new JcNode("c");
            JcNode usr = new JcNode("u");
            JcNumber id = new JcNumber("x");

            JcQueryResult res = Database.query(Database.access(), new IClause[]{
                MATCH.node(usr).label("user").property("email").value(email),
                CREATE.node(usr).relation().type("MEMBER_OF").out()
                    .node(coll).label("collection")
                    .property("name").value(name),
                RETURN.value(coll.id()).AS(id)
            });

            rc.json().send(res.resultOf(id).get(0));
        });

        //
        GET("/{id}/graph", (rc) -> {
            int id = rc.getParameter("id").toInt();

            JcNode coll = new JcNode("coll");
            JcNode node = new JcNode("entry");
            JcRelation rel = new JcRelation("rel");

            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(coll).label("collection")
                    .relation().type("CONTAINS")
                    .node(node).label("entry"),
                WHERE.valueOf(coll.id()).EQUALS(id),
                OPTIONAL_MATCH.node(node).relation(rel).out().node().label("facet"),
                RETURN.value(node),
                RETURN.value(rel)
            });

            rc.json().send(new Graph(res.resultOf(node), res.resultOf(rel)));
        });

        // GET api.serpconnect.cs.lth.se/{id}/stats HTTP/1.1
        // --> { members: int, entries: int }
        GET("/{id}/stats", (rc) -> {
            int id = rc.getParameter("id").toInt();

            JcNode coll = new JcNode("coll");
            JcRelation u = new JcRelation("u");
            JcRelation e = new JcRelation("e");

            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node().label("user")
                    .relation(u).type("MEMBER_OF")
                    .node(coll).label("collection")
                    .relation(e).type("CONTAINS")
                    .node().label("entry"),
                WHERE.valueOf(coll.id()).EQUALS(id),
                NATIVE.cypher("RETURN COUNT(DISTINCT u) AS users, COUNT(DISTINCT e) AS entries")
            });

            final java.math.BigDecimal users = res.resultOf(new JcNumber("users")).get(0);
            final java.math.BigDecimal entries = res.resultOf(new JcNumber("entries")).get(0);

            class RetVal {
                public int members, entries;
                public RetVal(int mem, int ent) {
                    members = mem;
                    entries = ent;
                }
            }

            rc.json().send(new RetVal(users.intValue(), entries.intValue()));
        });

        // GET api.serpconnect.cs.lth.se/{id}/entries HTTP/1.1
        // --> [entries in collection]
        GET("/{id}/entries", (rc) -> {
            int id = rc.getParameter("id").toInt();

            final JcNode entry = new JcNode("e");
            final JcNode coll = new JcNode("c");
            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(coll).label("collection")
                    .relation().type("CONTAINS")
                    .node(entry).label("entry"),
                WHERE.valueOf(coll.id()).EQUALS(id),
                RETURN.value(entry)
            });

            final List<GrNode> entries = res.resultOf(entry);

            rc.status(200).json().send(Graph.Node.fromList(entries));
        });

        // Must be logged in to accept
        ALL("/{id}/.*", (rc) -> {
            if (rc.getParameter("id").isEmpty())
                throw new RequestException("Invalid collection id");

            if (rc.getSession("email") == null)
                throw new RequestException(401, "Must be logged in.");

            rc.next();
        });

        // POST api.serpconnect.cs.lth.se/{id}/accept HTTP/1.1
        POST("/{id}/accept", (rc) -> {
            String email = rc.getSession("email");
            int id = rc.getParameter("id").toInt();

            JcNode user = new JcNode("user");
            JcNode coll = new JcNode("coll");
            JcRelation rel = new JcRelation("rel");

            // Replace an INVITE type relation with a MEMBER_OF relation
            JcQueryResult res = Database.query(Database.access(), new IClause[]{
                MATCH.node(user).label("user").property("email").value(email)
                    .relation(rel).type("INVITE")
                    .node(coll).label("collection"),
                WHERE.valueOf(coll.id()).EQUALS(id),
                MERGE.node(user).relation().out().type("MEMBER_OF").node(coll),
                DO.DELETE(rel),
                NATIVE.cypher("RETURN TRUE AS ok")
            });

            if (res.resultOf(new JcBoolean("ok")).size() > 0)
                rc.getResponse().ok();
            else
                throw new RequestException("Not invited to that collection.");
        });

        // Must be logged in AND member of collection to proceed
        ALL("/{id}/.*", (rc) -> {
            String email = rc.getSession("email");
            int id = rc.getParameter("id").toInt();

            JcNode coll = new JcNode("coll");

            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node().label("user").property("email").value(email)
                    .relation().out().type("MEMBER_OF")
                    .node(coll).label("collection"),
                WHERE.valueOf(coll.id()).EQUALS(id),
                NATIVE.cypher("RETURN TRUE AS ok")
            });

            if (res.resultOf(new JcBoolean("ok")).size() == 0)
                throw new RequestException(403, "You are not a member of that collection");

            rc.next();
        });

        // POST api.serpconnect.cs.lth.se/{id}/invite HTTP/1.1
        // email[0]=...&email[1]=
        POST("/{id}/invite", (rc) -> {
            int id = rc.getParameter("id").toInt();
            List<String> emails = rc.getParameter("email").toList(String.class);

            for (String email : emails) {
                JcNode user = new JcNode("user");
                JcNode coll = new JcNode("coll");

                // Use MERGE so we don't end up with multiple invites per user
                JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                    MATCH.node(user).label("user").property("email").value(email),
                    MATCH.node(coll).label("collection"),
                    WHERE.valueOf(coll.id()).EQUALS(id),
                    MERGE.node(user).relation().out().type("INVITE").node(coll)
                });

                Mailman.sendEmail(email, "SERP Connect - Collection Invite",
                    collectionInviteTemplate.replace("{frontend}", frontend));
            }

            rc.getResponse().ok();
        });

        // POST api.serpconnect.cs.lth.se/{id}/leave HTTP/1.1
        POST("/{id}/leave", (rc) -> {
            String email = rc.getSession("email");
            int id = rc.getParameter("id").toInt();

            JcNode user = new JcNode("user");
            JcNode coll = new JcNode("coll");
            JcRelation rel = new JcRelation("connection");

            Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(user).label("user").property("email").value(email)
                    .relation(rel)
                    .node(coll).label("collection"),
                WHERE.valueOf(coll.id()).EQUALS(id),
                DO.DELETE(rel)
            });
            
            //Delete collections that have no members (or invites)
            Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(coll).label("collection"),
                WHERE.valueOf(coll.id()).EQUALS(id),
                WHERE.NOT().existsPattern(
                        X.node().label("user")
                        .relation()
                        .node(coll)),
                DO.DETACH_DELETE(coll)
            });
            
            removeEntriesWithNoCollection(rc);

            rc.getResponse().ok();
        });

        // POST api.serpconnect.cs.lth.se/{id}/kick HTTP/1.1
        // email=...
        POST("/{id}/kick", (rc) -> {
            rc.status(500).text().send("Not yet implemented");
        });

        // POST api.serpconnect.cs.lth.se/{id}/removeEntry HTTP/1.1
        // entryId=...
        POST("/{id}/removeEntry", (rc) -> {
            int id = rc.getParameter("id").toInt();
            int entryId = rc.getParameter("entryId").toInt();

            final JcNode entry = new JcNode("e");
            final JcNode coll = new JcNode("c");
            final JcRelation rel = new JcRelation("r");

            // First, remove the relation to the current collection
            Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(coll).label("collection")
                    .relation(rel).type("CONTAINS")
                    .node(entry).label("entry"),
                WHERE.valueOf(coll.id()).EQUALS(id).AND().valueOf(entry.id()).EQUALS(entryId),
                DO.DELETE(rel)
            });
            
            //TODO: Optimize: Only remove the entry that was supposed to be delted.
            
            removeEntriesWithNoCollection(rc);

            rc.getResponse().ok();
        });

        // POST api.serpconnect.cs.lth.se/{id}/kick HTTP/1.1
        // entryId=...
        POST("/{id}/addEntry", (rc) -> {
            int id = rc.getParameter("id").toInt();
            int entryId = rc.getParameter("entryId").toInt();

            final JcNode entry = new JcNode("e");
            final JcNode coll = new JcNode("c");

            // Connect entry and collection
            Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(coll).label("collection"),
                WHERE.valueOf(coll.id()).EQUALS(id),
                MATCH.node(entry).label("entry"),
                WHERE.valueOf(entry.id()).EQUALS(entryId),
                MERGE.node(coll).relation().out().type("CONTAINS").node(entry)
            });

            rc.getResponse().ok();
        });

        // GET api.serpconnect.cs.lth.se/{id}/members HTTP/1.1
        // --> [members in collection]
        GET("/{id}/members", (rc) -> {
            int id = rc.getParameter("id").toInt();

            final JcNode user = new JcNode("u");
            final JcNode coll = new JcNode("c");
            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(coll).label("collection")
                    .relation().in().type("MEMBER_OF")
                    .node(user).label("user"),
                WHERE.valueOf(coll.id()).EQUALS(id),
                RETURN.value(user)
            });

            Graph.User[] members = Graph.User.fromList(res.resultOf(user));
            rc.status(200).json().send(members);
        });

    }
    
    private void removeEntriesWithNoCollection(RouteContext rc){
    	final JcNode entry = new JcNode("e");
    	Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(entry).label("entry"),
                WHERE.NOT().existsPattern(
                        X.node().label("collection")
                        .relation().type("CONTAINS")
                        .node(entry)),
                DO.DETACH_DELETE(entry)
            });
    }
}