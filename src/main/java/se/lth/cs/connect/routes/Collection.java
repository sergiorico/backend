package se.lth.cs.connect.routes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import iot.jcypher.database.IDBAccess;
import iot.jcypher.graph.GrNode;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.CREATE;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.MERGE;
import iot.jcypher.query.factories.clause.NATIVE;
import iot.jcypher.query.factories.clause.OPTIONAL_MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.factories.clause.WHERE;
import iot.jcypher.query.values.JcBoolean;
import iot.jcypher.query.values.JcCollection;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcNumber;
import iot.jcypher.query.values.JcRelation;
import iot.jcypher.query.values.JcString;
import ro.pippo.core.Messages;
import ro.pippo.core.PippoSettings;
import ro.pippo.core.route.RouteContext;
import se.lth.cs.connect.Connect;
import se.lth.cs.connect.Graph;
import se.lth.cs.connect.RequestException;
import se.lth.cs.connect.TrustLevel;
import se.lth.cs.connect.events.DeleteEntryEvent;
import se.lth.cs.connect.events.LeaveCollectionEvent;
import se.lth.cs.connect.modules.AccountSystem;
import se.lth.cs.connect.modules.Database;
import se.lth.cs.connect.modules.TaxonomyDB;
import se.lth.cs.connect.modules.TaxonomyDB.Facet;
import se.lth.cs.connect.routes.Entry.TaxonomyFacet;

/**
 * Handles account related actions.
 */
public class Collection extends BackendRouter {
    @Override
	public String getPrefix() { return "/v1/collection"; }

    private String inviteTemplate;
    private String inviteNewUserTemplate;
	private static String inviteActionTemplate;
	private String frontend;

    public Collection(Connect app) {
        super(app);

        Messages msg = app.getMessages();
        inviteTemplate = msg.get("pippo.collectioninvite", "en");
        inviteNewUserTemplate = msg.get("pippo.collectioninvitenewuser", "en");
		inviteActionTemplate = msg.get("pippo.collectioninviteaction", "en");
		
        frontend = app.getPippoSettings().getString("frontend", "http://localhost:8181");
    }
    
    /** 
     * JSON request body for /collection/{id}/reclassify
     */
    static class ReclassifyRequest 
    {
    	@JsonProperty("oldFacetId")
    	public String oldFacetId;
    	
    	@JsonProperty("newFacetId")
    	public String newFacetId;
    	
    	@JsonProperty("entities")
    	public List<Long> entities;
	}
     
    @Override
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

            final JcNode usr = new JcNode("u");
            final JcNumber id = new JcNumber("x");
            final JcNode coll = new JcNode("c");

            //usr-(member_of)->coll-(owner)->user
            JcQueryResult res = Database.query(Database.access(), new IClause[]{
                MATCH.node(usr).label("user").property("email").value(email),
                CREATE.node(usr).relation().type("MEMBER_OF").out()
                    .node(coll).label("collection")
                    .property("name").value(name)
                    .relation().type("OWNER").out().node(usr),
                RETURN.value(coll.id()).AS(id)
            });
            
