package edu.tufts.vue.layout;

import java.util.*;

import edu.tufts.vue.dataset.Dataset;
import tufts.vue.LWComponent;
import tufts.vue.LWContainer;
import tufts.vue.LWLink;
import tufts.vue.LWMap;
import tufts.vue.LWNode;
import tufts.vue.LWSelection;
import tufts.vue.VUE;

public class HierarchicalLayout extends Layout {
	public static final int MAX_DEPTH = 5; // Restricted max depth to 5
	public static final int MIN_RADIUS_INCREASE = 80;

	public LWMap createMap(Dataset ds, String mapName) throws Exception {
		LWMap map = new LWMap(mapName);
		return map;
	}

	public void layout(LWSelection selection) throws Exception {
		List<LWNode> processedNodes = new ArrayList<LWNode>();
		Iterator<LWComponent> iter = selection.iterator();
		while (iter.hasNext()) {
			LWComponent c = iter.next();
			if (c instanceof LWNode) {
				LWNode node = (LWNode) c;
				double centerX = c.getX() + c.getWidth() / 2;
				double centerY = c.getY() + c.getHeight() / 2;
				double angle = 0.0;
				List<LWNode> relatedNodes = getRelated(node, processedNodes);
				double radius = relatedNodes.size()
						* (c.getHeight() + c.getWidth()) / 2 / Math.PI;
				if (radius < 3 * c.getWidth())
					radius = 3 * c.getWidth(); // want to make the radius
												// atleast 3 times the width
				if (radius < MIN_RADIUS_INCREASE)
					radius = MIN_RADIUS_INCREASE;
				int count = 0;
				for (LWNode related : relatedNodes) {
					related.setLocation(centerX + radius * Math.cos(angle)
							- related.getWidth() / 2, centerY + radius
							* Math.sin(angle) - related.getHeight() / 2);
					layoutChildren(node, related, relatedNodes.size(),
							processedNodes);
					count++;
					angle = Math.PI * 2 * count / relatedNodes.size();
//					System.out.println("Completed Layout for: "+ related.getLabel());
				}
			}

		}

	}

	private void layoutChildren(LWNode parentNode, LWNode currentNode,
			int size, List<LWNode> processedNodes) {
		double centerX = currentNode.getX() + currentNode.getWidth() / 2;
		double centerY = currentNode.getY() + currentNode.getHeight() / 2;
		double centerParentX = parentNode.getX() + parentNode.getWidth() / 2;
		double centerParentY = parentNode.getY() + parentNode.getHeight() / 2;
		double angle = Math.atan2(centerY - centerParentY, centerX
				- centerParentX);

		List<LWNode> relatedNodes = getRelated(currentNode, processedNodes);
//		System.out.println("Applying Layout to: "+currentNode.getLabel()+" parent:"+parentNode.getLabel()+" size:"+size+" related:"+relatedNodes.size());
		
		if (relatedNodes.size() < 1)
			return;

		double radius = relatedNodes.size()
				* (currentNode.getHeight() + currentNode.getWidth()) / 2
				/ Math.PI;
		if (radius < 2 * currentNode.getWidth())
			radius = 2 * currentNode.getWidth(); // want to make the radius
													// atleast 2 times the width
		if (radius < MIN_RADIUS_INCREASE)
			radius = MIN_RADIUS_INCREASE;
		if (relatedNodes.size() == 1) {
			for (LWNode related : relatedNodes) {
//				System.out.println("Setting location for: "+related.getLabel());
				 
				related.setLocation(centerX + radius * Math.cos(angle)
						- related.getWidth() / 2, centerY + radius
						* Math.sin(angle) - related.getHeight() / 2);
				if(relatedNodes.size()>0) {
				layoutChildren(currentNode, related, relatedNodes.size(),
						processedNodes);
				}
			}
		} else if(relatedNodes.size()>1) {
			int count = 0;
			// computing the arc for plotting the children
			double dist2 = Math.pow(centerX - centerParentX, 2)
			+ Math.pow(centerY - centerParentY, 2);
			double r1 = Math.sqrt(dist2);
			double alpha = 2 * Math.PI / size / 2;
			
			double beta = alpha + Math.asin(r1 * Math.sin(alpha) / radius);
		   if(Math.abs(r1* Math.sin(alpha) / radius) >=1) {
			   beta = alpha;
			}
//			if(alpha == Math.PI/2) beta= Math.PI/2;
//			if(alpha == -Math.PI/2) beta = - Math.PI/2;
			
			beta = beta *0.95; // avoid collision of two  branches

			for (LWNode related : relatedNodes) {
//				double childAngle  = angle+(relatedNodes.size()/2 - count)*2*beta/relatedNodes.size();
				double childAngle = angle-beta*(2*count+1-relatedNodes.size())/relatedNodes.size();
				count++;
// 				System.out.println("Setting location for: "+related.getLabel()+" alpha:"+alpha+" beta:"+beta+" childAngle:"+childAngle+" r1:"+r1+" radius:"+radius+" dist2:"+dist2 );
				
			 
				related.setLocation(centerX + radius * Math.cos(childAngle)
						- related.getWidth() / 2, centerY + radius
						* Math.sin(childAngle) - related.getHeight() / 2);		
				if(relatedNodes.size()>0) {
					layoutChildren(currentNode, related, relatedNodes.size(),processedNodes);
				}
			 
			}
		}

	}

	private List<LWNode> getRelated(LWNode node, List<LWNode> processedNodes) {
		List<LWNode> relatedNodes = new ArrayList<LWNode>();
		processedNodes.add(node);
		Iterator<LWComponent> i = VUE.getActiveMap().getAllDescendents(
				LWContainer.ChildKind.PROPER).iterator();
		while (i.hasNext()) {
			LWComponent c = i.next();
			if (c instanceof LWLink) {
				LWLink link = (LWLink) c;
				LWComponent head = link.getHead();
				LWComponent tail = link.getTail();
				if (head instanceof LWNode && tail instanceof LWNode) {
					LWNode headNode = (LWNode) head;
					LWNode tailNode = (LWNode) tail;
					if (!processedNodes.contains(tailNode) && headNode == node) {
						processedNodes.add(tailNode);
						relatedNodes.add(tailNode);
					}
					if (!processedNodes.contains(headNode) && tailNode == node) {
						processedNodes.add(headNode);
						relatedNodes.add(headNode);
					}
				}
			}
		}
		return relatedNodes;
	}
}
