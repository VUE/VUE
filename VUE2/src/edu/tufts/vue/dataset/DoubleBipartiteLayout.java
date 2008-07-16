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
    

    public  LWMap loadMap(String fileName,String mapName) throws Exception{
        Map<String,LWNode> node1Map = new HashMap<String,LWNode>();
        Map<String,LWNode> node2Map = new HashMap<String,LWNode>();
       
        LWMap map = new LWMap(mapName);
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        int count = 0;
        int n1Counter = 0;
        int n2Counter = 0;
        while((line=reader.readLine()) != null && count <MAX_SIZE) {
            if(DEBUG.LAYOUT) System.out.println(line+" words: "+line.split(",").length);
            String[] words = line.split(",");
            LWNode node1;
            LWNode node2;
            if(!node1Map.containsKey(words[0])) {
                String label1 = words[0];
                if(words[0].length()>40)  {
                    label1 = words[0].substring(0,40)+"...";
                }
                node1 = new LWNode(label1);
                //               node1.setFillColor(Color.)
                node1Map.put(words[0],node1);
                node1.setLocation(MAP_SIZE/5 ,n1Counter*30);
                map.add(node1);
                n1Counter++;
            } else {
                node1 = node1Map.get(words[0]);
            }
            if(!node2Map.containsKey(words[1])) {
                String label2 = words[1];
                if(words[1].length()>40)  {
                    label2 = words[1].substring(0,40)+"...";
                }
                node2 = new LWNode(label2);
                node2.setFillColor(Color.LIGHT_GRAY) ;
                map.add(node2);
                node2Map.put(words[1],node2);
                node2.setLocation(MAP_SIZE/5+300 ,n2Counter*30);
                n2Counter++;
            } else {
                node2 = node2Map.get(words[1]);;
            }
            LWLink link = new LWLink(node1,node2);
            map.add(link);
            
            
            
            count++;
            System.out.println("Counters "+n1Counter+"\t"+n2Counter+"\t"+count);
        }
        return map;
    }
}
