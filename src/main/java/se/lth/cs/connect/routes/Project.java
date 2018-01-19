package se.lth.cs.connect.routes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

import iot.jcypher.graph.GrNode;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.CREATE;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.OPTIONAL_MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.factories.clause.SEPARATE;
import iot.jcypher.query.result.JcError;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcNumber;
import iot.jcypher.query.values.JcRelation;
import ro.pippo.core.PippoSettings;
import ro.pippo.core.route.RouteContext;
import se.lth.cs.connect.Connect;
import se.lth.cs.connect.DatabaseException;
import se.lth.cs.connect.Graph;
import se.lth.cs.connect.RequestException;
import se.lth.cs.connect.TrustLevel;
import se.lth.cs.connect.modules.AccountSystem;
import se.lth.cs.connect.modules.Database;
import se.lth.cs.connect.modules.TaxonomyDB;

public class Project extends BackendRouter {

    static final Pattern NAME_VALIDATOR = Pattern.compile("[a-z0-9\\-]+", Pattern.CASE_INSENSITIVE);

    public Project(Connect app) {
        super(app);
    }

    @Override
    public String getPrefix() { return "/v1/project"; }

    @Override
    protected void setup(PippoSettings conf) {

        // POST /project name=x link=y
        // --> { name: x, link: y }
        POST("", (rc) -> {
            if (rc.getSession("email") == null)
                throw new RequestException(401, "Must be logged in.");

            AccountSystem.Account user = AccountSystem.findByEmail(rc.getSession("email"));
            if (!TrustLevel.authorize(user.trust, TrustLevel.VERIFIED))
                throw new RequestException(403, "Account must be at least verified.");

            if (rc.getParameter("name").isNull() || rc.getParameter("name").isEmpty())
                throw new RequestException("Must specify 'name' parameter.");

            final String name = rc.getParameter("name").toString();
            if (!NAME_VALIDATOR.matcher(name).matches())
                throw new RequestException("'name' parameter must only use [a-zA-Z0-9-] characters.");

            if (rc.getParameter("link").isNull() || rc.getParameter("link").isEmpty())
                throw new RequestException("Must specify 'link' parameter.");
            final String link = rc.getParameter("link").toString();

            final JcNode userNode = new JcNode("user");
            final JcNode projNode = new JcNode("proj");
            GrNode graphNode = null;
            try {
                JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[] {
                    MATCH.node(userNode).label("user")
                        .property("email").value(user.email),
                    CREATE.node(projNode).label("project")
                        .property("name").value(name)
                        .property("link").value(link)
                        .relation().out().type("CREATED_BY").node(userNode),
                    RETURN.value(projNode)
                });
                graphNode = res.resultOf(projNode).get(0);
            } catch (DatabaseException de) {
                List<JcError> db = de.errors().db;
                final String alreadyInUse = "Neo.ClientError.Schema.ConstraintViolation";
                for (JcError err : db) {
                    if (alreadyInUse.equals(err.getCodeOrType()))
                        throw new RequestException(400, "Project name is already in use.");
                }
                throw de;
            }

            try {
                TaxonomyDB.Taxonomy defaultTaxonomy = new TaxonomyDB.Taxonomy();
                TaxonomyDB.update(TaxonomyDB.project(name), defaultTaxonomy);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new RequestException(500, "Error creating new taxonomy file.");
            }

            rc.json().send(new Graph.Project(graphNode));
        });

