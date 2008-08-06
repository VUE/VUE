/*
 * ListRandomLayout.java
 *
 * Created on August 6, 2008, 1:37 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2008
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.dataset;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;

public class ListRandomLayout extends Layout {
    
    /** Creates a new instance of ListRandomLayout */
    public ListRandomLayout() {
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
   
            node1.setLocation(MAP_SIZE*Math.random(),MAP_SIZE*Math.random());
            node2.setLocation(MAP_SIZE*Math.random(),MAP_SIZE*Math.random());
        }
        return map;
    }
}
