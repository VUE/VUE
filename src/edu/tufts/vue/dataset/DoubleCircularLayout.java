/*
 * DoubleCircular.java
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
import java.awt.*;
import javax.swing.*;
import tufts.vue.*;
import java.awt.geom.*;

public class DoubleCircularLayout extends AbstractLayout{
    
    public final int COLUMNS = 4;
    
    public static final String LABEL = "Double Circular Layout";
    
    /** Creates a new instance of DoubleCircular */
    public DoubleCircularLayout() {
        super(LABEL);
    }
    
      public LWMap createMap(Dataset ds,String mapName) throws Exception{
        MAP_SIZE = 5000;
        double Q_SIZE = (double)MAP_SIZE/COLUMNS;
        
        
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
                n1Counter++;
                String label1 = node1Label;
                
                node1 = new LWNode(label1);
                 double x = (n1Counter%4)*Q_SIZE-Q_SIZE/2;
                double y = (n1Counter/4)*Q_SIZE-Q_SIZE/2;
                node1Map.put(node1Label,node1);
                node1.setLocation(x,y);
                map.add(node1);
            } else {
                node1 = node1Map.get(node1Label);
            }
            String label2 = node2Label;
             
            node2 = new LWNode(label2);
            node2.setFillColor(Color.LIGHT_GRAY) ;
            map.add(node2);
            double angle = Math.random()*Math.PI*4;
            Point2D point = node1.getLocation();
            node2.setLocation(point.getX()+Math.cos(angle)*Q_SIZE/3,point.getY()+Math.sin(angle)*Q_SIZE/3);
            n2Counter++;
            LWLink link = new LWLink(node1,node2);
            map.add(link);
            count++;
        }
        return map;
    }
}
