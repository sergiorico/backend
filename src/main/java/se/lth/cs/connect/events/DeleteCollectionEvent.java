package se.lth.cs.connect.events;

import java.io.File;
import java.math.BigDecimal;

import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.factories.clause.WHERE;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcNumber;
import iot.jcypher.query.values.JcRelation;
import se.lth.cs.connect.modules.Database;
import se.lth.cs.connect.modules.TaxonomyDB;



public class DeleteCollectionEvent implements UserEvent {
    private long cid;

    public DeleteCollectionEvent(long id) {
        this.cid = id;
    }

    @Override
	public void execute() {
        detachMembers();
        detachEntries();
        deleteTaxonomy();
        destroy();
    }

     private void detachMembers() {
        JcNode collection = new JcNode("c");
        JcRelation rel = new JcRelation("m");

        Database.query(Database.access(), new IClause[]{
            MATCH.node(collection).label("collection")
                .relation(rel).node().label("user"),
            WHERE.valueOf(collection.id()).EQUALS(cid),
            DO.DELETE(rel)
        });
    }

    private void detachEntries() {
        JcNode entry = new JcNode("e");
        JcNode collection = new JcNode("c");
        JcNumber entryId = new JcNumber("x");

        /* A more efficient variant that only returns the entries which
           must be deleted (since they will be orphaned).

            OPTIONAL MATCH (e:entry)-[r]-(c)
            WHERE id(c) = XYZ
                AND NOT
                EXISTS((c)--(e)<-[:CONTAINS]-(:collection))
            DETACH DELETE c
            RETURN id(e)
        */
        JcQueryResult res = Database.query(Database.access(), new IClause[]{
            MATCH.node(collection).label("collection")
                .relation().node(entry).label("entry"),
            WHERE.valueOf(collection.id()).EQUALS(cid),
            RETURN.value(entry.id()).AS(entryId)
        });

        for (BigDecimal eid : res.resultOf(entryId))
            new DetachEntryEvent(cid, eid.longValue()).execute();
    }

    private void deleteTaxonomy() {
		new File(TaxonomyDB.collection(cid)).delete();
    }

    private void destroy() {
        JcNode collection = new JcNode("c");

        Database.query(Database.access(), new IClause[]{
            MATCH.node(collection).label("collection"),
            WHERE.valueOf(collection.id()).EQUALS(cid),
            DO.DETACH_DELETE(collection) /* rel to project still exists */
        });
    }
}