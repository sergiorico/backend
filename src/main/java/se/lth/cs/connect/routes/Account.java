package se.lth.cs.connect.routes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import iot.jcypher.database.IDBAccess;
// required for building queries and interpreting query results
import iot.jcypher.graph.GrNode;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.OPTIONAL_MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcString;
import ro.pippo.core.Messages;
import ro.pippo.core.PippoConstants;
import ro.pippo.core.PippoSettings;
import se.lth.cs.connect.Connect;
import se.lth.cs.connect.Graph;
import se.lth.cs.connect.RequestException;
import se.lth.cs.connect.TrustLevel;
import se.lth.cs.connect.events.DeleteAccountEvent;
import se.lth.cs.connect.modules.AccountSystem;
import se.lth.cs.connect.modules.Database;

/**
 * Handles account related actions.
 */
public class Account extends BackendRouter {
    private String registerTemplate,
                   resetPasswordRequestTemplate,
                   resetPasswordSuccessTemplate,
                   notifyAdminOnVerify;

    private String hostname, frontend, adminEmail;

    /**
     * Return an UTF-8, percent-encoded string. Used for base64-encoded tokens.
     */
    private String urlencode(String toencode) {
        try {
            return URLEncoder.encode(toencode, PippoConstants.UTF8);
        } catch (UnsupportedEncodingException uce) {
            return toencode;
        }
    }

    public Account(Connect app) {
        super(app);

        Messages msg = app.getMessages();
        registerTemplate = msg.get("pippo.register", "en");
        resetPasswordRequestTemplate = msg.get("pippo.passwordresetrequest", "en");
        resetPasswordSuccessTemplate = msg.get("pippo.passwordresetsuccess", "en");
        notifyAdminOnVerify = msg.get("pippo.notifyadminonverify", "en");

        hostname = app.getPippoSettings().getString("hostname", "http://localhost:8080");
        frontend = app.getPippoSettings().getString("frontend", "http://localhost:8181");
        adminEmail = app.getPippoSettings().getString("administrator.email", "en");
    }

    private List<GrNode> queryCollections(IDBAccess access, String email, String rel, String project) {
        final JcNode coll = new JcNode("coll");
        final JcNode proj = new JcNode("proj");
        return Database.query(access, new IClause[]{
            MATCH.node(proj).label("project")
                .property("name").value(project),
            MATCH.node().label("user").property("email").value(email)
                .relation().type(rel)
                .node(coll).label("collection")
                .relation().type("EXTENDS")
                .node(proj),
            RETURN.DISTINCT().value(coll)
        }).resultOf(coll);
    }

    private List<GrNode> queryCollections(IDBAccess access, String email, String rel) {
        final JcNode coll = new JcNode("coll");
        return Database.query(access, new IClause[]{
            MATCH.node().label("user").property("email").value(email)
                .relation().type(rel)
                .node(coll).label("collection"),
            RETURN.DISTINCT().value(coll)
        }).resultOf(coll);
    }

