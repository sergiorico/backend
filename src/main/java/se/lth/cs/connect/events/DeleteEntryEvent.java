package se.lth.cs.connect.events;

import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.OPTIONAL_MATCH;
import iot.jcypher.query.factories.clause.WHERE;
import iot.jcypher.query.factories.clause.WITH;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcRelation;
import se.lth.cs.connect.modules.Database;

public class DeleteEntryEvent implements UserEvent {
    private long id;

    public DeleteEntryEvent(long id) {this.id = id;}

    @Override
	public void execute() {
        detachDelete();
    }

    private void detachDelete() {
        JcNode entry = new JcNode("e");
        JcNode facet = new JcNode("f");
        JcRelation rel = new JcRelation("m");

        Database.query(Database.access(), new IClause[]{
            OPTIONAL_MATCH.node(entry).relation().node(facet).label("facet"),
            WHERE.valueOf(entry.id()).EQUALS(id),
            DO.DETACH_DELETE(entry),
            WITH.value(facet),
            OPTIONAL_MATCH.node(facet).relation(rel).node().label("entry"),
            WHERE.valueOf(rel).IS_NULL(),
            DO.DELETE(facet)
        });
    }

}