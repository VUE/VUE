 /*
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

package tufts.vue;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/**
 * A class for some utility functions.
 * @author theoky
 *
 */
public class Toolbox {

	/** 
	 * Compute the angle between two points with p as base.
	 * Points are assumed to be in Java coordinate space (-1 in y up)
	 * @see angleBetween2PointsCoord
	 */
	public static double angleBetween2Points(Point2D p, Point2D q)
	{
    	return angleBetween2PointsCoord(p, q, true);
    }

	/**
	 * Compute the angle between two points p and q with point p as base.
	 * @param p Point 1
	 * @param q Point 2
	 * @param javaCoord true if coordinates are with negative y up
	 * @return angle in radian
	 */
	public static double angleBetween2PointsCoord(Point2D p, Point2D q, boolean javaCoord) 
	{
		double angle;
		
		double factorY = 1;
		if (javaCoord) {
			factorY = -1;
		}
		
		double k1 = q.getX() - p.getX();
		double k2 = factorY * (q.getY() - p.getY());
    	
    	if (k1 == 0) {
    		if (k2 > 0) {
    			angle = Math.PI/2;
    		} else {
    			angle = -Math.PI/2;
    		}
    	} else {
    		angle = Math.atan(k2/k1);
    	}
    	
    	if (k1 < 0) {
    		angle += Math.PI;
    	} else {
    		if (angle < 0) {
    			angle += Math.PI*2;
    		}
    	}
		return angle;
	}

	/**
	 * This function fills the HashSet deepSelection with all objects at depth "depth" which
	 * are reachable from the set comps. Objects in the set userSelection are not added
	 * to deepSelection
	 * @param userSelection the currently selected objects 
	 * @param deepSelection the result set containing the selected objects
	 * @param comps the set of components to start with
	 * @param depth the maximal depth to search 
	 * @param expandIncoming follow incoming links
	 * @param expandOutgoing follow outgoing links
	 * @param alreadyVisited stores the components already visited and the depth where they were found.
	 */
	public static void findChildrenToDepth(
			HashSet<LWComponent> userSelection, 
			HashSet<LWComponent> deepSelection, 			
			Collection<LWComponent> comps, int depth, boolean expandIncoming, boolean expandOutgoing, Hashtable<LWComponent, Integer> alreadyVisited)
	{
		for (LWComponent comp : comps) {
			Integer		alreadyVisitedAtLevel = alreadyVisited.get(comp);

			// If this component has already been visited at a higher depth, don't revisit it.
			if (alreadyVisitedAtLevel == null || alreadyVisitedAtLevel.intValue() < depth) {
				alreadyVisited.put(comp, new Integer(depth));

				boolean		compIsUserSelected = userSelection.contains(comp),
							compIsLink =  (comp.getClass() == LWLink.class);
				int 		nextDepth = depth - (compIsLink ? (compIsUserSelected ? 1 : 0) : 1);

	
				if (!compIsUserSelected) {
					deepSelection.add(comp);
				}

				if (compIsLink) {
					// Recurse for link's endpoints.

					LWLink		link = (LWLink)comp;

					LWComponent				head = link.getHead(),
											tail = link.getTail();
					int						arrowState = link.getArrowState();
					HashSet<LWComponent>	compsToTraverse = new HashSet<LWComponent>();

					if (head != null &&
							(expandIncoming && arrowState != LWLink.ARROW_HEAD ||
							expandOutgoing && arrowState != LWLink.ARROW_TAIL)) {
						compsToTraverse.add(head);
					}

					if (tail != null && 
							(expandIncoming && arrowState != LWLink.ARROW_TAIL ||
							expandOutgoing && arrowState != LWLink.ARROW_HEAD)) {
						compsToTraverse.add(tail);
					}

					if (!compsToTraverse.isEmpty()) {
						findChildrenToDepth(userSelection, deepSelection, 
								compsToTraverse, nextDepth, expandIncoming, expandOutgoing, alreadyVisited);
					}
				}

				if (nextDepth > -1) {
					// Recurse for component's links.

					HashSet<LWComponent>		linksToTraverse = new HashSet<LWComponent>();

					if (expandIncoming && expandOutgoing) {
						linksToTraverse.addAll(comp.getLinks());
					} else if (expandIncoming) {
						linksToTraverse.addAll(comp.getIncomingLinks());
					} else if (expandOutgoing) {
						linksToTraverse.addAll(comp.getOutgoingLinks());
					}

					if (!linksToTraverse.isEmpty()) {
						findChildrenToDepth(userSelection, deepSelection,
								linksToTraverse, nextDepth, expandIncoming, expandOutgoing, alreadyVisited);
					}
				}
			}
		}
	}

	/**
	 * Returns the angle from a set with the minimum difference
	 * to a given reference angle
	 * @param angles the set of angles to compare against
	 * @param directionAlpha the reference angle
	 * @return the angle with the minimal difference to the reference angle
	 */
	public static double minAlphaDifference(
			Set<Double> angles,
			double directionAlpha)
	{
		double tmpAlpha = 0;
		double oldAlpha = Math.PI * 2;
		double curAlpha = 0;
		for (Double alpha : angles) 
		{
			tmpAlpha = angleDifference(directionAlpha, alpha);
			if (tmpAlpha < oldAlpha)
			{
				oldAlpha = tmpAlpha;
				curAlpha = alpha;
			}
		}
		return curAlpha;
	}

	/**
	 * Computes the difference between two angles given in radian.  
	 * @param alpha angle 1
	 * @param beta angle 2
	 * @return the difference in radian
	 */
	public static double angleDifference(double alpha, double beta)
	{
		double tmpAlpha;
		tmpAlpha = Math.abs(alpha - beta);
		if (tmpAlpha > Math.PI)
		{ 
			// more than 180 degrees, reduce
			tmpAlpha = 2*Math.PI - tmpAlpha;
		}
		return tmpAlpha;
	}
}
