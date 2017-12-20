package se.lth.cs.connect.routes;

import com.fasterxml.jackson.core.JsonProcessingException;

import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.CREATE;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.factories.clause.WHERE;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcNumber;
import iot.jcypher.query.values.JcString;
import ro.pippo.core.PippoSettings;
import se.lth.cs.connect.Connect;
import se.lth.cs.connect.RequestException;
import se.lth.cs.connect.TrustLevel;
import se.lth.cs.connect.modules.AccountSystem;
import se.lth.cs.connect.modules.Database;
import se.lth.cs.connect.modules.TaxonomyDB;

public class Project extends BackendRouter {

    public Project(Connect app) {
        super(app);
    }

    @Override
    public String getPrefix() { return "/v1/project"; }

    @Override
    protected void setup(PippoSettings conf) {

        /* GET / --> { projects: [name, name, ..., name ] } */
        GET("", (rc) -> {
            JcNode project = new JcNode("p");
            JcString name = new JcString("n");
            
            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[] {
                MATCH.node(project).label("project"),
                RETURN.value(project.property("name")).AS(name)
            });

            class ReturnVal {
                public String[] projects;
                public ReturnVal(int x) { projects = new String[x]; }
            }

            ReturnVal ret = new ReturnVal(res.resultOf(name).size());
            res.resultOf(name).toArray(ret.projects);

            rc.json().send(ret);
        });

        GET("/{id}", (rc) -> {
            JcNode project = new JcNode("p");
            JcString name = new JcString("n");
            final long pid = rc.getParameter("id").toLong();
            
            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[] {
                MATCH.node(project).label("project"),
                WHERE.valueOf(project.id()).EQUALS(pid),
                RETURN.value(project.property("name")).AS(name)
            });

            class ReturnVal {
                public String name;
                public ReturnVal(String n) { name = n; }
            }

            final String pname = res.resultOf(name).get(0);
            rc.json().send(new ReturnVal(pname));
        });

        // GET /taxonomy --> {version:X, taxonomy:[{id,name,parent}]}
        GET("/{name}/taxonomy", (rc) -> {
            final String name = rc.getParameter("name").toString();
            rc.json().send(TaxonomyDB.read(TaxonomyDB.project(name)));
        });

/*         ALL("/.*", (rc) -> {
            if (rc.getSession("email") == null)
                throw new RequestException("Must be logged in.");

            AccountSystem.Account user = AccountSystem.findByEmail(rc.getSession("email"));
            if (!TrustLevel.authorize(user.trust, TrustLevel.USER))
                throw new RequestException(403, "Account must be verified.");
        }); */

        /* POST / name=x --> { name: x, id: 0 } */
        POST("/", (rc) -> {
            JcNode project = new JcNode("p");
            JcNumber projectId = new JcNumber("x");
            final String name = rc.getParameter("name").toString();
            
            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[] {
                CREATE.node(project).label("project")
                    .property("name").value(name),
                RETURN.value(project.id()).AS(projectId)
            });

            final long pid = res.resultOf(projectId).get(0).longValue();

            try {
                TaxonomyDB.update(TaxonomyDB.project(name), new TaxonomyDB.Taxonomy());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            class ReturnVal {
                public String name;
                public long id;
                public ReturnVal(String n, long x) {
                    name = n;
                    id = x;
                }
            }
            rc.json().send(new ReturnVal(name, pid));
        });
    }
}