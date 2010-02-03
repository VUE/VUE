/*
 * ClusterLayout.java
 *
 * Created on October 1, 2008, 2:14 PM
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
import java.awt.geom.Point2D;

import tufts.vue.*;
import edu.tufts.vue.dataset.*;

public class Cluster3Layout extends Layout {
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
	public Cluster3Layout() {
	}

	public LWMap createMap(Dataset ds, String mapName) throws Exception {

		LWMap map = new LWMap(mapName);
		return map;
	}

	public void layout(LWSelection selection) {
		
		HashMap<LWComponent, ArrayList<LWComponent>> clusterMap = new HashMap<LWComponent, ArrayList<LWComponent>>();
		List<LWComponent> sortedComponents = new ArrayList<LWComponent>(); // nodes in the selection sorted by size
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxNodeWidth = X_COL_SIZE;
		double maxNodeHeight = Y_COL_SIZE;
		// create the clusters and sorted list
		Iterator<LWComponent> i = VUE.getActiveMap().getAllDescendents(
				LWContainer.ChildKind.PROPER).iterator();
		// placing the cluster nodes in a map with the center node as a key
		while (i.hasNext()) {
			LWComponent c = i.next();
			if (c instanceof LWLink) {
				LWLink link = (LWLink) c;
				LWComponent head = link.getHead();
				LWComponent tail = link.getTail();
				if (selection.contains(head)) {
					if (!clusterMap.containsKey(head)) {
						clusterMap.put(head, new ArrayList<LWComponent>());
					}
					clusterMap.get(head).add(tail);
				}
				if (selection.contains(tail)) {
					if (!clusterMap.containsKey(tail)) {
						clusterMap.put(tail, new ArrayList<LWComponent>());
					}
					clusterMap.get(tail).add(head);
				}
			} else if (c instanceof LWNode) {
				maxNodeWidth = maxNodeWidth > c.getWidth() ? maxNodeWidth : c
						.getWidth();
				maxNodeHeight = maxNodeHeight > c.getHeight() ? maxNodeHeight
						: c.getHeight();
			}

		}
	}
}
