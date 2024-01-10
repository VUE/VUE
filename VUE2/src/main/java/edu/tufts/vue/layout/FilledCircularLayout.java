/*
 * TabularLayout.java
 *
 * Created on October 8, 2008, 2:32 PM
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
public class FilledCircularLayout extends Layout {

    public static final double FACTOR =VueResources.getDouble("layout.space_ratio");
    public static final int MAX_COLLISION_CHECK = VueResources.getInt("layout.check_overlap_number");

    /** Creates a new instance of TabularLayout */
    public FilledCircularLayout() {
    }

    public LWMap createMap(Dataset ds, String mapName) throws Exception {
        LWMap map = new LWMap(mapName);
        return map;
    }

    public void layout(LWSelection selection) throws Exception {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double xAdd = X_COL_SIZE; // default horizontal distance between the nodes
        double yAdd = Y_COL_SIZE; //default vertical distance between nodes
        int total = 0;
        Iterator<LWComponent> i = selection.iterator();
        while (i.hasNext()) {
        	
            LWComponent c = i.next();
            if (c.isManagedLocation())
                continue; 
            if (c instanceof LWNode) {
                LWNode node = (LWNode) c;
                minX = node.getLocation().getX() < minX ? node.getLocation().getX() : minX;
                minY = node.getLocation().getY() < minY ? node.getLocation().getY() : minY;
                xAdd = xAdd > node.getWidth() ? xAdd : node.getWidth();
                yAdd = yAdd > node.getHeight() ? yAdd : node.getHeight();

                total++;
//               System.out.println(node.getLabel()+"X= "+node.getLocation().getX()+" Y= "+node.getLocation().getY()+" MIN: "+minX+" : "+minY);
            }
        }
        xAdd = xAdd*1.10; // 10% gap
        xAdd = xAdd*1.10; 
        double x = minX;
        double y = minY;
        double radius = Math.sqrt(FACTOR * total * xAdd * yAdd / Math.PI);
        double centerX = selection.getBounds().getCenterX(); 
        double centerY = selection.getBounds().getCenterY();
        i = selection.iterator();
        while (i.hasNext()) {
        	
            LWComponent c = i.next();
            if (c.isManagedLocation())
                continue; 
            if (c instanceof LWNode) {
                LWNode node = (LWNode) c;
                double angle = Math.PI * 2 * Math.random();
                double r = radius * (1 - Math.pow(Math.random(), 2.0));
                x = centerX + r * Math.cos(angle);
                y = centerY + r * Math.sin(angle);
                boolean flag = true;
                int col_count = 0;
                while (flag && col_count < MAX_COLLISION_CHECK) {
                    if ((VUE.getActiveViewer().pickNode((float) x, (float) y) != null) || (VUE.getActiveViewer().pickNode((float) x + node.getWidth(), (float) y + node.getHeight()) != null) || (VUE.getActiveViewer().pickNode((float) x , (float) y + node.getHeight()) != null ) || (VUE.getActiveViewer().pickNode((float) x + node.getWidth(), (float) y) != null))  {
                        angle = Math.PI * 2 * Math.random();
                        r = radius * (1 - Math.pow(Math.random(), 2.0));
                        x = centerX + r * Math.cos(angle)-node.getWidth();
                        y = centerY + r * Math.sin(angle)-node.getHeight();
                    } else {
                        flag = false;
                    }
                    col_count++;
 //                   System.out.println("count: "+col_count+" MAX"+MAX_COLLISION_CHECK+" node: "+node.getLabel());
                }
                node.setLocation(x, y);

            }
        }
    }
}
