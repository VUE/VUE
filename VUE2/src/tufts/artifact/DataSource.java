/*
 * DataSource.java
 *
 * Created on August 13, 2004, 11:05 AM
 */

package   tufts.artifact;

/**
 *
 * @author  akumar03
 */

import tufts.vue.DataSourceException;

public class DataSource extends tufts.vue.VueDataSource {
    
    /** Creates a new instance of DataSource */
    public DataSource() {
    }
    
    public DataSource(String displayName) throws tufts.vue.DataSourceException {
        super.setDisplayName(displayName);
        setResourceViewer();
    }
    
    public void  setResourceViewer() throws DataSourceException{
        
        try{
              this.resourceViewer = new tufts.artifact.ResourceViewer();
        }catch (Exception ex){
            throw new DataSourceException("FedoraDataSource.setResourceViewer "+ex);
        }
    }
    
}
