package edu.tufts.vue.layout;

import java.util.*;

import tufts.vue.LWComponent;
import tufts.vue.LWLink;
import tufts.vue.LWMap;
import tufts.vue.LWNode;
import tufts.vue.LWSelection;

public class HierarchicalLayout2 extends HierarchicalLayout {
	private static final long			serialVersionUID = 1L;
	protected static final org.apache.log4j.Logger
										Log = org.apache.log4j.Logger.getLogger(HierarchicalLayout2.class);
	protected boolean					DEBUG_LOCAL = false;


	public void layout(LWSelection selection) throws Exception {
		HashMap<String, LWNode>	processedNodes = new HashMap<String, LWNode>();
		Iterator<LWComponent>	iter = selection.iterator();

		// Layout the children and parents of each selected node.  The selected nodes
		// themselves are not moved.  If more than one node in a connected graph is selected,
		// the first one encountered in the selection iterator will be the one around which the
		// graph is layed out.
		while (iter.hasNext()) {
			LWComponent comp = iter.next();

			if (comp instanceof LWNode && !comp.isManagedLocation() && (processedNodes.get(comp.getID()) == null)) {
				LWNode			node = (LWNode)comp;

				if (DEBUG_LOCAL) {
					Log.info("Laying out node " + node.getLabel() + ".");
				}

				Vector<LWNode>	nodes = new Vector<LWNode>();
				HierarchyLayer	nodeLayer = new HierarchyLayer(node);

				nodes.add(node);
				findChildrenAndParents(nodes, nodeLayer, processedNodes);

				if (DEBUG_LOCAL) {
					nodeLayer.printLayers();
				}

				layoutLayers(node, nodeLayer);

				nodes.removeAllElements();
				nodeLayer.removeAllElements();
			}
		}

		processedNodes.clear();
	}


	protected void findChildrenAndParents(Vector<LWNode> nodes, HierarchyLayer layer, HashMap<String, LWNode> processedNodes) {
		for (LWNode node : nodes) {
			List<LWLink>		links = node.getLinks();
			Iterator<LWLink>	linkIter = links.iterator();
			Vector<LWNode>		children = new Vector<LWNode>(),
								parents = new Vector<LWNode>();

			processedNodes.put(node.getID(), node);

			// Find the node's children (nodes that point to it) and parents (nodes that it points to).
			while (linkIter.hasNext()) {
				LWLink				link = linkIter.next();
				LWComponent			head = link.getHead(),
									tail = link.getTail();
				int					arrowState = link.getArrowState();
				LWNode				child = null,
									parent = null;

				// If an link has a single arrow, the link points to the parent.  Otherwise, consider
				// the node on the other end of the link to be a child (for the purposes of no-arrow and
				// double-arrow links).
				// It's true that most trees usually point from parent to child, but this works the opposite
				// because that's what Java Analysis expects.
				if (node.equals(head)) {
					if (tail instanceof LWNode) {
						if (arrowState == LWLink.ARROW_TAIL) {
							parent = (LWNode)tail;
						}
						else {
							child = (LWNode)tail;
						}
					}
				}
				else if (node.equals(tail)) {
					if (head instanceof LWNode) {
						if (arrowState == LWLink.ARROW_HEAD) {
							parent = (LWNode)head;
						}
						else {
							child = (LWNode)head;
						}
					}
				}

				if (child != null) {
					String			childID = child.getID();

					if (processedNodes.get(childID) == null) {
						processedNodes.put(childID, child);
						children.add(child);

						if (DEBUG_LOCAL) {
							Log.info(node.getLabel() + " has child " + child.getLabel() + ".");
						}
					}
				}
				else if (parent != null) {
					String			parentID = parent.getID();

					if (processedNodes.get(parentID) == null) {
						processedNodes.put(parentID, parent);
						parents.add(parent);

						if (DEBUG_LOCAL) {
							Log.info(node.getLabel() + " has parent " + parent.getLabel() + ".");
						}
					}
				}
			}

			// Add the children and parents to their respective layers before recursing to the
			// higher and lower layers so that siblings are next to each other and aren't
			// interspersed with their cousins.
			layer.addChildNodes(children);
			layer.addParentNodes(parents);
			findChildrenAndParents(children, layer.getChildLayer(), processedNodes);
			findChildrenAndParents(parents, layer.getParentLayer(), processedNodes);

			children.removeAllElements();
			parents.removeAllElements();
		}
	}