    @Override
	protected void setup(PippoSettings conf) {
        // POST api.serp.se/v1/account/login HTTP/1.1
        // email=...&passw=...
        POST("/login", (rc) -> {
            String email = rc.getParameter("email").toString();
            String passw = rc.getParameter("passw").toString();

            if (rc.getParameter("email").isEmpty())
                throw new RequestException("Must provide an email using 'email'");

            if (rc.getParameter("passw").isEmpty())
                throw new RequestException("Must provide a password using 'passw'");

            if (AccountSystem.authenticate(email, passw)) {
                rc.resetSession();
                rc.setSession("email", email);
                rc.getResponse().ok();
            } else {
                throw new RequestException("Invalid email/password combination.");
            }
        });

        // POST api.serp.se/v1/account/register HTTP/1.1
        // email=...&passw=...
        POST("/register", (rc) -> {
            String email = rc.getParameter("email").toString();
            String passw = rc.getParameter("passw").toString();

            if (rc.getParameter("email").isEmpty())
                throw new RequestException("Must specify an email address using 'email'.");

            if (rc.getParameter("passw").isEmpty())
                throw new RequestException("Must specify a password using 'passw'.");

            if (!AccountSystem.createAccount(email, passw, TrustLevel.REGISTERED))
                throw new RequestException("Email is already registered.");

            String token = urlencode(AccountSystem.generateEmailToken(email));
            String message = registerTemplate
                .replace("{token}", token)
                .replace("{hostname}", hostname);

            app.getMailClient().
                sendEmail(email, "SERP connect registration", message);


            rc.resetSession();
            rc.setSession("email", email);
            rc.getResponse().ok();
        });

        // GET api.serp.se/v1/account/reset-password?email=... HTTP/1.1
        POST("/reset-password", (rc) -> {
            // randomize new password and send it to user
            String email = rc.getParameter("email").toString();

            if (rc.getParameter("email").isEmpty()) {
                throw new RequestException("Must provide an email.");
            } else if (AccountSystem.findByEmail(email) == null) {
                throw new RequestException("Email is not registered.");
            } else {
                String token = urlencode(AccountSystem.generatePasswordToken(email));
                String message = resetPasswordRequestTemplate
                    .replace("{token}", token)
                    .replace("{hostname}", hostname);

                app.getMailClient().
        			sendEmail(email, "Password reset request", message);

                rc.getResponse().ok();
            }
        });

        // GET api.serp.se/v1/account/reset-password?token=igotmypermitrighthere HTTP/1.1
        GET("/reset-password", (rc) -> {
            String token = rc.getParameter("token").toString();

            if (rc.getParameter("token").isEmpty())
                throw new RequestException("Must provide a reset token");

            String email = AccountSystem.verifyResetPasswordToken(token);
            if (email == null)
            	throw new RequestException("Invalid reset token!");

            rc.resetSession();
            rc.setSession("resetemail", email);
            rc.redirect(frontend + "/resetpassword.html");
        });

        // POST api.serp.se/v1/account/reset-password-confirm
        // passw=...
        POST("/reset-password-confirm",(rc)-> {
        	if(rc.getSession("resetemail") == null)
        		throw new RequestException("Session 'resetemail' is not set");
        	if(rc.getParameter("passw").isEmpty()){
        		throw new RequestException("Must provide a password.");
        	}

        	String email = rc.getSession("resetemail").toString();
        	String password = rc.getParameter("passw").toString();

		    AccountSystem.changePassword(email, password);

		    rc.resetSession();
        	rc.setSession("email", email);
        	rc.getResponse().ok();
        });


        // GET api.serp.se/v1/account/verify?token=verifyaccounttoken HTTP/1.1
        GET("/verify", (rc) -> {
            String token = rc.getParameter("token").toString();

            if (rc.getParameter("token").isEmpty())
                throw new RequestException("Must provide a token.");

            String email = AccountSystem.verifyEmail(token);
            if (email == null)
                throw new RequestException("Invalid token: " + token);

            String message = notifyAdminOnVerify.replace("{email}", email);
            app.getMailClient().
    			sendEmail(adminEmail, "SERP connect email registration", message);

            rc.resetSession();
            rc.setSession("email", email);
            rc.redirect(frontend + "/profile.html");
        });

        // -- REQUIRES LOGIN --

        ALL("/.*", (rc) -> {
            if (rc.getSession("email") == null) {
                throw new RequestException(401, "Not logged in.");
            }
            rc.next();
        });

        // GET api.serp.se/v1/account/login HTTP/1.1
        GET("/login", (rc) -> {
            rc.getResponse().ok();
        });

         // GET api.serp.se/v1/account/friends HTTP/1.1
        GET("/friends", (rc)->{
        	final JcNode coll = new JcNode("coll");
        	final JcNode u = new JcNode("u");
            final JcString friends = new JcString("friends");

        	//find all emails of users in mutual collections
        	String email = rc.getParameter("email").toString();

        	List<String> res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node().label("user").property("email").value(email)
                    .relation().type("MEMBER_OF").node(coll).label("collection")
                    .relation().type("MEMBER_OF").node(u).label("user"),
                RETURN.DISTINCT().value(u.property("email")).AS(friends)
            }).resultOf(friends);

        	rc.json().send(res.toArray());
        });

        // GET /account/collections?project=xyz
        // --> [ {collection}, ..., {collection} ]
        GET("/collections", (rc) -> {
            if (rc.getParameter("project").isEmpty())
                throw new RequestException("Must include 'project' parameter.");
            final String project = rc.getParameter("project").toString();
            final List<GrNode> groups = queryCollections(rc.getLocal("db"), rc.getSession("email"), "MEMBER_OF", project);
            rc.status(200).json().send(Graph.Collection.fromList(groups));
        });

        // GET /account/projects
        // --> [ { name:"", link: "" }, ..., { name:"", link:"" } ]
        GET("/projects", (rc) -> {
            final String email = rc.getSession("email");

            JcNode p = new JcNode("p");
            List<GrNode> projects = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node().label("user").property("email").value(email)
                    .relation().in().type("CREATED_BY")
                    .node(p).label("project"),
                RETURN.value(p)
            }).resultOf(p);

            rc.json().send(Graph.Project.fromList(projects));
        });

        // GET api.serp.se/v1/account/self HTTP/1.1
        GET("/self", (rc) -> {
            AccountSystem.Account user = AccountSystem.findByEmail(rc.getSession("email"));

            final JcNode coll = new JcNode("coll");
            final JcNode entry = new JcNode("entry");
            JcQueryResult res = Database.query(rc.getLocal("db"), new IClause[]{
                MATCH.node().label("user").property("email").value(user.email)
                    .relation().type("MEMBER_OF").node(coll).label("collection"),
                OPTIONAL_MATCH.node(coll).relation().type("CONTAINS")
                    .node(entry).label("entry"),
                // For some reason, it's incredibly hard to write:
                // RETURN collect(DISTINCT coll), collect(DISTINCT entry)
                RETURN.DISTINCT().value(coll),
                RETURN.value(entry)
            });

            final List<GrNode> groups = res.resultOf(coll);
            final List<GrNode> entries = res.resultOf(entry);

            rc.status(200).json().send(new UserDetails(user, groups, entries));
        });

        // GET api.serp.se/v1/account/logout HTTP/1.1
        POST("/logout", (rc) -> {
            rc.resetSession();
            rc.getResponse().ok();
        });

        // POST api.serp.se/v1/account/delete HTTP/1.1
        POST("/delete", (rc) -> {
            new DeleteAccountEvent(rc.getSession("email")).execute();
            rc.resetSession();
            rc.getResponse().ok();
        });

        // POST api.serp.se/v1/account/change-password HTTP/1.1
        // old=..&new=..
        POST("/change-password", (rc) -> {
            String oldpw = rc.getParameter("old").toString();
            String newpw = rc.getParameter("new").toString();

            if (!AccountSystem.authenticate(rc.getSession("email"), oldpw))
                throw new RequestException("Invalid password.");

            AccountSystem.changePassword(rc.getSession("email"), newpw);
            rc.getResponse().ok();
        });

        // GET /account/invites?project=xyz
        // --> [ {collection}, ..., {collection} ]
        GET("/invites", (rc) -> {
            final String email = rc.getSession("email");
            if (rc.getParameter("project").isEmpty())
                throw new RequestException("Must include 'project' parameter.");
            final String project = rc.getParameter("project").toString();
            final List<GrNode> collections = queryCollections(rc.getLocal("db"), email, "INVITE", project);
            rc.json().send(Graph.Collection.fromList(collections));
        });

        // GET api.serp.se/v1/account/some@email.com HTTP/1.1
        GET("/{email}", (rc) -> {
            AccountSystem.Account user = AccountSystem.findByEmail(rc.getSession("email"));
            if (!TrustLevel.authorize(user.trust, TrustLevel.USER))
                throw new RequestException(403, "Please verify your account email.");

            String email = rc.getParameter("email").toString();
            if (email == null || email.isEmpty())
                throw new RequestException("No such email in database");

            AccountSystem.Account query = AccountSystem.findByEmail(email);
            if (query == null)
                throw new RequestException("No such email in database");

            final List<GrNode> groups = queryCollections(rc.getLocal("db"), email, "MEMBER_OF");
            rc.status(200).json().send(new UserDetails(query, groups));
        });


        // --
    }

    private static class UserDetails {
        public String email, trust;
        public Graph.Node[] entries;
        public Graph.Collection[] collections;

        public UserDetails(AccountSystem.Account user, List<GrNode> coll) {
            this.email = user.email;
            this.trust = TrustLevel.toString(user.trust);
            this.collections = Graph.Collection.fromList(coll);
        }

        public UserDetails(AccountSystem.Account user, List<GrNode> coll, List<GrNode> entries) {
            this(user, coll);
            this.entries = Graph.Node.fromList(entries);
        }
    }

    @Override
	public String getPrefix() { return "/v1/account"; }
}
