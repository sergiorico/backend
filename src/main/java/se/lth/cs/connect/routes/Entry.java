package se.lth.cs.connect.routes;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest; // for sha256
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// required for building queries and interpreting query results
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
import ro.pippo.core.PippoSettings;
import se.lth.cs.connect.Connect;
import se.lth.cs.connect.Graph;
import se.lth.cs.connect.RequestException;
import se.lth.cs.connect.TrustLevel;
import se.lth.cs.connect.modules.AccountSystem;
import se.lth.cs.connect.modules.Database;

/**
 Handles /entry routes for now.


    (GET /{hash} --> {entry + relations})
    POST /new
 */
public class Entry extends BackendRouter {

    public Entry(Connect app) {
        super(app);
    }

    @Override
    public String getPrefix() { return "/v1/entry"; }

    /**
     * The format for posting /new entries.
     *  {
     *      "description": "required text",
     *      "entryType": "research" or "challenge",
     *      "contact": "optional contact details",
     *      "reference": "reference for research",
     *      "collection": "optional collection id",
     *      "serpClassification": {
     *          "people": ["turtle-like people", "snakes"],
     *          "improving": ["improving life quality"]
     *      }
     *  }
     */
    static class NewEntry {

        public String description;
        public String entryType;
        public String contact;
        public String reference;
        public String collection;
        public String doi;
        public String date;
        public Map<String,List<String>> serpClassification;

        public boolean isResearch() {
            return "research".equals(entryType);
        }
        public boolean isChallenge() {
            return "challenge".equals(entryType);
        }

        public String validate() {
            if (isResearch()) {
                if (reference == null)
                    return "No reference(s).";

                return null;
            }

            if (isChallenge()) {
                if (description == null)
                    return "No description";

                return null;
            }

            return "Invalid entryType";
        }

        public List<IClause> taxonomy(JcNode entry) {
            List<IClause> query = new ArrayList<IClause>();
            if (serpClassification == null)
                return query;

            java.util.Base64.Encoder base64 = java.util.Base64.getEncoder();
            long fid = 0;
            for (String facet : serpClassification.keySet()) {
                List<String> examples = serpClassification.get(facet);

                if (examples == null)
                    continue;

                for (String txt : examples) {
                    if (txt == null)
                        continue;

                    byte[] hashDigest = hasher().digest(bytes(txt));
                    String hash = base64.encodeToString(hashDigest);

                    JcNode fnode = new JcNode("f" + fid);
                    query.add(MERGE.node(fnode).label("facet")
                        .property("hash").value(hash)
                        .property("text").value(txt));

                    query.add(CREATE.node(entry)
                        .relation().out().type(facet.toUpperCase())
                        .node(fnode));

                    fid++;
                }
            }
            return query;
        }

        // hash(research) = sha256(reference || doi)
        // hash(challenge) = sha256(desc )
        public String hash() {
            MessageDigest sha256 = hasher();

            if (isChallenge()) {
                sha256.update(bytes(description));
            }

            if (isResearch()) {
                sha256.update(bytes(reference));
                sha256.update(bytes(doi));
            }

            java.util.Base64.Encoder base64 = java.util.Base64.getEncoder();
            return base64.encodeToString(sha256.digest());
        }

        public IClause build(JcNode node) {
            String hval = hash();

            if (isResearch())
                return CREATE.node(node).label("entry").label(entryType)
                    .property("contact").value(contact)
                    .property("reference").value(reference)
                    .property("doi").value(doi)
                    .property("hash").value(hval)
                    .property("date").value(date);
            else
                return CREATE.node(node).label("entry").label(entryType)
                    .property("contact").value(contact)
                    .property("hash").value(hval)
                    .property("date").value(date)
                    .property("description").value(description);
        }
    }

    static class TaxonomyFacet {
        public String facet;
        public String[] text;

        public TaxonomyFacet(String facet, List<?> data) {
            this.facet = facet;
            this.text = new String[data.size()];

            for (int i = 0; i < data.size(); ++i)
                text[i] = (String)data.get(i);
        }
    }