            int collectionId = res.resultOf(id).get(0).intValue();
            try {
				TaxonomyDB.update(collectionId, new TaxonomyDB.Taxonomy());
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
            
            rc.json().send(
            	"{ \"id\": " + collectionId + " }");
            
        });

        //id must be a string and the id must exist in the database.
        ALL("/{id}/.*", (rc) -> {

        	String ids = rc.getParameter("id").toString();
        	if (!StringUtils.isNumeric(ids)){
        		throw new RequestException("Invalid id");
        	}
        	int id = Integer.parseInt(ids);
        	JcNode coll = new JcNode("coll");

        	JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                    MATCH.node(coll).label("collection"),
                    WHERE.valueOf(coll.id()).EQUALS(id),
                    NATIVE.cypher("RETURN true AS ok")
            });

        	if (res.resultOf(new JcBoolean("ok")).size() == 0)
                throw new RequestException("id does not exist in database");
            
            rc.next();
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
        
        // { version: number, taxonomy: [...] } 
        GET("/{id}/taxonomy", (rc) -> {
        	final int id = rc.getParameter("id").toInt();
        	rc.json().send(TaxonomyDB.readCollectionTaxonomy(id));
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
                    .node(coll).label("collection"),
                WHERE.valueOf(coll.id()).EQUALS(id),
                OPTIONAL_MATCH.node(coll)
                    .relation(e).type("CONTAINS")
                    .node().label("entry"),
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

        POST("/{id}/reject", (rc) -> {
			String email = rc.getSession("email");
			int id = rc.getParameter("id").toInt();
			JcNode user = new JcNode("user");
			JcNode coll = new JcNode("coll");
			JcRelation rel = new JcRelation("rel");

			handleInvitation(rc, "rejected");

			// Delete invitation
			JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[] {
                MATCH.node(user).label("user").property("email").value(email)
                    .relation(rel).type("INVITE")
                    .node(coll).label("collection"),
                WHERE.valueOf(coll.id()).EQUALS(id),
                DO.DELETE(rel),
                NATIVE.cypher("RETURN TRUE AS ok")
            });

			if (res.resultOf(new JcBoolean("ok")).size() > 0)
				rc.getResponse().ok();
			else
				throw new RequestException("Not invited to that collection.");
		});

        // POST api.serpconnect.cs.lth.se/{id}/accept HTTP/1.1
        POST("/{id}/accept", (rc) -> {
            String email = rc.getSession("email");
            int id = rc.getParameter("id").toInt();

            JcNode user = new JcNode("user");
            JcNode coll = new JcNode("coll");
            JcRelation rel = new JcRelation("rel");

            handleInvitation(rc,"accepted");

            // Replace an INVITE type relation with a MEMBER_OF relation
            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
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

        // Must be logged in AND member of collection to proceed (or ADMIN)
        ALL("/{id}/.*", (rc) -> {
            final String email = rc.getSession("email");
            final int id = rc.getParameter("id").toInt();

            final JcNode coll = new JcNode("coll");

            final JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node().label("user").property("email").value(email)
                    .relation().out().type("MEMBER_OF")
                    .node(coll).label("collection"),
                WHERE.valueOf(coll.id()).EQUALS(id),
                NATIVE.cypher("RETURN TRUE AS ok")
            });

            /* allow admins to do whatever they want */
            boolean isAdmin = false;
            if (email != null) {
                AccountSystem.Account user = AccountSystem.findByEmail(email);
                if (TrustLevel.authorize(user.trust, TrustLevel.ADMIN)) {
                    isAdmin = true;
                }
            }
            
            if (res.resultOf(new JcBoolean("ok")).size() == 0 && !isAdmin)
            throw new RequestException(403, "You are not a member of that collection");
            
            rc.setLocal("admin", isAdmin);
            rc.next();
        });
        
        // GET /{id}/entities --> [{id:1,text:"yalla"}]
        GET("/{id}/entities", (rc) -> {
        	final long id = rc.getParameter("id").toLong();
        	final JcNode collection = new JcNode("c");
        	final JcNode entity = new JcNode("e");
        	final JcNumber eid = new JcNumber("eid");
        	final JcString txt = new JcString("txt");

        	JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
        		MATCH.node(collection)
            		.relation().type("CONTAINS")
            		.node().label("entry")
            		.relation()
            		.node(entity).label("facet"),
            	WHERE.valueOf(collection.id()).EQUALS(id),
            	RETURN.DISTINCT().value(entity.id()).AS(eid),
            	RETURN.value(entity.property("text")).AS(txt)
            });

            List<BigDecimal> entityIds = res.resultOf(eid);
            List<String> entityText = res.resultOf(txt);
            
            class Entity {
            	public long id;
            	public String text;
            	public Entity(long id, String text) {
            		this.id = id;
            		this.text = text;
            	}
            }

            Entity[] entities = new Entity[entityIds.size()];
            for (int i = 0; i < entities.length; i++)
            	entities[i] = new Entity(entityIds.get(i).longValue(), entityText.get(i));

            rc.json().send(entities);
        });
        
        // GET /{id}/classification --> [{facetId:"PLANNING,text:["yalla"]}]
        GET("/{id}/classification", (rc) -> {
        	final long id = rc.getParameter("id").toLong();
        	final JcNode collection = new JcNode("c");
        	final JcNode entity = new JcNode("e");
        	final JcRelation facet = new JcRelation("f");

        	JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
        		MATCH.node(collection)
            		.relation().type("CONTAINS")
            		.node().label("entry")
            		.relation(facet)
            		.node(entity).label("facet"),
            	WHERE.valueOf(collection.id()).EQUALS(id),
            	NATIVE.cypher("RETURN COLLECT(DISTINCT e.text) AS text, type(f) AS rel"),
            	NATIVE.cypher("ORDER BY type(f)")
            });

            List<List<?>> facetText = res.resultOf(new JcCollection("text"));
            List<String> facetTypes = res.resultOf(new JcString("rel"));

            TaxonomyFacet[] facets = new TaxonomyFacet[facetTypes.size()];
            for (int i = 0; i < facets.length; i++)
            	facets[i] = new TaxonomyFacet(facetTypes.get(i), facetText.get(i));

            rc.json().send(facets);
        });
        
        
        // POST /55/reclassify {oldType:facetId,newType:facetId,entities:[33,13]}
        POST("/{id}/reclassify", (rc) -> {
        	final long id = rc.getParameter("id").toLong();
        	final ReclassifyRequest req = rc.createEntityFromBody(ReclassifyRequest.class);
        	
        	for (long eid : req.entities) {
        		final JcNode c = new JcNode("c");
        		final JcRelation r = new JcRelation("r");
        		final JcNode n = new JcNode("n");
        		final JcNode e = new JcNode("e");
        		
        		Database.query(rc.getLocal("db"), new IClause[]{
        			MATCH.node(c).label("collection")
        				.relation().type("CONTAINS")
        				.node(n).label("entry")
        				.relation(r).type(req.oldFacetId)
        				.node(e).label("facet"),
        			WHERE.valueOf(c.id()).EQUALS(id).AND().valueOf(e.id()).EQUALS(eid),
        			CREATE.node(n).relation().type(req.newFacetId).out().node(e),
        			DO.DELETE(r)
        		});
        	}
        	
        	rc.getResponse().ok();
        });

        // POST api.serpconnect.cs.lth.se/{id}/leave HTTP/1.1
        POST("/{id}/leave", (rc) -> {
            final String email = rc.getSession("email");
            final int id = rc.getParameter("id").toInt();
            new LeaveCollectionEvent(id, email).execute();
            rc.getResponse().ok();
        });

        // POST api.serpconnect.cs.lth.se/{id}/removeEntry HTTP/1.1
        // entryId=...
        POST("/{id}/removeEntry", (rc) -> {
            if (rc.getParameter("entryId").isEmpty())
            	throw new RequestException("Must provide entryId");
            
            new DeleteEntryEvent(rc.getParameter("entryId").toInt()).execute();
            rc.getResponse().ok();
        });

        // POST api.serpconnect.cs.lth.se/{id}/addEntry HTTP/1.1
        // entryId=...
        POST("/{id}/addEntry", (rc) -> {
        	/**
        	 * Hehe this route is actually never used in the frontend.
        	 */
            final int id = rc.getParameter("id").toInt();
            if (rc.getParameter("entryId").isEmpty())
            	throw new RequestException("Must provide entryId");
            
            final int entryId = rc.getParameter("entryId").toInt();

            final JcNode entry = new JcNode("e");
            final JcNode coll = new JcNode("c");
            final JcNode entity = new JcNode("x");
            final JcRelation facet = new JcRelation("f");
            final JcNumber entityId = new JcNumber("d");
            final JcString facetType = new JcString("t");
            
            final JcQueryResult rr = Database.query(rc.getLocal("db"), new IClause[]{
            	MATCH.node(entry).label("entry")
            		.relation(facet)
            		.node(entity).label("facet"),
            	WHERE.valueOf(entry.id()).EQUALS(entryId),
            	RETURN.value(entry).AS(entry),
            	RETURN.value(facet.type()).AS(facetType),
            	RETURN.value(entity.id()).AS(entityId)
            });
            
            final Graph.Node graphNode = new Graph.Node(rr.resultOf(entry).get(0));
            
            final JcQueryResult cqr = Database.query(rc.getLocal("db"), new IClause[]{
            	MATCH.node(coll).label("collection"),
            	WHERE.valueOf(coll.id()).EQUALS(id),
        		graphNode.create(entry),
        		CREATE.node(entry)
        			.relation().type("CONTAINS").out()
        			.node(coll),
        		RETURN.value(entry.id()).AS(entityId)
            });

            // Only copy classification relations if they exist in the
            // extended or default taxonomy.
            ArrayList<String> taxonomy = new ArrayList<String>();
            for (Facet f : TaxonomyDB.SERP().taxonomy)
            	taxonomy.add(f.id);
            
        	for (Facet f : TaxonomyDB.taxonomyOf(id).taxonomy)
        		taxonomy.add(f.id);
            
        	final long newEntryId = cqr.resultOf(entityId).get(0).longValue();
        	final List<String> facets = rr.resultOf(facetType);
        	final List<BigDecimal> entities = rr.resultOf(entityId);
            for (int i = 0; i < facets.size(); i++) {
            	final String type = facets.get(i);
            	
            	if (!taxonomy.contains(type))
            		continue;
            	
            	Database.query(rc.getLocal("db"), new IClause[]{
            		MATCH.node(entry).label("entry"),
            		MATCH.node(entity).label("facet"),
            		WHERE.valueOf(entry.id()).EQUALS(newEntryId)
            			.AND().valueOf(entity.id()).EQUALS(entities.get(i).longValue()),
            		CREATE.node(entry).relation().type(type).out().node(entity)
            	});
            }
            
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

        //return true if current logged in user is owner of the given collection
        GET("/{id}/is-owner", (rc)->{
    		 rc.status(200).json().send(isOwner(rc));
        });

        //must be logged in AND be member AND be owner of collection AND provide an email to proceed
        ALL("/{id}/.*", (rc) -> {
        	if(!isOwner(rc))
       		 	throw new RequestException(401, "You must be an owner of the collection");

            rc.next();
        });

        // POST api.serpconnect.cs.lth.se/{id}/kick HTTP/1.1
        // email=...
        POST("/{id}/kick", (rc) -> {
        	if (rc.getParameter("email").isEmpty())
                throw new RequestException("Invalid email");
        	 String email = rc.getParameter("email").toString();

        	 //don't allow the user to kick himself
    		 if(rc.getSession("email").toString().equals(rc.getParameter("email").toString()))
        		throw new RequestException(400, "Can't kick yourself, please use leave collection if you want to leave the collection");

        	 int id = rc.getParameter("id").toInt();
             JcNode user = new JcNode("user");
             JcNode coll = new JcNode("coll");
             JcRelation rel = new JcRelation("connection");

             JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                 MATCH.node(user).label("user").property("email").value(email)
                     .relation(rel)
                     .node(coll).label("collection"),
                 WHERE.valueOf(coll.id()).EQUALS(id),
                 DO.DELETE(rel),
                 RETURN.value(user)
             });

             if(res.resultOf(user).isEmpty())
            	 throw new RequestException(404, email + " is not a member of collection " + id);

        	 rc.getResponse().ok();
        });

        // POST api.serpconnect.cs.lth.se/{id}/invite HTTP/1.1
        // email[0]=...&email[1]=
        POST("/{id}/invite", (rc) -> {
        	if (rc.getParameter("email").isEmpty())
                throw new RequestException("Invalid email");
            int id = rc.getParameter("id").toInt();
            List<String> emails = rc.getParameter("email").toList(String.class);
            String inviter = rc.getSession("email");

            for (String email : emails) {
                JcNode user = new JcNode("user");
                JcNode inviterNode = new JcNode("inviter");
                JcNode coll = new JcNode("coll");

                boolean memberOf = Database.query(rc.getLocal("db"), new IClause[]{
                        MATCH.node().label("user").property("email").value(email).
                        relation().out().type("MEMBER_OF").
                        node(coll),
                        WHERE.valueOf(coll.id()).EQUALS(id),
                        NATIVE.cypher("RETURN TRUE AS ok")
                }).resultOf(new JcBoolean("ok")).size() > 0;

                // Don't send new invites if already member
                if(memberOf)
                    continue;

                JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                    MATCH.node(user).label("user").property("email").value(email),
                    RETURN.value(user)
                });

                boolean emptyUser = res.resultOf(user).isEmpty();

                //create temporary unregistered user if non existent
                if (emptyUser) {
                    AccountSystem.createAccount(email, "", TrustLevel.UNREGISTERED);
                } else if(res.resultOf(user).get(0).getProperty("trust").getValue().equals(TrustLevel.UNREGISTERED)){
                    emptyUser = true;
                }

                // Use MERGE so we don't end up with multiple invites per user
                // keep track of who invited the user and to which collection
                Database.query(rc.getLocal("db"), new IClause[] {
                    MATCH.node(user).label("user").property("email").value(email),
                    MATCH.node(coll).label("collection"),
                    WHERE.valueOf(coll.id()).EQUALS(id),
                    MATCH.node(inviterNode).label("user").property("email").value(inviter),
                    MERGE.node(user).relation().out().type("INVITE").node(coll),
                    MERGE.node(user).relation().out().type("INVITER")
                        .property("parentnode").value(id).node(inviterNode)
                });

                String template = emptyUser ? inviteNewUserTemplate
                                            : inviteTemplate;
                template = template.replace("{frontend}", frontend);

                app.getMailClient().sendEmail(email, "SERP Connect - Collection Invite", template);
            }

            rc.getResponse().ok();
        });
        
        // { version: number, taxonomy: [...] }
        PUT("/{id}/taxonomy", (rc) -> {
        	final int id = rc.getParameter("id").toInt();
            final ObjectMapper mapper = new ObjectMapper(); 
            TaxonomyDB.Taxonomy req;

        	try {
        		req = mapper.readValue(rc.getRequest().getBody(), 
        								TaxonomyDB.Taxonomy.class);
			} catch (JsonProcessingException e) {
				throw new RequestException("Illegal JSON");
			} catch (IOException e) {
				throw new RequestException("Illegal JSON");
			}
            long current = TaxonomyDB.taxonomyOf(id).version;
            
            if (req.version < current)
                throw new RequestException("Out of date version. Stored=" + 
                							current + ", Request=" + req.version);
            
            if (req.version == current)
            	req.version += 1;
            
			try {
				TaxonomyDB.update(id, req);
			} catch (JsonProcessingException e) {
				throw new RequestException("Sneaky illegal JSON");
			}
            
    	    rc.getResponse().ok();
        });

    }

    public static boolean isOwner(RouteContext rc){
    	final int id = rc.getParameter("id").toInt();
    	final String email = rc.getSession("email");
    	final JcNode usr = new JcNode("u");
    	final JcNode coll = new JcNode("c");

    	//return true as ok if the logged in user is owner of the collection
    	JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
             MATCH.node(usr).label("user").
             	property("email").value(email).
             	relation().type("OWNER").in().
             	node(coll).label("collection"),
             WHERE.valueOf(coll.id()).EQUALS(id),
             NATIVE.cypher("RETURN true AS ok")
    	});

		return res.resultOf(new JcBoolean("ok")).size()>0;
    }

    //Calls static handleInvitation. Exists to make the code cleaner for functions in this class.
    private void handleInvitation(RouteContext rc, String action){
    	final String email = rc.getSession("email");
		final int id = rc.getParameter("id").toInt();

		handleInvitation(rc.getLocal("db"),email,id,action,app);
    }

	//will reply an email to all users that invited this user which informs them
	//of what action was taken.
	public static void handleInvitation(IDBAccess db, String email, int id, String action, Connect app) {
		// 1 invitee -- Many inviters: Must destroy all relations
		final JcNode user = new JcNode("user");
		final JcNode inviter = new JcNode("inviter");
		final JcRelation rel = new JcRelation("rel");
		final JcString em = new JcString("em");
		final JcString collName = new JcString("collName");
		final JcNode coll = new JcNode("coll2");

		JcQueryResult inviters = Database.query(db, new IClause[] {
            MATCH.node(user).label("user").property("email").value(email)
                .relation(rel).type("INVITER").node(inviter),
            WHERE.valueOf(rel.property("parentnode").toInt()).EQUALS(id),
            DO.DELETE(rel),
            RETURN.value(inviter.property("email")).AS(em)
		});

		// Get the name of the collection.
		final JcQueryResult collQuery = Database.query(db, new IClause[] {
            MATCH.node(coll).label("collection"),
			WHERE.valueOf(coll.id()).EQUALS(id),
            RETURN.value(coll.property("name")).AS(collName)
        });

        final String template = inviteActionTemplate
            .replace("{user}", email)
            .replace("{action}", action)
            .replace("{collection}", collQuery.resultOf(collName).get(0));

		// Maybe only send email to person whose invite user accepted?
        final List<String> invitors = inviters.resultOf(em);
        for (String invitor : invitors) {
            app.getMailClient().sendEmail(invitor, "SERP Connect - Collection Invite Response", template);
        }
	}
}
