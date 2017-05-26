package se.lth.cs.connect.modules;

import java.util.List;
import java.util.Properties;

import iot.jcypher.database.DBAccessFactory;
import iot.jcypher.database.DBProperties;
import iot.jcypher.database.DBType;
// required for accessing the database
import iot.jcypher.database.IDBAccess;
// required for executing queries and handling errors
import iot.jcypher.query.JcQuery;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.result.JcError;
import ro.pippo.core.PippoSettings;
import se.lth.cs.connect.DatabaseException;

public class Database {
    private static String USERNAME, PASSWORD, HOST;

    /**
     * Helper class for parsing Cypher errors.
     */
    public static class Error {
        private StringBuilder sb;

        public Error() {
            sb = new StringBuilder();
        }

        @Override
		public String toString() {
            return sb.toString();
        }

        /**
         * Add both general and db errors to a new Error object.
         */
        public static Error fromResult(JcQueryResult res) {
            Error err = new Error();

            err.sb.append("General Errors:\n\n");
            try {
                appendErrors(err.sb, res.getGeneralErrors());
            } catch (Exception e) {
                err.sb.append("Couldn't parse general errors.");
            }

            err.sb.append("\n\nDB Errors:\n\n");
            try {
                appendErrors(err.sb, res.getDBErrors());
            } catch (Exception e) {
                err.sb.append("Couldn't parse db errors.\n");
            }
            return err;
        }

        /**
         * Append the error list to the given string builder.
         */
        public static void appendErrors(StringBuilder sb, List<JcError> errors) {
            int num = errors.size();
            for (int i = 0; i < num; i++) {
                JcError err = errors.get(i);
                if (i > 0)
                    sb.append("\n\n");

                sb.append(err.getCodeOrType());
                sb.append(": ");
                sb.append(err.getMessage());
            }
        }
    }

    public static void configure(PippoSettings props) {
        USERNAME = props.getString("neo4j.username", "neo4j");
        PASSWORD = props.getString("neo4j.password", "neo4j");
        HOST = props.getString("neo4j.host", "http://localhost:7474");
    }

    public static IDBAccess access(){
        Properties props = new Properties();
        props.setProperty(DBProperties.SERVER_ROOT_URI, HOST);

        return DBAccessFactory.createDBAccess(DBType.REMOTE, props, USERNAME, PASSWORD);
    }

    public static JcQueryResult query(IDBAccess access, IClause[] clause) {
        JcQuery query = new JcQuery();
        query.setClauses(clause);

        JcQueryResult res = access.execute(query);

        if (res.hasErrors())
            throw new DatabaseException(res);
        return res;
    }
}