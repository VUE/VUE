/*
 * Circular.java
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

public class CircularLayout extends AbstractLayout{
    public static final int MAP_SIZE = 500;
    public static final int MAX_SIZE =5000;
    static final String LABEL = "Circular Layout";
    
    /** Creates a new instance of Circular */
    public CircularLayout() {
        super(LABEL);
    }
    
    
    public LWMap loadMap(String fileName,String mapName) throws Exception{
        Map<String,LWNode> nodeMap = new HashMap<String,LWNode>();
        Map<String,Integer> repeatMap = new HashMap<String,Integer>();
        
        LWMap map = new LWMap(mapName);
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        int count = 0;
        while((line=reader.readLine()) != null && count <MAX_SIZE) {
            if(DEBUG.LAYOUT) System.out.println(line+" words: "+line.split(",").length);
            String[] words = line.split(",");
            LWNode node1;
            LWNode node2;
            if(!nodeMap.containsKey(words[0])) {
                node1 = new LWNode(words[0]);
                nodeMap.put(words[0],node1);
                map.add(node1);
            } else {
                node1 = nodeMap.get(words[0]);
            }
            if(!nodeMap.containsKey(words[1])) {
                node2 = new LWNode(words[1]);
                map.add(node2);
                nodeMap.put(words[1],node2);
            } else {
                node2 = nodeMap.get(words[1]);
            }
            LWLink link = new LWLink(node1,node2);
            map.add(link);
            double angle = Math.random()*Math.PI*4;
            node1.setLocation(MAP_SIZE*(1+Math.cos(angle)),MAP_SIZE*(1+Math.sin(angle)));
            angle = Math.random()*Math.PI*4;
            node2.setLocation(MAP_SIZE*(1+Math.cos(angle)),MAP_SIZE*(1+Math.sin(angle)));
            count++;
        }
        return map;
    }
    
    public LWMap createMap(RelationalDataset ds,String mapName) throws Exception{
        Map<String,LWNode> nodeMap = new HashMap<String,LWNode>();
         
        LWMap map = new LWMap(mapName);
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
            double angle = Math.random()*Math.PI*4;
            node1.setLocation(MAP_SIZE*(1+Math.cos(angle)),MAP_SIZE*(1+Math.sin(angle)));
            angle = Math.random()*Math.PI*4;
            node2.setLocation(MAP_SIZE*(1+Math.cos(angle)),MAP_SIZE*(1+Math.sin(angle)));
        }
        return map;
    }
    
    public LWMap createMap(ListDataset ds,String mapName) throws Exception{
        Map<String,LWNode> nodeMap = new HashMap<String,LWNode>();
        LWMap map = new LWMap(mapName);
        for(ArrayList<String> row: ds.getRowList()) {
            String node1Label = row.get(0);
            LWNode node1;
            if(!nodeMap.containsKey(node1Label)) {
                node1 = new LWNode(node1Label);
                nodeMap.put(node1Label,node1);
                map.add(node1);
            } else {
                node1 = nodeMap.get(node1Label);
            }
            
            
            double angle = Math.random()*Math.PI*4;
            node1.setLocation(MAP_SIZE*(1+Math.cos(angle)),MAP_SIZE*(1+Math.sin(angle)));
            
        }
        return map;
    }
}
