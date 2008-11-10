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
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;
import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.dataset.*;

/* ListRandomLaout - This layout scatters all the nodes at random in a square
 * by default the area of the square is propotional to the number of nodes in 
 * selection. 
 */

public class ListRandomLayout extends Layout {
    public static String DEFAULT_METADATA_LABEL = "default";
    double nodeSize = 100; // assume the node size to be 100. this can be calculated or set
        
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
            if(!nodeMap.containsKey(node1Label)) {
                node1 = new LWNode(node1Label);
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
                if(ds.getHeading().size()>1 && ds.getHeading().get(1).equals("resource")) {
                    Resource resource = node1.getResourceFactory().get(new File(row.get(1)));
                    node1.setResource(resource);
                }
                // special hack to demo the dataset laurie baise dataset
                if(ds.getHeading().size()>6 && ds.getHeading().get(6).equals("Actual")) {
                    if(row.get(6).equalsIgnoreCase("A")) {
                        node1.setFillColor(Color.CYAN);
                    }
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
    
    public void layout(java.util.List<LWNode> nodeList) {
        for(LWNode node: nodeList) {
            node.setLocation(MAP_SIZE*Math.random(),MAP_SIZE*Math.random());
        }
    }
    
    public void layout(LWSelection selection) {
        // determine the left corner of the selection and and use that as the 
        // center for layout.  We assume the selection is a rectangle. 
        // also compute the total number of nodes in the selection
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        int total = 0;
        Iterator<LWComponent> i = selection.iterator(); 
        while (i.hasNext()) {
            LWComponent c = i.next();
            if(c instanceof LWNode) {
                LWNode node = (LWNode)c;
                minX = node.getLocation().getX()<minX?node.getLocation().getX():minX;
                minY =node.getLocation().getY()<minY?node.getLocation().getY():minY;
                total++;
             }
        }
        double x = minX;
        double y = minY;
        double size = Math.sqrt(total)* nodeSize;
        i = selection.iterator();
         while (i.hasNext()) {
            LWComponent c = i.next();
            if(c instanceof LWNode) {
                LWNode node = (LWNode)c;
                node.setLocation(minX+size*(Math.random()-0.5),minY+size*(Math.random()-0.5)); 
            }
        }
    }
}