        // GET /project
        // --> { projects: [{name:"",link:""}, ... ] }
        GET("", (rc) -> {
            JcNode project = new JcNode("p");

            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[] {
                MATCH.node(project).label("project"),
                RETURN.value(project)
            });

            class ReturnVal {
                public Graph.Project[] projects;
                public ReturnVal(Graph.Project[] p) { projects = p; }
            }

            final Graph.Project[] projects = Graph.Project.fromList(res.resultOf(project));
            rc.json().send(new ReturnVal(projects));
        });

        // GET /project/xyz
        // --> { name: "xyz", link: "http//serp.xyz" }
        GET("/{name}", (rc) -> {
            final JcNode project = new JcNode("p");
            final String name = rc.getParameter("name").toString();
            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[] {
                MATCH.node(project).label("project")
                    .property("name").value(name),
                RETURN.value(project)
            });

            List<GrNode> matches = res.resultOf(project);
            if (matches.size() == 0)
                throw new RequestException(404, "Project not found.");

            rc.json().send(new Graph.Project(matches.get(0)));
        });

        // GET /project/xyz/taxonomy
        // --> {version:X, taxonomy:[{id,name,parent}]}
        GET("/{name}/taxonomy", (rc) -> {
            final String name = rc.getParameter("name").toString();
            try {
                rc.json().send(TaxonomyDB.read(TaxonomyDB.project(name)));
            } catch (RequestException re) {
                if (re.getStatus() == 404)
                    throw new RequestException(404, "No such project.");
                else
                    throw new RequestException(500, "Error reading project taxonomy.");
            }
        });

        // PUT /project/xyz [name=xyz1] [link=serp.xyz]
        PUT("/{id}", (rc) -> {
            authorize(rc, rc.getParameter("id").toString());

            /* id to avoid name conflict with request data */
            final String id = rc.getParameter("id").toString();
            final JcNode proj = new JcNode("p");
            final ArrayList<IClause> queryBuilder = new ArrayList<IClause>(2);
            queryBuilder.add(MATCH.node(proj).label("project")
                .property("name").value(id));

            if (!rc.getParameter("name").isNull()) {
                final String name = rc.getParameter("name").toString();
                queryBuilder.add(DO.SET(proj.property("name")).to(name));
            }

            if (!rc.getParameter("link").isNull()) {
                final String link = rc.getParameter("link").toString();
                queryBuilder.add(DO.SET(proj.property("link")).to(link));
            }

            if (queryBuilder.size() > 1) {
                try {
                    IClause[] query = new IClause[queryBuilder.size()];
                    queryBuilder.toArray(query);
                    Database.query(rc.getLocal("db"), query);

                    // Must also update the name of the taxonomy database file
                    if (!rc.getParameter("name").isNull()) {
                        final String name = rc.getParameter("name").toString();
                        final String oldCtx = TaxonomyDB.project(id);
                        final String newCtx = TaxonomyDB.project(name);

                        final File dbfile = new File(oldCtx);
                        if (!dbfile.renameTo(new File(newCtx))) {
                            String data = TaxonomyDB.read(oldCtx);
                            TaxonomyDB.write(newCtx, data);
                            dbfile.delete();
                        }
                    }

                    rc.getResponse().ok();
                } catch (DatabaseException de) {
                    List<JcError> db = de.errors().db;
                    final String alreadyInUse = "Neo.ClientError.Schema.ConstraintViolation";
                    for (JcError err : db) {
                        if (alreadyInUse.equals(err.getCodeOrType()))
                            throw new RequestException(400, "Project name is already in use.");
                    }
                    throw de;
                }
            } else {
                throw new RequestException(400, "Must include at least one parameter.");
            }
        });

        // POST /project/xyz/delete
        POST("/{name}/delete", (rc) -> {
            authorize(rc, rc.getParameter("name").toString());
            throw new RequestException(500, "Not yet implemented!");
        });

        // PUT /project/xyz/taxonomy
        PUT("/{name}/taxonomy", (rc) -> {
            authorize(rc, rc.getParameter("name").toString());

            final String name = rc.getParameter("name").toString();
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
            final String ctx = TaxonomyDB.project(name);
            long current = TaxonomyDB.taxonomyOf(ctx).version;

            if (req.version < current)
                throw new RequestException("Out of date version. Stored=" +
                                            current + ", Request=" + req.version);

            if (req.version == current)
                req.version += 1;

            try {
                TaxonomyDB.update(ctx, req);
            } catch (JsonProcessingException e) {
                throw new RequestException("Sneaky illegal JSON");
            }

            rc.getResponse().ok();
        });
    }

    // Static method instead of filter b/c url regex not guaranteed to work
    static void authorize(RouteContext rc, String name) {
        final String email = rc.getSession("email");
        if (email == null)
            throw new RequestException(401, "Must be logged in.");

        final JcNode proj = new JcNode("p");
        final JcNode user = new JcNode("u");
        final JcRelation rel = new JcRelation("r");
        final JcNumber trust = new JcNumber("x");
        JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
            MATCH.node(user).label("user")
                .property("email").value(email),
            SEPARATE.nextClause(),
            OPTIONAL_MATCH.node(proj).label("project")
                .property("name").value(name),
            SEPARATE.nextClause(),
            OPTIONAL_MATCH.node(user)
                .relation(rel).type("CREATED_BY")
                .node(proj),
            RETURN.value(user.property("trust")).AS(trust),
            RETURN.value(proj),
            RETURN.value(rel)
        });

        final boolean not_found = res.resultOf(proj).get(0) == null;
        if (not_found)
            throw new RequestException(404, "No such project exists.");

        final int trustValue = res.resultOf(trust).get(0).intValue();
        final boolean admin = TrustLevel.authorize(trustValue, TrustLevel.ADMIN);
        final boolean creator = res.resultOf(rel).get(0) != null;
        if (!(creator || admin)) {
            throw new RequestException(403, "You are not authorized for this project.");
        }
    }
}