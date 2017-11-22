package se.lth.cs.connect.events;

import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.WHERE;
import iot.jcypher.query.factories.xpression.X;
import iot.jcypher.query.values.JcNode;
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

        Database.query(Database.access(), new IClause[]{
            MATCH.node(entry).label("entry"),
            WHERE.valueOf(entry.id()).EQUALS(id),
            DO.DETACH_DELETE(entry)
        });

        Database.query(Database.access(), new IClause[]{
            MATCH.node(facet).label("facet"),
            WHERE.NOT().existsPattern(X.node(facet).relation().node().label("entry")),
            DO.DETACH_DELETE(facet)
        });
    }

}