	protected void layoutLayers(LWNode node, HierarchyLayer layer) {
		// Arrange nodes in the layer which includes the selected node.
		// The selected node will be left in its current location.
		// The other nodes in this layer will be placed to the selected node's right.
		float		layerCenterX = node.getX() + (layer.getPaddedWidth() / 2),
					layerCenterY = node.getY() + (node.getHeight() / 2);

		layer.layout(layerCenterX, layerCenterY);

		// Arrange nodes in layers below (children);
		HierarchyLayer	childLayer = layer.getChildLayer();
		float			childLayerCenterY = layerCenterY,
						previousLayerHeight = layer.getHeight();

		while (childLayer != null) {
			float		childLayerHeight = childLayer.getHeight();

			childLayerCenterY += (previousLayerHeight / 2) + (childLayerHeight / 2 ) +
				(2 * Math.min(previousLayerHeight, childLayerHeight));
			childLayer.layout(layerCenterX, childLayerCenterY);
			childLayer = childLayer.getChildLayer();
			previousLayerHeight = childLayerHeight;
		}

		// Arrange nodes in layers above (parents).
		HierarchyLayer	parentLayer = layer.getParentLayer();
		float			parentLayerCenterY = layerCenterY;

		previousLayerHeight = layer.getHeight();

		while (parentLayer != null) {
			float		parentLayerHeight = parentLayer.getHeight();

			parentLayerCenterY -=  (previousLayerHeight / 2) + (parentLayerHeight / 2 ) +
				2 * Math.min(previousLayerHeight, parentLayerHeight);
			parentLayer.layout(layerCenterX, parentLayerCenterY);
			parentLayer = parentLayer.getParentLayer();
			previousLayerHeight = parentLayerHeight;
		}
	}


	class HierarchyLayer {
		protected final float	HORIZONTAL_PADDING_FRACTION = 2;
		HierarchyLayer			mChildren = null,
								mParents = null;
		Vector<LWNode>			mNodes = null;
		float					mWidth = 0,
								mHeight = 0;


		public HierarchyLayer(LWNode node) {
			addNode(node);
		}


		public void addNode(LWNode node) {
			float	nodeHeight = node.getHeight();

			if (mNodes == null) {
				mNodes = new Vector<LWNode>();
			}

			mNodes.add(node);
			mWidth += node.getWidth();

			if (nodeHeight > mHeight) {
				mHeight = nodeHeight;
			}
		}


		public void addChildNode(LWNode child) {
			if (mChildren == null) {
				mChildren = new HierarchyLayer(child);
				mChildren.mParents = this;
			} else {
				mChildren.addNode(child);
			}
		}


		public void addParentNode(LWNode parent) {
			if (mParents == null) {
				mParents = new HierarchyLayer(parent);
				mParents.mChildren = this;
			} else {
				mParents.addNode(parent);
			}
		}


		public void addChildNodes(Vector<LWNode> children) {
			for (LWNode child : children) {
				addChildNode(child);
			}
		}


		public void addParentNodes(Vector<LWNode> parents) {
			for (LWNode parent : parents) {
				addParentNode(parent);
			}
		}


		public HierarchyLayer getChildLayer() {
			return mChildren;
		}


		public HierarchyLayer getParentLayer() {
			return mParents;
		}


		public HierarchyLayer getHighestLayer() {
			HierarchyLayer	result = this;

			if (mParents != null) {
				result = mParents.getHighestLayer();
			}

			return result;
		}


		public float getWidth() {
			return mWidth;
		}


		public float getPaddedWidth() {
			int		nodeCount = mNodes.size();
			float	horizontalPadding = (mWidth / HORIZONTAL_PADDING_FRACTION) / nodeCount;

			return mWidth + (horizontalPadding * (nodeCount - 1));
		}


		public float getHeight() {
			return mHeight;
		}


		public void removeAllElements() {
			getHighestLayer().removeAllElementsAndChildren();
		}


		protected void removeAllElementsAndChildren() {
			if (mNodes != null) {
				mNodes.removeAllElements();
				mNodes = null;
			}

			mParents = null;

			if (mChildren != null) {
				mChildren.removeAllElementsAndChildren();
				mChildren = null;
			}
		}


		public void layout(float centerX, float centerY) {
			int		nodeCount = mNodes.size();
			float	x = centerX - (getPaddedWidth() / 2),
					horizontalPadding = (mWidth / HORIZONTAL_PADDING_FRACTION) / nodeCount;

			for (LWNode node : mNodes) {
				float	y = centerY - (node.getHeight() / 2);

				node.setLocation(x, y);
				x += node.getWidth() + horizontalPadding;
			}
		}


		public void printLayers() {
			getHighestLayer().printLayerAndChildren(1);
		}


		protected void printLayerAndChildren(int level) {
			System.out.print(level + " (width " + mWidth + ", height " + mHeight + "): ");

			for (int index = 0; index < mNodes.size(); index++) {
				if (index > 0) {
					System.out.print(", ");
				}

				System.out.print(mNodes.elementAt(index).getLabel());
			}

			System.out.println();

			if (mChildren != null) {
				mChildren.printLayerAndChildren(level + 1);
			}
		}
	}
}
