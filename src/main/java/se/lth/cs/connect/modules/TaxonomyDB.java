package se.lth.cs.connect.modules;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.factories.clause.WHERE;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcNumber;
import ro.pippo.core.PippoSettings;
import se.lth.cs.connect.RequestException;


/**
* Update taxonomy.
*/
public class TaxonomyDB {
    static String projectsPath;
    static String collectionsPath;

    public static class Facet {
    	public String name, id, parent, desc;
    }

    public static class Taxonomy {
    	public List<Facet> taxonomy;
    	public long version;

    	public Taxonomy() {
    		version = 0;
    		taxonomy = new ArrayList<Facet>();
    	}
    }

    /*
     * /txdb
     *     /projects
     *              /<id>.json
     *     /collections
     *              /<id>.json
     */

    public static void configure(PippoSettings props) {
        String dbPath = props.getString("connect.taxonomy.txdb", "./txdb");

        projectsPath = dbPath + "/projects";
        collectionsPath = dbPath + "/collections";
    }

    /**
     * Since we are reading from the file system, stop any path haxxing...
     */
    private static boolean checkPath(String name) {
        if (name.contains(".")) return false;
        if (name.contains("/")) return false;
        return true;
    }

    public static String collection(long collectionId) {
        return collectionsPath + "/" + "c-" + collectionId + ".json";
    }

    public static String project(String pname) {
        if (!checkPath(pname))
            throw new RequestException(500, "Invalid project name");
        return projectsPath + "/" + pname + ".json";
    }

    public static Taxonomy taxonomyOf(String path) {
    	try {
    		return (new ObjectMapper()).readValue(read(path), Taxonomy.class);
    	} catch (Exception e) {
    		System.err.println("TaxonomyDB.taxonomyOf: " + e.getMessage());
    		return null;
    	}
    }

    public static void write(String txPath, String serialized) {
        try {
            final FileOutputStream fos = new FileOutputStream(new File(txPath));
            final BufferedOutputStream bos = new BufferedOutputStream(fos, 128*100);
            bos.write(serialized.getBytes(Charset.forName("UTF-8")));
            bos.flush();
            fos.close();
        } catch (IOException e) {
            throw new RequestException("Error writing taxonomy to file: " + e.getMessage());
        }
    }

    public static void update(String path, Taxonomy taxonomy) throws JsonProcessingException {
    	ObjectMapper mapper = new ObjectMapper();
    	write(path, mapper.writeValueAsString(taxonomy));
    }

    public static String read(String path) {
        final File file = new File(path);
    	try {
        	char[] data = new char[(int) file.length()];
        	final BufferedReader reader = new BufferedReader(new FileReader(file));
        	reader.read(data);
        	reader.close();
        	return String.valueOf(data);
        } catch (IOException e) {
            // TODO: Detect type of IO exception: enoent, other
            throw new RequestException(404, "No such taxonomy.");
        }
    }
}