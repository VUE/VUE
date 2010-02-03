/*
 * RelRandomLayout.java
 *
 * Created on August 6, 2008, 1:37 PM
 *
* Copyright 2003-2010 Tufts University  Licensed under the
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

package edu.tufts.vue.layout;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;
import edu.tufts.vue.dataset.*;

public class RelRandomLayout extends Layout {
    static String LABEL = "Random Layout";
    
    /** Creates a new instance of RelRandomLayout */
    public RelRandomLayout() {
    }
    
    public LWMap createMap(Dataset ds,String mapName) throws Exception{
        Map<String,LWNode> nodeMap = new HashMap<String,LWNode>();
        Map<String,Integer> repeatMap = new HashMap<String,Integer>();
        
        LWMap map = new LWMap(mapName);
        int count = 0;
        for(ArrayList<String> row: ds.getRowList()) {
            String node1Label = row.get(0);
            String node2Label = row.get(1);
            LWNode node1;
            LWNode node2;
            if(!nodeMap.containsKey(node1Label)) {
                node1 = new LWNode(node1Label);
                nodeMap.put(node1Label,node1);
                map.add(node1);
            } else {
                node1 = nodeMap.get(node1Label);
            }
            if(!nodeMap.containsKey(node2Label)) {
                node2 = new LWNode(node2Label);
                map.add(node2);
                nodeMap.put(node2Label,node2);
            } else {
                node2 = nodeMap.get(node2Label);
            }
            LWLink link = new LWLink(node1,node2);
            map.add(link);
            node1.setLocation(MAP_SIZE*Math.random(),MAP_SIZE*Math.random());
            node2.setLocation(MAP_SIZE*Math.random(),MAP_SIZE*Math.random());
        }
        return map;
    }
    
    public void layout(LWSelection selection) {
        Iterator<LWComponent> i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = i.next();
            if(c instanceof LWNode) {
                LWNode node = (LWNode)c;
                node.setLocation(MAP_SIZE*Math.random(),MAP_SIZE*Math.random());
            }
        }
    }
}
