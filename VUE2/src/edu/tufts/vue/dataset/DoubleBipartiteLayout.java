/*
 * DoubleBipartite.java
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
import java.awt.Color;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;

public class DoubleBipartiteLayout extends AbstractLayout{
    
    static final String LABEL = "Double Bipartite Layout";
    
    /** Creates a new instance of DoubleBipartite */
    public DoubleBipartiteLayout() {
        super(LABEL);
    }
    
    
    public LWMap createMap(Dataset ds,String mapName) throws Exception{
        Map<String,LWNode> node1Map = new HashMap<String,LWNode>();
        Map<String,LWNode> node2Map = new HashMap<String,LWNode>();
        
        LWMap map = new LWMap(mapName);
        int count = 0;
        int n1Counter = 0;
        int n2Counter = 0;
        for(ArrayList<String> row: ds.getRowList()) {
            String node1Label = row.get(0);
            String node2Label = row.get(1);
            LWNode node1;
            LWNode node2;
            if(!node1Map.containsKey(node1Label)) {
                node1 = new LWNode(node1Label);
                node1Map.put(node1Label,node1);
                node1.setLocation(MAP_SIZE/5 ,n1Counter*30);
                map.add(node1);
                n1Counter++;
            } else {
                node1 = node1Map.get(node1Label);
            }
            if(!node2Map.containsKey(node2Label)) {
                 node2 = new LWNode(node2Label);
                node2.setFillColor(Color.LIGHT_GRAY) ;
                map.add(node2);
                node2Map.put(node2Label,node2);
                node2.setLocation(MAP_SIZE/5+300 ,n2Counter*30);
                n2Counter++;
            } else {
                node2 = node2Map.get(node2Label);;
            }
            LWLink link = new LWLink(node1,node2);
            map.add(link);
            count++;
        }
        return map;
    }
}
