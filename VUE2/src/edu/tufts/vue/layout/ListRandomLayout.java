/*
 * ListRandomLayout.java
 *
 * Created on August 6, 2008, 1:37 PM
 *
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
package edu.tufts.vue.layout;


import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;
import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.dataset.*;


public class ListRandomLayout extends Layout {
    public static String DEFAULT_METADATA_LABEL = "default";
    /** Creates a new instance of ListRandomLayout */
    public ListRandomLayout() {
    }
    
    
    public LWMap createMap(Dataset ds,String mapName) throws Exception{
        Map<String,LWNode> nodeMap = new HashMap<String,LWNode>();
        Map<String,Integer> repeatMap = new HashMap<String,Integer>();
        LWMap map = new LWMap(mapName);
        int count = 0;
        // set map size of the map
       double rowCount = ds.getRowList().size();
        double goodSize =  (int)Math.sqrt(rowCount)*100;
        MAP_SIZE = MAP_SIZE>goodSize?MAP_SIZE:goodSize;
        
        
        for(ArrayList<String> row: ds.getRowList()) {
            String node1Label = row.get(0);
            LWNode node1;
            LWNode node2;
            if(!nodeMap.containsKey(node1Label)) {
                node1 = new LWNode(node1Label);
                // add the ont type
                VueMetadataElement vmOnt = new VueMetadataElement();
                vmOnt.setValue(ds.getBaseClass());
                vmOnt.setType(VueMetadataElement.ONTO_TYPE);
                node1.getMetadataList().addElement(vmOnt);
                
                for(int i=1;i<row.size();i++ ) {
                    String value = row.get(i);
                    String key = ((ds.getHeading()==null) || ds.getHeading().size() <i)?DEFAULT_METADATA_LABEL:ds.getHeading().get(i);
                    //                   System.out.println("i="+i+" key="+key+" value ="+value);
                    
                    VueMetadataElement vm = new VueMetadataElement();
                    vm.setKey(key);
                    vm.setValue(value);
                    vm.setType(VueMetadataElement.CATEGORY);
                    node1.getMetadataList().addElement(vm);
                }
                
                nodeMap.put(node1Label,node1);
                node1.layout();
                map.add(node1);
            } else {
                node1 = nodeMap.get(node1Label);
            }
            
            node1.setLocation(MAP_SIZE*Math.random(),MAP_SIZE*Math.random());
            
        }
        return map;
    }
    
    /** It takes a map and scatters the nodes randomly
     * @param map the map for layout
     */
    public void layout(LWMap map) {
        Iterator<LWNode> nodeIterator =  map.getChildIterator();
        while(nodeIterator.hasNext()) {
            LWNode node = nodeIterator.next();
            node.setLocation(MAP_SIZE*Math.random(),MAP_SIZE*Math.random());
        }
    }
}
