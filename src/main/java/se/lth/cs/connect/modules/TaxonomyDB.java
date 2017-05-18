package se.lth.cs.connect.modules;

import se.lth.cs.connect.RequestException;

import ro.pippo.core.PippoSettings;
import ro.pippo.core.route.RouteContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.nio.charset.Charset;


/**
* Update taxonomy.
*/
public abstract class TaxonomyDB {
    public abstract void configure(PippoSettings props);
    public abstract void read(int collectionId, RouteContext rc);
    public abstract void write(int collectionId, String serialized);

    public abstract void init(int collectionId);

    public static class FileTaxonomy extends TaxonomyDB {
        String dbPath;
        String baseTaxonomy;

        public void configure(PippoSettings props) {
            dbPath = props.getString("connect.taxonomy.txdb", "~/.connect/txdb");
        }

        public String getPath(int collectionId) {
            return dbPath + "/" + "c-" + collectionId + ".json";
        }

        @Override
        public void init(int collectionId) {
            write(collectionId, baseTaxonomy);
        }
        
        @Override
        public void read(int collectionId, RouteContext rc) {
            final String txPath = getPath(collectionId);
            rc.send(new File(txPath));
        }
        
        @Override
        public void write(int collectionId, String serialized) {
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
}