/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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
            VueUtil.alert(null, ex.getMessage(), VueResources.getString("publishdatasourceaction.title"));
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
    
}
