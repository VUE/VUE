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

package tufts.vue;
/*
 * SaveNode.java
 *
 * Created on October 14, 2003, 1:01 PM
 */

/**
 *
 * @author  rsaigal
 */

import java.util.Vector;
import javax.swing.tree.*;
import javax.swing.*;
import java.io.*;
import java.util.Enumeration;

public class SaveNode {
    
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(SaveNode.class);
    
    private Resource resource;
    private Vector children;
    
    public SaveNode() {}
    
    public SaveNode(ResourceNode resourceNode) {
        setResource(resourceNode.getResource());
        if (DEBUG.Enabled) Log.debug("created for " + resource.asDebug());
        if (resource.getClientType() == Resource.FAVORITES) {
            final Enumeration e = resourceNode.children();
            final Vector v = new Vector();
            while (e.hasMoreElements()) {
                ResourceNode newResNode =(ResourceNode)e.nextElement();
                SaveNode child = new SaveNode(newResNode);
                v.add(child);
            }
            setChildren(v);
        }
    }
    
    
    public void setResource(Resource resource){
        this.resource = resource;
    }
    
    public Resource getResource(){
        return (this.resource);
    }
    
    public void setChildren(Vector children){
        this.children= children;
    }
    
    public Vector getChildren(){
        return (this.children);
    }
    
}
