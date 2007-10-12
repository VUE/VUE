/*
 * PublishDataSourceAction.java
 *
 * Created on October 12, 2007, 11:56 AM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */

package tufts.vue.action;

import tufts.vue.*;

public class PublishDataSourceAction extends VueAction   {
     public static final String LABEL = "Publish";
    /** Creates a new instance of PublishDataSourceAction */
    private edu.tufts.vue.dsm.DataSource dataSource;
    PublishDataSourceAction(edu.tufts.vue.dsm.DataSource dataSource) {
        super(dataSource.getRepositoryDisplayName());
        this.dataSource = dataSource;
    }
    public void act() {
        try {
            Publisher publisher = new Publisher(dataSource);
        } catch (Exception ex) {
            VueUtil.alert(null, ex.getMessage(), "Publish Error");
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
    
}
