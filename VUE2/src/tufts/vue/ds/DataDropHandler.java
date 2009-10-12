package tufts.vue.ds;

import tufts.Util;
import tufts.vue.DEBUG;

import tufts.vue.MetaMap;
import tufts.vue.LWComponent;
import tufts.vue.LWNode;
import tufts.vue.LWMap;
import tufts.vue.MapDropTarget;
import static tufts.vue.MapDropTarget.*;
import tufts.vue.ds.DataTree.DataNode;
import tufts.vue.ds.DataTree.RowNode;
import tufts.vue.ds.DataTree.AllRowsNode;

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
    
import com.google.common.collect.Multiset;

/**
 * Once a Row, Field, or Value has been selected for dragging from the DataTree,
 * this handles what happens when it's dropped on the map.  What happends depends
 * on what it's dropped on.
 *
 * @version $Revision: 1.4 $ / $Date: 2009-10-12 19:31:52 $ / $Author: sfraize $
 * @author  Scott Fraize
 */

class DataDropHandler extends MapDropTarget.DropHandler
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataDropHandler.class);

    /** the dropping DataNode -- note that these are not VUE nodes, they're nodes from the VUE DataTree JTree impl */
    private final DataNode droppingDataItem;

    @Override public String toString() {
        return droppingDataItem.toString();
    }

    DataDropHandler(DataNode n, DataTree tree) {
        droppingDataItem = n;
    }

    /** DropHandler */
    @Override public DropIndication getIndication(LWComponent target, int requestAction) {

        final int acceptAction;
        
        if (target instanceof LWNode && target.isDataNode()) {
            acceptAction = java.awt.dnd.DnDConstants.ACTION_LINK; // indicate data-action
        }
        else if (target == null) {
            // default map drop
            acceptAction = requestAction;
        }
        else {
            //return DnDConstants.ACTION_COPY; // copy-only (e.g., no resource-link creation for "regular" nodes)
            // we're attempt to drop onto a non-data node:
            return DropIndication.rejected();
        }
        
        return new DropIndication(DROP_ACCEPT_DATA, acceptAction, target);
    }
    
    private static final boolean AUTO_FIT = false;
    
            
    /** DropHandler */
    @Override public boolean handleDrop(DropContext drop)
    {
        final List<LWComponent> clusteringTargets = new ArrayList(); // nodes already on the map
            
        final List<LWComponent> newNodes; // nodes created

        if (drop.hit != null && drop.hit.isDataNode()) {

            newNodes = produceRelatedNodes(droppingDataItem, drop, clusteringTargets);

            // clearing hit/hitParent this will prevent MapDropTarget from
            // adding the nodes to the target node, and will then add them
            // directly to the map.  Todo: something cleaner (allow an
            // override in the DropHandler for what to do w/parenting)
            drop.hit = drop.hitParent = null;
            
        } else {
            Log.debug("PRODUCING NODES ON THE MAP (no drop target for filtering)");
                
            newNodes = produceAllDroppedNodes(droppingDataItem);

            // note: clusteringTargets will always be empty here
        }

        if (newNodes == null || newNodes.size() == 0)
            return false;

        final boolean doZoomFit = layoutNodes(drop, newNodes, clusteringTargets);

        //-------------------------------------------------------
        // TODO: handle selection manually (not in MapDropTarget)
        // so we can add the field style to the selection in case
        // what was dropped is immediately styled
        //-------------------------------------------------------

        drop.items = newNodes; // tells MapDropTarget to add these to the map

        // tell MapDropTarget what to select:
        // todo: either make this always handled by the drop handler or make
        // a clean API for this (MapDropTargets has standard things to do
        // regarding application focus it is going to handle after we return,
        // and we don't want to be worrying about that in DropHandler's)
        //         if (clusteringTargets.size() > 0) {
        //             drop.select = new ArrayList(newNodes);
        //             drop.select.addAll(clusteringTargets);
        //         } else {
        //             drop.select = newNodes;
        //         }
        
        drop.select = null; // tells MapDropTarget to skip selection handling (we do it here)
        final tufts.vue.LWSelection s = drop.viewer.getSelection();
        s.clear();
        s.setSource(drop.viewer);
        s.setSelectionSourceFocal(drop.viewer.getFocal());
        
        // USER USE-CASE CONFLICT:
        //
        // If what the user wants to do next after this drop would be to style all the row nodes that
        // appeared (say, change the fill color), what we'd like is to select only the NEW NODES,
        // and set the selection with the style for those nodes.
        //
        // If what the user wants to do is drag the new nodes created along with their center clustering
        // node to somewhere else on the map, we *do* want the center cluster node in the selection, but then
        // we can't set the selection style, as both the row-node selection style and the center-clutering
        // value-node style are in play.
        //
        // For now, we're prioritizing the first case: dragging the new cluster somewhere.

            
        if (clusteringTargets.size() > 0) {

            if (DEBUG.Enabled) Log.debug("SELECTING based on both new nodes and clustering targets " + Util.tags(clusteringTargets));
            s.add(new Util.GroupIterator(newNodes, clusteringTargets));
        } else {
            if (DEBUG.Enabled) Log.debug("SELECTING based on " + droppingDataItem + "; hasStyle=" + droppingDataItem.hasStyle());
            if (droppingDataItem.hasStyle()) {
                s.setWithStyle(newNodes, "", droppingDataItem.getStyle());
            } else {
                s.setTo(newNodes);
            }
        }
        

        if (doZoomFit) {
            drop.viewer.setZoomFit();
        } else {
            // the various layout actions can completely screw up the map view for some reason,
            // so we always make sure the map is at least SOMEWHAT visible at the end.
            // TODO: debug what the layout actions are doing to the view.
            drop.viewer.ensureMapVisible();
        }

        // can't do this yet: MapDropTarget still has yet to do the actual add-to-map,
        // and then it will generate the generic "Drop" undo-mark.
        //drop.viewer.getMap().getUndoManager().mark("Data Drop: " + droppingDataItem);

        return true;
        
    }

    private static List<LWComponent> produceAllDroppedNodes(final DataNode treeNode)
    {
        final Field field = treeNode.getField();
        final Schema schema = treeNode.getSchema();
            
        Log.debug("PRODUCING NODES FOR FIELD: " + field);
        Log.debug("                IN SCHEMA: " + schema);
            
        final java.util.List<LWComponent> nodes;

        LWNode n = null;

        if (treeNode instanceof RowNode) {

            Log.debug("PRODUCING SINGLE ROW NODE");
            nodes = DataAction.makeSingleRowNode(schema, treeNode.getRow());
                
            //} else if (treeNode.isRowNode()) {
        } else if (treeNode instanceof AllRowsNode) {

            List<LWComponent> _nodes = null;
            Log.debug("PRODUCING ALL DATA NODES");
            try {
                _nodes = DataAction.makeRowNodes(schema);
                Log.debug("PRODUCED ALL DATA NODES; nodeCount="+_nodes.size());
            } catch (Throwable t) {
                Util.printStackTrace(t);
            }

            nodes = _nodes;
                
        } else if (treeNode.isValue()) {
                
            Log.debug("PRODUCING A SINGLE VALUE NODE");
            // is a single value from a column
            nodes = Collections.singletonList(DataAction.makeValueNode(field, treeNode.getValue()));
                    
        } else {

            Log.debug("PRODUCING ALL VALUE NODES FOR FIELD: " + field);
            nodes = new ArrayList();

            // handle all the enumerated values for a column
                
            for (String value : field.getValues()) {
                nodes.add(DataAction.makeValueNode(field, value));
            }
        }

        return nodes;
    }


    /**
     * Combine the given tree node with the drop target (e.g., search/filter) to find
     * the new nodes to create
     */
    private static List<LWComponent> produceRelatedNodes
        (final DataNode treeNode,
         final DropContext drop,
         final List<LWComponent> clusteringTargets)
    {
        final List<LWComponent> dropTargets = new ArrayList();
        final Field dragField = treeNode.getField();
                
        Schema dragSchema = null;
        boolean draggingAllRows = false;
        if (treeNode instanceof AllRowsNode) {
            dragSchema = treeNode.getSchema();
            draggingAllRows = true;
        }

        if (drop.hit.isSelected()) {
            dropTargets.addAll(drop.viewer.getSelection());
        } else {
            dropTargets.add(drop.hit);
        }

        Log.debug("DATA ACTION ON " + drop.hit
                  + "\n\tdropTargets: " + Util.tags(dropTargets)
                  + "\n\t  dragField: " + dragField
                  + "\n\t dragSchema: " + dragSchema);

        //-----------------------------------------------------------------------------
        // TODO: "merge" action for VALUE nodes.
        // For value nodes with the same value, delete one, merge all links
        // For value nodes with with DIFFERENT keys and/or values, create a COMPOUND VALUE
        // node that either has multiple values, or multiple keys and values.
        // This will complicate the hell out of the search code tho.
        //-----------------------------------------------------------------------------
                
                
        final List <LWComponent> newNodes = new ArrayList();
        for (LWComponent dropTarget : dropTargets) {
            final MetaMap dropTargetData;
                    
            if (dropTarget.isDataNode()) {
                dropTargetData = dropTarget.getRawData();
                clusteringTargets.add(dropTarget);
                if (draggingAllRows) {
                    // TODO: dropTargetData instead of dropTarget?
                    newNodes.addAll(DataAction.makeRelatedRowNodes(dragSchema, dropTarget));
                }
                else {
                    newNodes.addAll(DataAction.makeRelatedNodes(dragField, dropTarget));
                }
//                 else if (dropTarget.isDataRowNode()) {
//                     // TODO: if dropTarget is a single value node, this makes no sense
//                     newNodes.addAll(DataAction.makeRelatedValueNodes(dragField, dropTargetData));
//                 }
//                 else { // if (dropTarget.isDataValueNode())
//                     Log.debug("UNIMPLEMENTED: hierarchy use case? relate linked to of "
//                               + dropTarget + " based on " + dragField,
//                               new Throwable("HERE"));
//                     // if a value node, find all ROW nodes connected to it, and color
//                     // them based on the VALUES from the dragged Field?
//                     // Or, add all the values nodes and recluster all of of the linked
//                     // items based on that -- this is the HIERARCHY USE CASE.
//                     return false;
//                 }
            } else if (dropTarget.hasResource()) {
                // TODO: what is dragField going to be?  Can we drag from the meta-data pane?
                dropTargetData = dropTarget.getResource().getProperties();
                newNodes.addAll(DataAction.makeRelatedValueNodes(dragField, dropTargetData));
            }
        }

        return newNodes;
    }
    
        
    private boolean layoutNodes
        (final DropContext drop,
         final List<LWComponent> newNodes, 
         final List<LWComponent> clusteringTargets)
    {
        //-----------------------------------------------------------------------------
        // Currently, we must set node locations before adding any links, as when
        // the link-add events happen, the viewer may adjust the canvas size
        // to include room for the new links, which will all be linking to 0,0
        // unless the nodes have had their locations set, even if the nodes
        // are about to be re-laid out via a group clustering.
        //
        // TODO: can we re-work all the layout code so the nodes and links don't
        // have to first be added to the map?  It would really clean some things up.
        //-----------------------------------------------------------------------------

        boolean zoomFit = false;

        if (DEBUG.Enabled) Log.debug("NEW-DATA-NODES: " + Util.tags(newNodes));
                
        MapDropTarget.setCenterAt(newNodes, drop.location);

//         final List<tufts.vue.LWLink> linksAdded =
//             DataAction.addDataLinksForNodes(drop.viewer.getMap(),
//                                             newNodes,
//                                             droppingDataItem.getField());
//        final boolean didAddLinks = linksAdded.size() > 0;
        
        final Multiset<tufts.vue.LWComponent> targetsUsed =
            DataAction.addDataLinksForNodes(drop.viewer.getMap(),
                                            newNodes,
                                            droppingDataItem.getField());

        final boolean didAddLinks = targetsUsed.size() > 0;

        if (clusteringTargets.size() > 0) {
            //tufts.vue.Actions.MakeCluster.doClusterAction(clusterNode, newNodes);                
            for (LWComponent center : clusteringTargets) {
                tufts.vue.Actions.MakeCluster.doClusterAction(center, center.getLinked());
            }
        } else if (drop.isLinkAction) {
            //tufts.vue.Actions.MakeCluster.act(newNodes); // TODO: GET THIS WORKING -- needs to work w/out a center
            tufts.vue.LayoutAction.filledCircle.act(newNodes, AUTO_FIT); // TODO: this goes into infinite loops sometimes!
        } else {
            // TODO: pass isLinkAction to clusterNodes and sort out there 
            zoomFit = clusterNodes(drop, newNodes, didAddLinks);
        }

        return zoomFit;

    }

    private boolean clusterNodes
        (final DropContext drop,
         final List<LWComponent> nodes, 
         final boolean newLinksAvailable)
    {
        if (DEBUG.Enabled) Log.debug("clusterNodes: " + Util.tags(nodes) + "; addedLinks=" + newLinksAvailable);

        if (nodes.size() <= 1) {
            if (DEBUG.Enabled) Log.debug("clusterNodes: skipping: not enough nodes");
            return false;
        }

        boolean zoomFit = false;

        final LWMap map = drop.viewer.getMap();
        final boolean didFullReorganization = map.hasState(LWMap.State.HAS_AUTO_CLUSTERED);

        if (DEBUG.Enabled) Log.debug("clusterNodes: map has already re-organized: " + didFullReorganization);

        try {

            if (droppingDataItem.isRowNode()) {
                tufts.vue.LayoutAction.random.act(nodes, AUTO_FIT);
            }
            else if (newLinksAvailable && !didFullReorganization) {
                
//                 // TODO: Use the fast clustering code if we can --  filledCircle can
//                 // be VERY slow, and sometimes hangs!
//                 // TODO: the center nodes still need to be laid out in the big grid!
//                 for (LWComponent center : nodes) {
//                     tufts.vue.Actions.MakeCluster.doClusterAction(center, center.getLinked());
//                 }
                
                // TODO: cluster will currently fail (NPE) if no data-links exist
                // Note: this action will re-arrange all the data-nodes on the map
                tufts.vue.LayoutAction.cluster.act(nodes, AUTO_FIT);
                map.setState(LWMap.State.HAS_AUTO_CLUSTERED);
                zoomFit = true;
            }
            else {
                tufts.vue.LayoutAction.filledCircle.act(nodes, AUTO_FIT);
            }
            
        } catch (Throwable t) {
            Log.error("clustering failure: " + Util.tags(nodes), t);
            zoomFit = false;
        }
        
        return zoomFit;
    }
}
        

