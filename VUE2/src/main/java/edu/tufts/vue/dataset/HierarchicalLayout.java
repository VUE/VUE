/*
 * Hierarchical.java
 *
 * Created on July 7, 2008, 3:07 PM
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
package edu.tufts.vue.dataset;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import javax.swing.*;
import tufts.vue.*;

public class HierarchicalLayout extends AbstractLayout{
 
    public static final String LABEL = "Hierarchical Layout";
    
    /** Creates a new instance of Hierarchical */
    public HierarchicalLayout() {
        super(LABEL);
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
                repeatMap.put(node1Label, new Integer(1));
                count++;
                node1.setLocation(MAP_SIZE/2,count*40);
                map.add(node1);
            } else {
                node1 = nodeMap.get(node1Label);
                int nc= repeatMap.get(node1Label).intValue();
                repeatMap.put(node1Label,new Integer(nc+1));
                Point2D p = node1.getLocation();
                node1.setLocation(p.getX()-40,p.getY());
            }
            if(!nodeMap.containsKey(node2Label)) {
                node2 = new LWNode(node2Label);
                repeatMap.put(node2Label, new Integer(1));
                nodeMap.put(node2Label,node2);
                count++;
                node2.setLocation(MAP_SIZE/2,count*40+20);
                map.add(node2);
            } else {
                node2 = nodeMap.get(node2Label);
                int nc= repeatMap.get(node2Label).intValue();
                Point2D p = node2.getLocation();
                node2.setLocation(p.getX()-40,p.getY());
                repeatMap.put(node2Label,new Integer(nc+1));
            }
            LWLink link = new LWLink(node1,node2);
            map.add(link);
        }
        return map;
    }
}