    @Override
    protected void setup(PippoSettings conf) {
        // GET / --> {nodes:[], edges:[]}
        GET("", (rc) -> {
            JcNode node = new JcNode("entry");
            JcRelation rel = new JcRelation("rel");

            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(node).label("entry"),
                WHERE.NOT().has(node.property("pending")),
                OPTIONAL_MATCH.node(node).relation(rel).out().node().label("facet"),
                RETURN.value(node),
                RETURN.value(rel)
            });

            rc.json().send(new Graph(res.resultOf(node), res.resultOf(rel)));
        });

        // GET /taxonomy --> [{facet:'EXECUTION',texts:[samples]}, ..., {}]
        GET("/taxonomy", (rc) -> {
            // Dragon city
            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                NATIVE.cypher("MATCH (n)-[r]->(f:facet)"),
                NATIVE.cypher("WHERE n.pending IS NULL"),
                NATIVE.cypher("RETURN COLLECT(DISTINCT f.text) AS auto, type(r) AS rel"),
                NATIVE.cypher("ORDER BY type(r)")
            });

            List<List<?>> auto = res.resultOf(new JcCollection("auto"));
            List<String> rel = res.resultOf(new JcString("rel"));

            TaxonomyFacet[] facets = new TaxonomyFacet[rel.size()];
            for (int i = 0; i < facets.length; i++)
                facets[i] = new TaxonomyFacet(rel.get(i), auto.get(i));

            rc.json().send(facets);
        });

        // GET /{id} --> {entry}
        GET("/{id}", (rc) -> {
            JcNode node = new JcNode("entry");
            int id = rc.getParameter("id").toInt();

            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(node).label("entry"),
                WHERE.valueOf(node.id()).EQUALS(id),
                RETURN.value(node)
            });

            List<GrNode> entries = res.resultOf(node);

            // Catch both 0 and >1 cases; >1 requires human mistake
            if (entries.size() != 1)
                throw new RequestException("No entry with that id exists");
            else
                rc.json().send(new Graph.Node(entries.get(0)));
        });

        // GET /{id}/taxonomy --> { category: [], ..., category: [] }
        GET("/{id}/taxonomy", (rc) -> {
            int id = rc.getParameter("id").toInt();

            JcNode entry = new JcNode("e");
            JcNode facet = new JcNode("f");
            JcRelation rel = new JcRelation("r");
            JcString type = new JcString("t");

            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(entry).label("entry").relation(rel).node(facet).label("facet"),
                WHERE.valueOf(entry.id()).EQUALS(id),
                RETURN.value(facet),
                RETURN.value(rel.type()).AS(type)
            });

            List<GrNode> samples = res.resultOf(facet);
            List<String> relations = res.resultOf(type);

            Map<String,List<String>> taxonomy = new HashMap<String,List<String>>();
            for (int i = 0; i < relations.size(); i++) {
                String category = relations.get(i);
                if (!taxonomy.containsKey(category))
                    taxonomy.put(category, new ArrayList<String>());
                List<String> list = taxonomy.get(category);
                list.add(samples.get(i).getProperty("text").getValue().toString());
            }

            rc.json().send(taxonomy);
        });

        // PUT /{id}
        PUT("/{id}", (rc) -> {
            int id = rc.getParameter("id").toInt();
            if (rc.getSession("email") == null)
                throw new RequestException("Must be logged in.");

            AccountSystem.Account user = AccountSystem.findByEmail(rc.getSession("email"));
            if (!TrustLevel.authorize(user.trust, TrustLevel.USER))
                throw new RequestException(403, "Please verify account before submitting entries.");

            NewEntry e = rc.createEntityFromBody(NewEntry.class);

            JcNode entry = new JcNode("entry");
            JcNode unode = new JcNode("user");
            JcNode coll = new JcNode("collection");

            // Admins are allowed to edit any entry
            if (!TrustLevel.authorize(user.trust, TrustLevel.ADMIN)) {
                // User must have be connected to at least one collection that contains the entry
                JcQueryResult collAccess = Database.query(rc.getLocal("db"), new IClause[] {
                    MATCH.node(entry).label("entry").relation().type("CONTAINS")
                        .node(coll).label("collection").relation().type("MEMBER_OF")
                        .node().label("user").property("email").value(user.email),
                    WHERE.valueOf(entry.id()).EQUALS(id),
                    NATIVE.cypher("RETURN true as ok")
                });

                if (collAccess.resultOf(new JcBoolean("ok")).size() == 0)
                    throw new RequestException(403, "You don't have access to that collection.");
            }

            JcRelation rel = new JcRelation("rel");
            Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node(entry).label("entry").relation(rel).node().label("facet"),
                WHERE.valueOf(entry.id()).EQUALS(id),
                DO.DELETE(rel)
            });

            List<IClause> taxonomy = e.taxonomy(entry);
            ArrayList<IClause> update = new ArrayList();
            update.add(DO.SET(entry.property("contact")).to(e.contact));
            update.add(DO.SET(entry.property("hash")).to(e.hash()));
            if (e.isResearch()) {
                update.add(DO.SET(entry.property("reference")).to(e.reference));
                update.add(DO.SET(entry.property("doi")).to(e.doi));
            } else
                update.add(DO.SET(entry.property("description")).to(e.description));

            IClause[] query = new IClause[taxonomy.size() + update.size() + 2];
            query[0] = MATCH.node(entry).label("entry");
            query[1] = WHERE.valueOf(entry.id()).EQUALS(id);

            for (int i = 0; i < update.size(); i++)
                query[2 + i] = update.get(i);

            for (int i = 0; i < taxonomy.size(); i++)
                query[2 + update.size() + i] = taxonomy.get(i);

            Database.query(rc.getLocal("db"), query);

            rc.json().send("{\"message\": \"Ok\"}");
        });

        // POST /new --> {message: "ok", id: {id}}
        POST("/new", (rc) -> {
            if (rc.getSession("email") == null)
                throw new RequestException("Must be logged in.");

            AccountSystem.Account user = AccountSystem.findByEmail(rc.getSession("email"));
            if (!TrustLevel.authorize(user.trust, TrustLevel.USER))
                throw new RequestException(403, "Please verify account before submitting entries.");

            NewEntry e = rc.createEntityFromBody(NewEntry.class);
            if (e.contact == null || e.contact.length() == 0)
                e.contact = user.email;

            int collectionId = -1;
            try {
                collectionId = Integer.parseInt(e.collection);
                if (collectionId == -1)
                    collectionId = user.defaultCollection;
            } catch (NumberFormatException nfe) {
                throw new RequestException("Collection Id must to an integer");
            }
            
            boolean tagAsPending = !TrustLevel.authorize(user.trust, TrustLevel.VERIFIED);
            
            // Validate
            String err = e.validate();
            if (err != null)
                throw new RequestException(err);

            // Node is the new node
            JcNode entry = new JcNode("entry");
            JcNode unode = new JcNode("user");
            JcNode coll = new JcNode("collection");

            if(collectionId != -1){
                // User must have be connected to the specified collection id
                int access = Database.query(rc.getLocal("db"), new IClause[] {
                    MATCH.node().label("user").property("email").value(user.email)
                        .relation().type("MEMBER_OF")
                        .node(coll).label("collection"),
                    WHERE.valueOf(coll.id()).EQUALS(collectionId),
                    NATIVE.cypher("RETURN true as ok")
                }).resultOf(new JcBoolean("ok")).size();

                if (access == 0)
                    throw new RequestException(403, "You don't have access to that collection.");
            }

            List<IClause> taxonomy = e.taxonomy(entry);
            if (tagAsPending)
                taxonomy.add(DO.SET(entry.property("pending")).to(true));

            IClause[] query = new IClause[taxonomy.size() + 5 + 1];
            query[0] = MATCH.node(unode).label("user").property("email").value(user.email);
            query[1] = MATCH.node(coll).label("collection");
            query[2] = WHERE.valueOf(coll.id()).EQUALS(collectionId);
            query[3] = e.build(entry);
            query[4] = CREATE.node(coll).relation().out().type("CONTAINS").node(entry);

            for (int i = 0; i < taxonomy.size(); i++)
                query[5 + i] = taxonomy.get(i);
            
            query[taxonomy.size() + 5] = NATIVE.cypher("RETURN id(entry) as id");

            JcQueryResult jqr = Database.query(rc.getLocal("db"), query);
            String id = jqr.resultOf(new JcNumber("id")).get(0).toString();
            
            // TODO: Return something proper
            rc.json().send("{\"message\": \"Ok\", \"id\": " + id + "}");
        });
    }

    private static MessageDigest hasher() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException nsa) {
            throw new RequestException("Platform doesn't support SHA-256");
        }
    }

    private static byte[] bytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }
}