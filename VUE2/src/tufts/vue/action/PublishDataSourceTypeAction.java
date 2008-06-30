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

import java.util.*;

import tufts.vue.*;

public class PublishDataSourceTypeAction extends VueAction{
    private static  Map<org.osid.shared.Type,String> typeLabelMap;
    org.osid.shared.Type type;
    PublishDataSourceTypeAction(org.osid.shared.Type type) {
        super(getTypeLabelMap().get(type));
        this.type = type;
    }
    
    public void act() {
        try {
            Publisher publisher = new Publisher(type);
        } catch (Exception ex) {
            VueUtil.alert(null, ex.getMessage(), "Publish Error");
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
    
    private static Map<org.osid.shared.Type,String> getTypeLabelMap() {
        if(typeLabelMap == null) {
            typeLabelMap = new HashMap<org.osid.shared.Type,String> ();
            typeLabelMap.put(edu.tufts.vue.dsm.DataSourceTypes.FEDORA_REPOSITORY_TYPE,"Fedora 2.2");
            typeLabelMap.put(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE,"Sakai");
        }
        return typeLabelMap;
    }
}
