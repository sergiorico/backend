package se.lth.cs.connect.events;
import java.math.BigDecimal;

import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.OPTIONAL_MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcNumber;
import se.lth.cs.connect.modules.Database;

public final class DeleteAccountEvent implements UserEvent {
    private final String email;

    public DeleteAccountEvent(String email) {
        this.email = email;
    }

    @Override
	public void execute() {
        leaveAllCollections();
        deleteAccount();
    }

    private void leaveAllCollections() {
        JcNode collection = new JcNode("c");
        JcNumber collectionId = new JcNumber("x");

        JcQueryResult res = Database.query(Database.access(), new IClause[]{
            MATCH.node().label("user").property("email").value(email)
                .relation().type("MEMBER_OF").node(collection).label("collection"),
            RETURN.value(collection.id()).AS(collectionId)
        });

        for (BigDecimal bi : res.resultOf(collectionId))
            new LeaveCollectionEvent(bi.longValue(), email).execute();
    }

    /**
     * Delete account and associated tokens.
     */
    private void deleteAccount() {
        JcNode self = new JcNode("u");
        JcNode token = new JcNode("t");

        Database.query(Database.access(), new IClause[]{
            MATCH.node(self).label("user").property("email").value(email),
            OPTIONAL_MATCH.node(self).relation().node(token).label("token"),
            DO.DETACH_DELETE(self),
            DO.DETACH_DELETE(token)
        });
    }
}