package se.lth.cs.connect.modules;

import java.security.SecureRandom;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import com.lambdaworks.crypto.SCryptUtil;

import iot.jcypher.graph.GrNode;
import iot.jcypher.graph.GrProperty;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.CREATE;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcString;
import se.lth.cs.connect.TrustLevel;

/**
 * An account must have a unique email.
 * Trust is elevated from 0-->1 (verify email) and 1-->2 (admin promotes).
 * Passwords are encrypted with scrypt.
 */
public class AccountSystem {
    // 2^14/8/1 ~ 100ms
    private static final int SCRYPT_N = 16384;
    private static final int SCRYPT_R = 8;
    private static final int SCRYPT_P = 1;

    // Must be reseeded from time to time
    private static final SecureRandom random = new SecureRandom();

    /**
     * POJO representation of an account. Changes to the variables will not be
     * reflected in the database, use the methods "changeXYZ" further down.
     */
    public static class Account {
        public String email;
        public String password; // bcrypt/scrypt hashed
        public int trust; // See TrustLevel
        public int defaultCollection;

        public Account(String email, String pwd, int trust) {
            this.email = email;
            this.password = pwd;
            this.trust = trust;
        }

        public Account(GrNode base) {
            for (GrProperty prop : base.getProperties()) {
                switch (prop.getName()) {
                case "email": email = prop.getValue().toString(); break;
                case "password": password = prop.getValue().toString(); break;
                case "trust":
                    java.math.BigDecimal a = (java.math.BigDecimal)prop.getValue();
                    trust = a.intValue();
                    break;
                case "default":
                    java.math.BigDecimal b = (java.math.BigDecimal)prop.getValue();
                    defaultCollection = b.intValue();
                default: break;
                }
            }
        }

        public boolean authenticate(String email, String password) {
            // important to fail on email check first, otherwise very slow
            return email != null && password != null &&
                this.email.equals(email) &&
                SCryptUtil.check(password, this.password);
        }
    }

    /**
     * Get Account by email, or null if email doesn't exist. Use this method to
     * get trust level and username. Throws DatabaseException if query fails.
     */
    public static Account findByEmail(String email) {
        JcNode node = new JcNode("user");
        JcQueryResult res = Database.query(Database.access(), new IClause[]{
            MATCH.node(node).label("user").property("email").value(email),
            RETURN.value(node)
        });

        List<GrNode> entries = res.resultOf(node);

        // This is almost undefined behavior
        if (entries.size() > 1) {}

        if (entries.size() == 0)
            return null;

        // "Invalid username or password"
        return new Account(entries.get(0));
    }

    /**
     * Try to logon email with plaintextPassword. Returns true on success (email
     * exists and password was correct) and false on any type of failure (email
     * doesn't exist, password was wrong). Throws database exceptions on error.
     */
    public static boolean authenticate(String email, String plaintextPassword) {
        Account acc = findByEmail(email);

        // "Invalid email or password"
        if (acc == null)
            return false;

        return acc.authenticate(email, plaintextPassword);
    }

    /**
     * Try and create a user with the provided details. Email uniqueness is
     * enforced and if email is already in database, false will be returned.
     *
     * A database exception is thrown if the create query returns with errors.
     *
     * Synchronized due to uniqueness constraint not enabled in the community
     * edition, and email addresses must be unique.
     */
    public static synchronized boolean createAccount(String email,
                                        String password, int trust) {
    	Account acc = findByEmail(email);

    	JcNode coll = new JcNode("c");
    	JcNode user = new JcNode("user");

    	ZonedDateTime currentTime = ZonedDateTime.now(ZoneOffset.UTC);

        // "Email already exists"
        if (acc != null){
        	//email is already registered
        	if(acc.trust!=TrustLevel.UNREGISTERED)
        		return false;

        	//email isn't registered, merge existing mail with the new registration info
        	acc.password =  SCryptUtil.scrypt(password, SCRYPT_N, SCRYPT_R, SCRYPT_P);

        	Database.query(Database.access(), new IClause[]{
				MATCH.node(user).label("user")
					.property("email").value(email),
					DO.SET(user.property("trust")).to(trust),
					DO.SET(user.property("password")).to(acc.password),
					DO.SET(user.property("signupdate")).to(currentTime)
			});
        	return true;
        }

        // Unless synchronized, email may or may not longer be unique
        acc = new Account(email,
            SCryptUtil.scrypt(password, SCRYPT_N, SCRYPT_R, SCRYPT_P), trust);

        // (u)-[:MEMBER_OF]->(c)-[:OWNER]->(u)
        Database.query(Database.access(), new IClause[]{
            CREATE.node(coll).label("collection")
                .property("name").value("default"),
            CREATE.node(user).label("user")
                .property("email").value(email)
                .property("password").value(acc.password)
                .property("trust").value(trust)
                .property("signupdate").value(currentTime)
                .property("default").value(coll.id())
                .relation().out().type("MEMBER_OF")
                .node(coll)
                .relation().out().type("OWNER").node(user)
        });
        return true;
    }

