package se.lth.cs.connect.routes;

import com.fasterxml.jackson.core.JsonProcessingException;

import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.CREATE;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.OPTIONAL_MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.factories.clause.WHERE;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcNumber;
import iot.jcypher.query.values.JcRelation;
import java.util.List;
import java.util.regex.Pattern;
import iot.jcypher.query.values.JcString;
import ro.pippo.core.PippoSettings;
import se.lth.cs.connect.Connect;
import se.lth.cs.connect.RequestException;
import se.lth.cs.connect.TrustLevel;
import se.lth.cs.connect.modules.AccountSystem;
import se.lth.cs.connect.modules.Database;
import se.lth.cs.connect.modules.TaxonomyDB;
import se.lth.cs.connect.Graph;
import se.lth.cs.connect.DatabaseException;
import iot.jcypher.query.result.JcError;

public class Project extends BackendRouter {

    static final Pattern NAME_VALIDATOR = Pattern.compile("[a-z0-9]+", Pattern.CASE_INSENSITIVE);

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

            final String name = rc.getParameter("name").toString();
            if (name == null || name.isEmpty())
                throw new RequestException("Must specify 'name' parameter.");

            if (!NAME_VALIDATOR.matcher(name).matches())
                throw new RequestException("'name' parameter must only use alphanumeric characters.");

            final String link = rc.getParameter("link").toString();
            if (link == null || link.isEmpty())
                throw new RequestException("Must specify 'link' parameter.");

            JcNode userNode = new JcNode("user");
            try {
                JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[] {
                    MATCH.node(userNode).label("user")
                        .property("email").value(user.email),
                    CREATE.node().label("project")
                        .property("name").value(name)
                        .property("link").value(link)
                        .relation().out().type("CREATED_BY").node(userNode)
                });
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
            }

            class ReturnVal {
                public String name, link;
                public ReturnVal(String n, String l) {
                    name = n;
                    link = l;
                }
            }
            rc.json().send(new ReturnVal(name, link));
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
            final ReturnVal ret = new ReturnVal(projects);
            rc.json().send(ret);
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

        // DELETE /project/xyz
        DELETE("/{name}", (rc) -> {
            authorize(rc);

            // delete ok
        });

        // PUT /project/xyz/taxonomy
        PUT("/{name}/taxonomy", (rc) -> {
            authorize(rc);

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
    static void authorize(RouteContext rc) {
        final String name = rc.getParameter("name").toString();
        final String email = rc.getSession("email");
        if (email == null)
            throw new RequestException(401, "Must be logged in.");

        final JcNode proj = new JcNode("p");
        final JcNode user = new JcNode("u");
        final JcRelation rel = new JcRelation("r");
        final JcNumber trust = new JcNumber("t");
        JcQueryResult res = Datbase.query(rc.getLocal("db"), new IClause[]{
            MATCH.node(user).label("user")
                .property("email").value(email),
            OPTIONAL_MATCH.node(proj).label("project")
                .property("name").value(name),
            OPTIONAL_MATCH.node(proj)
                .relation(rel).type("CREATED_BY")
                .node(user),
            RETURN.value(proj),
            RETURN.value(rel),
            RETURN.value(user.property("trust")).AS(trust);
        });

        final boolean exists = res.resultOf(proj).size() > 0;
        if (!exists) {
            throw new RequestException(404, "No such project exists.");
        }

        final boolean creator = res.resultOf(rel).size() > 0;
        final int trustValue = res.resultOf(trust).get(0).intValue();
        final boolean admin = TrustLevel.authorize(trustValue, TrustLevel.ADMIN);

        if (!creator && !admin)
            throw new RequestException(403, "You are not authorized for this project.");
    }
}