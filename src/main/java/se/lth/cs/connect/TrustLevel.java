package se.lth.cs.connect;

public class TrustLevel {
    public static final int ADMIN        = 99999;
    public static final int VERIFIED     = 9999;
    public static final int USER         = 999;
    public static final int REGISTERED   = 99;
    public static final int UNREGISTERED = 9;

    /**
     * Compare access levels of a and b (compare(a,b)):
     *  >0: a has greater access (b has less access)
     *  =0: a and b have same access
     *  <0: a has less access (b has greater access)
     *
     * To check if user with access a is allowed access on resource:
     *
     *  boolean allowed = compare(a, TrustLevel.MOD) >= 0;
     *
     */
    public static int compare(int left, int right) {
        return left - right;
    }

    /**
     * Check if a user is authorized to do some action requiring ref trust level.
     */
    public static boolean authorize(int userTrust, int ref) {
        return compare(userTrust, ref) >= 0;
    }

    /**
     * Convert trust level to string representation. Unknown level = "Unknown"
     */
    public static String toString(int trustLevel) {
        switch (trustLevel) {
        case ADMIN: return "Admin";
        case VERIFIED: return "Verified";
        case USER: return "User";
        case REGISTERED: return "Registered";
        case UNREGISTERED: return "Unregistered";
        default: return "Unknown";
        }
    }

    /**
     * Convert a string representation to int. Unknown string = UNREGISTERED.
     */
    public static int fromString(String repr) {
        switch (repr) {
        case "Admin": return ADMIN;
        case "Verified": return VERIFIED;
        case "User": return USER;
        case "Registered": return REGISTERED;
        case "Unregistered": return UNREGISTERED;
        default: return UNREGISTERED;
        }
    }
}