package se.lth.cs.connect.modules;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.CREATE;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.factories.clause.WHERE;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcNumber;
import ro.pippo.core.PippoSettings;
import ro.pippo.core.route.RouteContext;
import se.lth.cs.connect.RequestException;


/**
* Update taxonomy.
*/
public class TaxonomyDB {
	static String dbPath;
	static String baseTaxonomy;
    
    public static void configure(PippoSettings props) {
        dbPath = props.getString("connect.taxonomy.txdb", "~/.connect/txdb");
    }

    public static String getPath(int collectionId) {
        return dbPath + "/" + "c-" + collectionId + ".json";
        

    }

    private static String readTaxonomyFile(int collectionId) {
        final String txPath = getPath(collectionId);
        final File file = new File(txPath);

        try {
        	char[] data = new char[(int) file.length()];
        	final BufferedReader reader = new BufferedReader(new FileReader(file));
        	reader.read(data);
        	reader.close();
        	return String.valueOf(data);
        } catch (IOException e) {
            throw new RequestException("Error writing taxonomy to file");
        }
    }
    
    public static void init(int collectionId) {
    	write(collectionId, "[]");
    }
    
    public static class TaxonomySnapshot {
    	public long version;
    	public String taxonomy;
    	
    	public TaxonomySnapshot(long v, String t) {
    		this.version = v;
    		this.taxonomy = t;
    	}
    }
    
    public static long version(int collectionId) {
    	JcNode collection = new JcNode("c");
        JcNode taxonomy = new JcNode("t");
    	JcNumber version = new JcNumber("v");
    	
    	JcQueryResult res = Database.query(Database.access(), new IClause[]{
    		MATCH.node(collection).label("collection")
    			.relation().type("TAXONOMY")
    			.node(taxonomy),
    		WHERE.valueOf(collection.id()).EQUALS(collectionId),
    		RETURN.value(taxonomy.property("version")).AS(version)
    	});
    	
    	
    	return res.resultOf(version).get(0).longValue();
    }
    
    public static void increment(int collectionId) {
    	JcNode collection = new JcNode("c");
        JcNode taxonomy = new JcNode("t");
    	
    	Database.query(Database.access(), new IClause[]{
    		MATCH.node(collection).label("collection")
    			.relation().type("TAXONOMY")
    			.node(taxonomy),
    		WHERE.valueOf(collection.id()).EQUALS(collectionId),
    		DO.SET(taxonomy.property("version"))
    			.to(taxonomy.property("version").toInt().plus(1))
    	});
    }
    
    public static TaxonomySnapshot read(int collectionId) {
    	long taxonomyVersion = version(collectionId);
    	String taxonomyExt = readTaxonomyFile(collectionId);
    	
    	return new TaxonomySnapshot(taxonomyVersion, taxonomyExt);
    }
    
  
    public static void write(int collectionId, String serialized) {
        final String txPath = getPath(collectionId);
        try {
            final FileOutputStream fos = new FileOutputStream(new File(txPath));
            final BufferedOutputStream bos = new BufferedOutputStream(fos, 128*100);
            bos.write(serialized.getBytes(Charset.forName("UTF-8")));
            bos.flush();
            fos.close();
        } catch (IOException e) {
            throw new RequestException("Error writing taxonomy to file");
        }
    }

}