package se.lth.cs.connect;

import iot.jcypher.query.JcQueryResult;
import ro.pippo.core.RuntimeMode;
import se.lth.cs.connect.modules.Database;
import iot.jcypher.query.result.JcError;
import java.util.List;

/**
 * Routes that access the database may fail when accessing/querying it.
 * They should handle failures, but instead of duplicating error handling
 * code on a per-route basis, they can throw a DatabaseException.
 *
 * If an exception handler is defined, it can catch these exceptions and
 * do the right thing:
 *
 *  - http status = 500
 *  - send debug data if in DEV mode
 *  - send error message if in PROD mode
 *
 * Luckily, the default exception handler renders a template based on
 * getMessage(). If DEV mode is active, the stack trace is also included.
 */
public class DatabaseException extends RequestException {
    private String message;
    private Error error;

    public static class Error {
        public List<JcError> db, general;
        public Error(List<JcError> d, List<JcError> g) {
            db = d;
            general = g;
        }
    }

    /**
     * Construct based on a cypher result object. Contains both general and db
     * errors in DEV mode, otherwise a general error message is used.
     */
    public DatabaseException(JcQueryResult res) {
        super(500, "Unknown database error");

        message = Database.Error.fromResult(res).toString();
        error = new Error(res.getDBErrors(), res.getGeneralErrors());

        if (!RuntimeMode.getCurrent().equals(RuntimeMode.DEV)) {
            if (message.contains("already exists"))
                message = "Node or relation already exists";
            else
                message = super.getMessage();
        }
    }

    public Error errors() {
        return error;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
