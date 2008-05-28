/*
 * Copyright 2003-2007 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package   tufts.artifact;

/**
 *
 * @author  akumar03
 */

import tufts.vue.DataSourceException;

public class DataSource extends tufts.vue.VueDataSource {
    
    /** for use during persistant restore */
    public DataSource() {}
    
    public DataSource(String displayName) throws tufts.vue.DataSourceException {
        setDisplayName(displayName);
    }
    
    @Override
    public javax.swing.JComponent buildResourceViewer() {
        return new tufts.artifact.ResourceViewer();
    }
    
}
