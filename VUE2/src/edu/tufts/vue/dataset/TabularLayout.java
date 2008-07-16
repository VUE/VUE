/*
 * Tabular.java
 *
 * Created on July 7, 2008, 3:04 PM
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


public class TabularLayout extends  AbstractLayout{
 
    public static final String LABEL = "Tabular Layout";
    
    /** Creates a new instance of Tabular */
    public TabularLayout() {
        super(LABEL);
    }
     
    public  LWMap loadMap(String fileName,String mapName) throws Exception{
        Map<String,LWNode> nodeMap = new HashMap<String,LWNode>();
        Map<String,Integer> repeatMap = new HashMap<String,Integer>();
        
        LWMap map = new LWMap(mapName);
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        int count = 0;
        float y = 20;
        float x = 0;
        int toggle = 0;
        while((line=reader.readLine()) != null && count <MAX_SIZE) {
            if(DEBUG.LAYOUT) System.out.println(line+" words: "+line.split(",").length);
            String[] words = line.split(",");
            LWNode node1;
            LWNode node2;
            x += 300;
            if(toggle == 0) {
                toggle++;
                y = y + 50;
            } else
                if(toggle == 1) {
                toggle = 0;
                y = y - 50;
                }
            if(count % 5 == 0) {
                y += 100;
                x = 400;
                toggle = 0;
            }
            if(!nodeMap.containsKey(words[0])) {
                node1 = new LWNode(words[0]);
                nodeMap.put(words[0],node1);
                map.add(node1);
                count++;
            } else {
                node1 = nodeMap.get(words[0]);
            }
            if(!nodeMap.containsKey(words[1])) {
                node2 = new LWNode(words[1]);
                map.add(node2);
                nodeMap.put(words[1],node2);
                count++;
            } else {
                node2 = nodeMap.get(words[1]);
            }
            LWLink link = new LWLink(node1,node2);
            map.add(link);
            
            
            node1.setLocation(x,y);
            if(node2 != null)
                node2.setLocation(x+150,y);
            
        }
        return map;
    }
    
}
