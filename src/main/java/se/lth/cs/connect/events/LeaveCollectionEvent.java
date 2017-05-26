package se.lth.cs.connect.events;

import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.NATIVE;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.factories.clause.WHERE;
import iot.jcypher.query.values.JcBoolean;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcRelation;
import iot.jcypher.query.values.JcString;
import se.lth.cs.connect.modules.Database;

public class LeaveCollectionEvent implements UserEvent {
    private final String email;
    private final long cid;

    public LeaveCollectionEvent(long cid, String email) {
        this.email = email;
        this.cid = cid;
    }

    @Override
	public void execute() {
        deletePendingInvites();
        leaveOrNuke();
    }

    private void deletePendingInvites() {
        JcNode self = new JcNode("u");
        JcNode invited = new JcNode("x");
        JcNode collection = new JcNode("c");
        JcRelation invitation = new JcRelation("i");
        JcRelation inviter = new JcRelation("r");

        // SELF <-MEMBER_OF-> COLLECTION <-INVITE-> USER <-INVITER-> SELF
        // EQUALS(cid) forces the invite to be to the specified collection
        Database.query(Database.access(), new IClause[]{
            MATCH.node(self).label("user").property("email").value(email)
                .relation().type("MEMBER_OF").node(collection).label("collection")
                .relation(invitation).type("INVITE").node(invited).label("user")
                .relation(inviter).type("INVITER").node(self),
            WHERE.valueOf(inviter.property("parentnode")).EQUALS(cid),
            DO.DELETE(invitation),
            DO.DELETE(inviter)
        });
    }

    private void leaveOrNuke() {
        JcString owner = new JcString("o");
        JcNode collection = new JcNode("c");
        JcRelation rel = new JcRelation("r");
        JcBoolean exists = new JcBoolean("e");

        JcQueryResult res = Database.query(Database.access(), new IClause[]{
            MATCH.node().label("user").property("email").value(email)
                .relation(rel).node(collection).label("collection"),
            WHERE.valueOf(collection.id()).EQUALS(cid),
            DO.DELETE(rel),
            RETURN.value(rel.type()).AS(owner),
        });


        // If user was owner, delete the collection
        for (String str : res.resultOf(owner)) {
            if (str.toLowerCase().equals("owner")) {
                new DeleteCollectionEvent(cid).execute();
                return;
            }
        }

        // Otherwise, delete collection if there are no members left
        res = Database.query(Database.access(), new IClause[]{
            MATCH.node().label("user")
                .relation(rel).node(collection).label("collection"),
            WHERE.valueOf(collection.id()).EQUALS(cid),
            NATIVE.cypher("RETURN true AS e")
        });

        boolean hasMembers = res.resultOf(exists).size() > 0;
        if (!hasMembers)
            new DeleteCollectionEvent(cid).execute();
    }
}