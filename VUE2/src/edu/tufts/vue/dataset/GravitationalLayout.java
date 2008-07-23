/*
 * Gravitational.java
 *
 * Created on July 7, 2008, 3:06 PM
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
import javax.swing.*;
import tufts.vue.*;

public class GravitationalLayout extends AbstractLayout{
    
    public static final String LABEL = "Gravitational Layout";
    
    /** Creates a new instance of Gravitational */
    public GravitationalLayout() {
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
                map.add(node1);
            } else {
                node1 = nodeMap.get(node1Label);
                int nc= repeatMap.get(node1Label).intValue();
                repeatMap.put(node1Label,new Integer(nc+1));
            }
            if(!nodeMap.containsKey(node2Label)) {
                node2 = new LWNode(node2Label);
                map.add(node2);
                repeatMap.put(node2Label, new Integer(1));
                nodeMap.put(node2Label,node2);
            } else {
                node2 = nodeMap.get(node2Label);
                int nc= repeatMap.get(node2Label).intValue();
                repeatMap.put(node2Label,new Integer(nc+1));
            }
            LWLink link = new LWLink(node1,node2);
            map.add(link);
            double angle = Math.random()*Math.PI*4;
            int nc1= repeatMap.get(node1.getLabel()).intValue();
            // nc1 = nc1*nc1;
            double fact1 = Math.sqrt(nc1);
            node1.setLocation(MAP_SIZE*(1+Math.cos(angle)/fact1),MAP_SIZE*(1+Math.sin(angle)/fact1));
            angle = Math.random()*Math.PI*4;
            int nc2= repeatMap.get(node2.getLabel()).intValue();
            //nc2=nc2*nc2;
            double fact2 = Math.sqrt(nc2);
            node2.setLocation(MAP_SIZE*(1+Math.cos(angle)/fact2),MAP_SIZE*(1+Math.sin(angle)/fact2));
            count++;
        }
        return map;
    }
}
