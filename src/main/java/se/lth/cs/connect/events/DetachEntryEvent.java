package se.lth.cs.connect.events;

import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.NATIVE;
import iot.jcypher.query.factories.clause.WHERE;
import iot.jcypher.query.factories.clause.WITH;
import iot.jcypher.query.values.JcBoolean;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcRelation;
import se.lth.cs.connect.modules.Database;

public class DetachEntryEvent implements UserEvent {
    private long cid, eid;

    public DetachEntryEvent(long cid, long eid) {
        this.cid = cid;
        this.eid = eid;
    }

    @Override
	public void execute() {
        boolean isOrphan = detach();
        if (isOrphan)
            new DeleteEntryEvent(eid).execute();
    }

     private boolean detach() {
        JcNode collection = new JcNode("c");
        JcNode entry = new JcNode("e");
        JcRelation rel = new JcRelation("m");
        JcBoolean orphan = new JcBoolean("o");

        Database.query(Database.access(), new IClause[]{
            MATCH.node(collection).label("collection")
                .relation(rel).node(entry).label("entry"),
            WHERE.valueOf(entry.id()).EQUALS(eid)
                .AND().valueOf(collection.id()).EQUALS(cid),
            DO.DELETE(rel)
        });

        JcQueryResult res = Database.query(Database.access(), new IClause[]{
            MATCH.node(entry).label("entry")
                .relation()
                .node().label("collection"),
            WHERE.valueOf(entry.id()).EQUALS(eid),
            NATIVE.cypher("RETURN false AS o")
        });
        return res.resultOf(orphan).size() == 0;
    }

}