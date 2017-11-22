package utils;

import ro.pippo.core.route.RouteHandler;
import ro.pippo.core.route.RouteContext;
import se.lth.cs.connect.RequestException;

public class CORS<T extends RouteContext> implements RouteHandler<T> {
    final String[] allowedOrigins;

    public CORS(final String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public void handle(T rc) {
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
    }
}