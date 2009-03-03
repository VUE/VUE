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
import java.util.*;
import edu.tufts.vue.dataset.*;
import tufts.vue.*;
import java.awt.geom.Point2D;

/*
 * This layout has been designed specifically for the search results. It takes
 * all the search results and displaces them in a filled circle layout. At the
 * same time all the nodes that interfere are pushed from the center.
 */
public class SearchLayout extends Layout {

    public static final double FACTOR = VueResources.getDouble("layout.space_ratio");
    public static final int MAX_COLLISION_CHECK = VueResources.getInt("layout.check_overlap_number");
    public static final double MIN_AVOID_DIST = VueResources.getDouble("layout.avoid_distance");
    public static final double RIPPLE_RANGE = VueResources.getDouble("layout.ripple_range");
    public LWMap createMap(Dataset ds, String mapName) throws Exception {
        LWMap map = new LWMap(mapName);
        return map;
    }

    public void layout(LWSelection selection) {
        double centerX = 0;
        double centerY = 0;
        double totalNodeWidth = 0;
        double totalNodeHeight = 0;
        double xAdd = X_COL_SIZE; // default horizontal distance between the nodes
        double yAdd = Y_COL_SIZE; //default vertical distance between nodes

        int count = 0;
        int total = 0;
        // determine the mean node size
        Iterator<LWComponent> iter = selection.iterator();
        while (iter.hasNext()) {
            LWComponent c = iter.next();
            if (c instanceof LWNode) {
                LWNode node = (LWNode) c;
                centerX += node.getLocation().getX();
                centerY += node.getLocation().getY();
                totalNodeWidth += node.getWidth();
                totalNodeHeight += node.getHeight();
                total++;
            }
        }
        double meanNodeWidth = totalNodeWidth / total;
        double meanNodeHeight = totalNodeHeight / total;
        centerX = centerX / total;
        centerY = centerY / total;
        double radius = Math.sqrt(FACTOR * total * meanNodeWidth * meanNodeWidth / Math.PI);
         double maxShift  = radius ; //maximum shift of a node
         double rippleRange= RIPPLE_RANGE*radius;
        // generate a ripple with center(centerX,centerY) and radius
        Iterator<LWComponent> i = VUE.getActiveMap().getAllDescendents(LWContainer.ChildKind.PROPER).iterator();
        while (i.hasNext()) {
            LWComponent c = i.next();
            if (!selection.contains(c) && c instanceof LWNode) {
                double x = c.getLocation().getX();
                double y = c.getLocation().getY();
                double angle = Math.atan2(centerY - y, x - centerX);
                double dist = Point2D.distance(centerX, centerY, x, y);
                if (dist < rippleRange && dist >MIN_AVOID_DIST) {
                    double newDist = dist + (rippleRange - dist) * maxShift/ rippleRange;
                    double newX = centerX + newDist * Math.cos(angle);
                    double newY = centerY - newDist * Math.sin(angle);
                    c.setLocation(newX, newY);
                }
            }
        }


        // layout out the selection nodes
        i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = i.next();
            if (c instanceof LWNode) {
                LWNode node = (LWNode) c;
                double angle = Math.PI * 2 * Math.random();
                double r = radius * (1 - Math.pow(Math.random(), 2.0));
                double x = centerX + r * Math.cos(angle);
                double y = centerY + r * Math.sin(angle);
                boolean flag = true;
                int col_count = 0;
                while (flag && col_count < MAX_COLLISION_CHECK) {
                    if ((VUE.getActiveViewer().pickNode((float) x, (float) y) != null) || (VUE.getActiveViewer().pickNode((float) x + node.getWidth(), (float) y + node.getHeight()) != null) || (VUE.getActiveViewer().pickNode((float) x, (float) y + node.getHeight()) != null) || (VUE.getActiveViewer().pickNode((float) x + node.getWidth(), (float) y) != null)) {
                        angle = Math.PI * 2 * Math.random();
                        r = radius * (1 - Math.pow(Math.random(), 2.0));
                        x = centerX + r * Math.cos(angle);
                        y = centerY + r * Math.sin(angle);
                    } else {
                        flag = false;
                    }
                }
                node.setLocation(x, y);

            }
        }
    }
}