    /**
     * Generate 32 bytes of random data and encode it with base64.
     */
    private static String generateToken() {
        byte[] entropy = new byte[32];
        random.nextBytes(entropy);
        return java.util.Base64.getEncoder().encodeToString(entropy);
    }

    /**
     * Attempt to verify a token, removing it if possible and returning the
     * related email address.
     */
    private static String verifyToken(String tokenType, String tokenStr) {
        JcNode user = new JcNode("user");
        JcNode token = new JcNode("token");
        JcString email = new JcString("email");

        JcQueryResult res = Database.query(Database.access(), new IClause[]{
            MATCH.node(token).label("token").property("token").value(tokenStr)
                .relation().type(tokenType)
                .node(user).label("user"),
            DO.DETACH_DELETE(token),
            RETURN.value(user.property("email")).AS(email)
        });

        List<String> emails = res.resultOf(email);
        // Either 0 (no token-user match) or >1 (multiple token-user matches)
        if (emails.size() != 1)
            return null;

        return emails.get(0);
    }

    /**
     * Attaches a token to the specified email. Returns token.
     */
    private static String attachToken(String tokenType, String email) {
        JcNode user = new JcNode("user");
        JcNode token = new JcNode("token");
        String tokenValue = generateToken();

        JcQueryResult res = Database.query(Database.access(), new IClause[]{
            MATCH.node(user).label("user").property("email").value(email),
            CREATE.node(token).label("token").property("token").value(tokenValue),
            CREATE.node(token).relation().out().type(tokenType).node(user)
        });

        return tokenValue;
    }

    /**
     * Generate a password reset token that points to a user and returns it.
     */
    public static String generatePasswordToken(String email) {
        return attachToken("RESET_TOKEN", email);
    }

    /**
     * Generate an email confirmation token that points to a user and returns it.
     */
    public static String generateEmailToken(String email) {
        return attachToken("EMAIL_TOKEN", email);
    }

    /**
    * Attempt to use a token value to verify an email address. Returns email
    * if token was found and deletes token, otherwise method returns null.
    */
    public static String verifyEmail(String hash) {
        String email = verifyToken("EMAIL_TOKEN", hash);

        if (email == null)
            return null;

        changeTrust(email, TrustLevel.USER);
        return email;
    }

    /**
     * Consumes the provided token and returns the email to continue
     * the reset password progress. Must check that email isn't null
     * when using this method.
     */
    public static String verifyResetPasswordToken(String hash) {
        return verifyToken("RESET_TOKEN", hash);
    }

    /**
     * Helper method to set a user property. Pass in the IClause to carried out.
     */
    private static void setUserProperty(JcNode node, String email, IClause property) {
        JcQueryResult res = Database.query(Database.access(), new IClause[]{
            MATCH.node(node).label("user").property("email").value(email),
            property
        });
    }

    /**
     * Set a user's trust level, see TrustLevel for (meaningful) values.
     */
    public static void changeTrust(String email, int trust) {
        JcNode node = new JcNode("user");
        setUserProperty(node, email, DO.SET(node.property("trust")).to(trust));
    }

    /**
     * Set a user's email address. Should be verified before making this switch.
     */
    public static void changeEmail(String current, String email) {
        JcNode node = new JcNode("user");
        setUserProperty(node, current, DO.SET(node.property("email")).to(email));
    }

    /**
     * Change password. Current password should be checked before calling this.
     */
    public static void changePassword(String email, String plaintext) {
        JcNode node = new JcNode("user");
        String hashed = SCryptUtil.scrypt(plaintext, SCRYPT_N, SCRYPT_R, SCRYPT_P);
        setUserProperty(node, email, DO.SET(node.property("password")).to(hashed));
    }

}
