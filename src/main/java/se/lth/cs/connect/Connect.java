package se.lth.cs.connect;

import ro.pippo.core.Application;
import ro.pippo.core.ExceptionHandler;
import ro.pippo.core.Pippo;
import ro.pippo.core.PippoSettings;
import ro.pippo.core.route.Route;
import ro.pippo.core.route.RouteContext;
import ro.pippo.core.route.Router;
import se.lth.cs.connect.modules.Database;
import se.lth.cs.connect.modules.MailClient;
import se.lth.cs.connect.modules.Mailman;
import se.lth.cs.connect.modules.TaxonomyDB;
import se.lth.cs.connect.routes.Account;
import se.lth.cs.connect.routes.Admin;
import se.lth.cs.connect.routes.Collection;
import se.lth.cs.connect.routes.Entry;
import se.lth.cs.connect.routes.Project;
import utils.CORS;
import utils.CleanupUsers;

/**
 * Default addr and neo4j credentials are read from conf/application.properties
 */
public class Connect extends Application {

	static final String[] CORS_ORIGINS = new String[] { 
		"http://localhost:8181", 
		"https://localhost:8181", 
		"http://localhost:8080",

		"http://serpconnect.cs.lth.se",
		"https://serpconnect.cs.lth.se",

		"https://test.serpconnect.cs.lth.se",
		
		"http://api.serpconnect.cs.lth.se", 
		"https://api.serpconnect.cs.lth.se"
	};

	private MailClient mailClient;

	public MailClient getMailClient() { return mailClient; }

	public void useMailClient(MailClient client) {
		mailClient = client;
		client.configure(getPippoSettings());
	}

	@Override
	protected void onInit() {
		PippoSettings conf = getPippoSettings();

		Database.configure(conf);
		TaxonomyDB.configure(conf);

		// Use the ordinary mailman by default
		useMailClient(new Mailman());

		// Specify which Origin headers that may access the site
		ANY(".*", new CORS(CORS_ORIGINS));

		// Specify which http methods CORS requests can use
		getRouter().addRoute(new Route("OPTIONS", ".*", (rc) -> {
			rc.setHeader("Access-Control-Allow-Methods", "PUT, POST, OPTIONS");
			rc.setHeader("Access-Control-Allow-Headers", "Content-Type");
			rc.setHeader("Access-Control-Max-Age", "86400");
			rc.getResponse().ok();
		}));

		// Register all routes
		use(new Admin(this));
		use(new Entry(this));
		use(new Account(this));
		use(new Collection(this));
		use(new Project(this));

		getErrorHandler().setExceptionHandler(RequestException.class, new ExceptionHandler() {
			@Override
			public void handle(Exception e, RouteContext rc) {
				if (e instanceof RequestException)
					rc.status(((RequestException) e).getStatus());
				else
					rc.status(500);
				rc.text().send(e.getMessage());
			}
		});
	}

	/* Dump routes from source into app router */
	private void use(Router source) {
		Router target = getRouter();

		for (Route r : source.getRoutes()) {
			target.addRoute(r);
		}
	}

	/**
	 * ENTRY POINT
	 */
	public static void main(String[] args) {
		Connect conn = new Connect();
		Pippo pippo = new Pippo(conn);
		pippo.start();

		Bootstrap.runFirstTimeCheck();

		CleanupUsers cl = new CleanupUsers(conn);
		cl.everyTwelveHours();
	}

}