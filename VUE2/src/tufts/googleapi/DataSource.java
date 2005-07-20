/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */
/*
 * DataSource.java
 *
 * Created on April 4, 2005, 11:45 AM
 */

package  tufts.googleapi;

/**
 *
 * @author  akumar03
 */

import tufts.vue.DataSourceException;

public class  DataSource extends tufts.vue.VueDataSource{
    String key;
    /** Creates a new instance of DataSource */
    public DataSource() {
    }
    
    public DataSource(String displayName,String key) throws tufts.vue.DataSourceException {
        super.setDisplayName(displayName);
        this.key = key;
        setResourceViewer();
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    public String getKey(){
        return key;
    }
    public void  setResourceViewer() throws DataSourceException{
        
        try{
            this.resourceViewer = new tufts.googleapi.ResourceViewer(this.key);
        }catch (Exception ex){
            throw new DataSourceException("Googleapi.setResourceViewer "+ex);
        }
    }
    
}
