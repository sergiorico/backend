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
import utils.CleanupUsers;

/**
 * Default addr and neo4j credentials are read from conf/application.properties
 */
public class Connect extends Application {

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

		final String[] allowedOrigins = new String[] { "http://localhost:8181", "http://localhost:8080",
				"https://localhost:8181", "http://serpconnect.cs.lth.se", "http://api.serpconnect.cs.lth.se", "https://serpconnect.cs.lth.se",
				"https://api.serpconnect.cs.lth.se" };

		ALL(".*", (rc) -> {
			String origin = rc.getHeader("Origin");
			boolean originOk = false;

			for (String allowed : allowedOrigins) {
				if (allowed.equals(origin)) {
					originOk = true;
					break;
				}
			}

			if (!originOk && origin != null)
				throw new RequestException("CORS for this origin is not allowed");

			if (origin != null) {
				rc.setHeader("Access-Control-Allow-Origin", origin);
				rc.setHeader("Access-Control-Allow-Credentials", "true");
				rc.setHeader("Access-Control-Allow-Headers", "*");
			}
			rc.next();
		});

		getRouter().addRoute(new Route("OPTIONS", ".*", (rc) -> {
			rc.setHeader("Access-Control-Allow-Methods", "PUT, POST, OPTIONS");
			rc.setHeader("Access-Control-Allow-Headers", "Content-Type");
			rc.setHeader("Access-Control-Max-Age", "86400");
			rc.getResponse().ok();
		}));

		use("/v1/admin", new Admin(this));
		use("/v1/entry", new Entry(this));
		use("/v1/account", new Account(this));
		use("/v1/account", new Collection(this));

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

	// For now, ignore the 'prefix' b/c it's hardcoded in each module (as
	// PREFIX)
	private void use(String prefix, Router source) {
		Router target = getRouter();

		// RouteGroup is available @ master which can set a prefix for routes
		// in a group. Will enable us to mount a router onto a specific path.
		for (Route r : source.getRoutes()) {
			target.addRoute(r);
		}
	}

	/**
	 * ENTRY POINT
	 */
	public static void main(String[] args) {
		// System.setProperty("pippo.mode", "dev");
		// System.setProperty("pippo.mode", "prod");
		// System.setProperty("pippo.mode", "test");

		Connect conn = new Connect();
		Pippo pippo = new Pippo(conn);
		pippo.start();

		CleanupUsers cl = new CleanupUsers(conn);
		cl.everyTwelveHours();
	}

}