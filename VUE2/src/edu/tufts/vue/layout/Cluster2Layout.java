/*
 * ClusterLayout.java
 *
 * Created on October 1, 2008, 2:14 PM
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
import java.awt.geom.Point2D;

import javax.swing.*;
import tufts.vue.*;
import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.dataset.*;

public class Cluster2Layout extends Layout {
	public static String DEFAULT_METADATA_LABEL = "default";
	public static final int MINX_RADIUS = VueResources
			.getInt("layout.minx_radius");
	public static final int MINY_RADIUS = VueResources
			.getInt("layout.miny_radius");
	public static final int X_SPACING = VueResources.getInt("layout.x_spacing");
	public static final int Y_SPACING = VueResources.getInt("layout.y_spacing");
	public static final double FACTOR = 2;
	public static final int MAX_COLLISION_CHECK = VueResources
			.getInt("layout.check_overlap_number");

	/** Creates a new instance of ClusterLayout */
	public Cluster2Layout() {
	}

	public LWMap createMap(Dataset ds, String mapName) throws Exception {

		LWMap map = new LWMap(mapName);
		return map;
	}

	public void layout(LWSelection selection) {
		HashMap<LWComponent, ArrayList<LWComponent>> clusterMap = new HashMap<LWComponent, ArrayList<LWComponent>>();
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxNodeWidth = X_COL_SIZE;
		double maxNodeHeight = Y_COL_SIZE;
		double centerX = selection.getBounds().getCenterX();
		double centerY = selection.getBounds().getCenterY();
		double maxDistance = 0;
		// compute the radius of the cluster
		double area = 0;
		for (LWComponent component : selection) {
			if (component instanceof LWNode) {
				LWNode node = (LWNode) component;
				area += node.getWidth() * node.getHeight();
				double distance = Point2D.distance(centerX, centerY, node
						.getX(), node.getY());
				if (distance > maxDistance)
					maxDistance = distance;
			}
		}
		double radius = Math.sqrt((area * FACTOR / Math.PI));
		// move nodes in selection
		Iterator<LWComponent> i = VUE.getActiveMap().getAllDescendents(
				LWContainer.ChildKind.PROPER).iterator();
		while (i.hasNext()) {
			LWComponent c = i.next();
			if (c instanceof LWNode) {
				LWNode node = (LWNode) c;
				double x = node.getX();
				double y = node.getY();
				double angle = Math.atan2(centerY - y, x - centerX);
				double dist = Point2D.distance(centerX, centerY, x, y);
				if (selection.contains(node)) {
					double newDist = radius * dist / maxDistance;
					double newX = centerX + newDist * Math.cos(angle);
					double newY = centerY - newDist * Math.sin(angle);
					node.setLocation(newX, newY);
				} else {
					double shiftRange = 3 * radius;
//					System.out.println(node.getLabel()+"\t"+radius+"\t"+shiftRange);
					if (dist < shiftRange) {
						double newDist = dist + (shiftRange - dist) * radius
								/ shiftRange;
						double newX = centerX + newDist * Math.cos(angle);
						double newY = centerY - newDist * Math.sin(angle);
						node.setLocation(newX, newY);
					}
				}
			}
		}
	}
}
