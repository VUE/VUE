/*
 * TabularLayout.java
 *
 * Created on October 8, 2008, 2:32 PM
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

/*
 * This layout puts nodes in a circle.   We may want to add the cellspacing and 
 * also extend it to make an elipse
 */


public class CircularLayout extends Layout {
    
    /** Creates a new instance of TabularLayout */
    public CircularLayout() {
    }
    public   LWMap createMap(Dataset ds,String mapName) throws Exception {
        LWMap map = new LWMap(mapName);
        return map;
    }
    
    public   void layout(LWSelection selection) throws Exception {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        double xAdd = X_COL_SIZE; // default horizontal distance between the nodes
        double  yAdd = Y_COL_SIZE; //default vertical distance between nodes

        int count = 0;
        int total = 0;
        Iterator<LWComponent> i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = i.next();
            if (c.isManagedLocation())
                continue; 
            if(c instanceof LWNode ) {
                minX = c.getLocation().getX()<minX?c.getLocation().getX():minX;
                minY =c.getLocation().getY()<minY?c.getLocation().getY():minY;
                maxX = c.getLocation().getX() > maxX ? c.getLocation().getX() : maxX;
                maxY = c.getLocation().getY() > maxY ? c.getLocation().getY() : maxY;               
                xAdd = xAdd > c.getWidth() ? xAdd : c.getWidth();
                yAdd = yAdd > c.getHeight() ? yAdd : c.getHeight();
                total++;
//               System.out.println(node.getLabel()+"X= "+node.getLocation().getX()+" Y= "+node.getLocation().getY()+" MIN: "+minX+" : "+minY);
            }
        }
       
        double size = total* xAdd/6;
        double radiusX = size;
        double radiusY = total*yAdd/3;
        double centerX = (minX + maxX+xAdd) / 2; 
        double centerY = (minY+maxY+yAdd)/2;
        i = selection.iterator();
        double angle = 0.0;
        while (i.hasNext()) {
            LWComponent c = i.next();
            if (c.isManagedLocation())
                continue; 
            if(c instanceof LWNode ) {
 
                c.setLocation(centerX+radiusX*Math.cos(angle),centerY+radiusY*Math.sin(angle));
                count++;
                angle = Math.PI*2*count/total;
            }
        }
    }
    
}
