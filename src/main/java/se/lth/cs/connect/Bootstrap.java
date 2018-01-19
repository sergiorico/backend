package se.lth.cs.connect;

import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.CREATE;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.NATIVE;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcNumber;
import se.lth.cs.connect.modules.AccountSystem;
import se.lth.cs.connect.modules.Database;

public class Bootstrap {

    public static class Metadata {
        public int version;

        public Metadata() {
            version = 0;
        }
    }

    public static class Superuser {
        public String username, password;
        public Superuser() {
            username = "superuser";
            password = "i-eat-pancakes";
        }
    }

    public static int databaseVersion() {
        final JcNode md = new JcNode("md");
        final JcNumber version = new JcNumber("v");

        JcQueryResult res = Database.query(Database.access(), new IClause[] {
            MATCH.node(md).label("metadata"),
            RETURN.value(md.property("version")).AS(version)
        });

        if (res.resultOf(version).size() == 0)
            return -1;
        else
            return res.resultOf(version).get(0).intValue();
    }

    public static boolean isInitialized() {
        return databaseVersion() >= 0;
    }

    public static void createConstraints() {
        Database.query(Database.access(), new IClause[]{
            NATIVE.cypher("CREATE CONSTRAINT ON (p:project) ASSERT p.name IS UNIQUE")
        });
    }

    public static void createMetadata(Metadata data) {
        Database.query(Database.access(), new IClause[]{
            CREATE.node().label("metadata")
                .property("version").value(data.version)
        });
    }

    public static void createSuperuser(Superuser superuser) {
        AccountSystem.createAccount(superuser.username, superuser.password, TrustLevel.ADMIN);
    }

    public static void runFirstTimeCheck() {
        if (isInitialized())
            return;

        Metadata metadata = new Metadata();
        Superuser superuser = new Superuser();

        for (int i = 0; i < 80; i++)
            System.out.print("=");

        System.out.println();
        System.out.println("No database matadata found, bootstrapping database:");

        System.out.println();
        System.out.println(" > creating database constraints...");
        createConstraints();

        System.out.println(" > creating metadata node...");
        createMetadata(metadata);

        System.out.println(" > creating superuser (username=" + superuser.username + " password=" + superuser.password + ")...");
        createSuperuser(superuser);

        System.out.println("Done!");
    }

}