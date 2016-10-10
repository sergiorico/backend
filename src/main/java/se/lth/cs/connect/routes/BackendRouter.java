package se.lth.cs.connect.routes;

import iot.jcypher.database.IDBAccess;
import ro.pippo.core.PippoSettings;
import ro.pippo.core.route.DefaultRouter;
import ro.pippo.core.route.Route;
import ro.pippo.core.route.RouteHandler;
import se.lth.cs.connect.Connect;
import se.lth.cs.connect.modules.Database;

/**
 * This router provides a db access instance to route handlers:
 *
 *		rc.getLocal("db")
 *
 * Because of this, your own routes must be set up in
 *
 *		setup(PippoSettings conf) {}
 *
 * Some helper methods are also included: ALL, GET, POST
 */
public class BackendRouter extends DefaultRouter {
    protected Connect app;

    public BackendRouter(Connect app) {
        this.app = app;

        // All requests will need a database connection
        ALL(".*", (rc) -> {
            rc.setLocal("db", Database.access());
            rc.next();
        });

        setup(app.getPippoSettings());

        // Make sure to close connection, even if request failed/throwed
        ALL(".*", (rc) -> {
            IDBAccess conn = rc.removeLocal("db");
            if (conn != null)
                conn.close();
        }).runAsFinally();
    }

    /**
     * Get router prefix.
     */
    public String getPrefix() { return ""; }

    /**
     * Setup routes here.
     */
    protected void setup(PippoSettings conf) {}

    /**
     * Add route and return it, allowing for chained method calls.
     * Cannot @Override addRoute because it has return type 'void'.
     */
    protected Route register(Route r) {
        super.addRoute(r);
        return r;
    }

    protected Route ALL(String uri, RouteHandler handler){
        return register(Route.ALL(getPrefix() + uri, handler));
    }

    protected Route GET(String uri, RouteHandler handler){
        return register(Route.GET(getPrefix() + uri, handler));
    }

    protected Route POST(String uri, RouteHandler handler){
        return register(Route.POST(getPrefix() + uri, handler));
    }

    protected Route PUT(String uri, RouteHandler handler){
        return register(Route.PUT(getPrefix() + uri, handler));
    }

    protected Route OPTIONS(String uri, RouteHandler handler){
        return register(new Route("OPTIONS", getPrefix() + uri, handler));
    }
}