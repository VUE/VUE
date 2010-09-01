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

package tufts.vue.ds;

import tufts.Util;
import tufts.vue.DEBUG;
import tufts.vue.gui.GUI;

import tufts.vue.MetaMap;
import tufts.vue.LWComponent;
import tufts.vue.LWNode;
import tufts.vue.LWLink;
import tufts.vue.LWMap;
import tufts.vue.MapDropTarget;
import static tufts.vue.MapDropTarget.*;
import tufts.vue.ds.DataTree.Criteria;
import tufts.vue.ds.DataTree.DataNode;
import tufts.vue.ds.DataTree.RowNode;
import tufts.vue.ds.DataTree.AllRowsNode;
import tufts.vue.ds.DataTree.SmartSearch;

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
 * @version $Revision: 1.8 $ / $Date: 2010-02-03 19:13:16 $ / $Author: mike $
 * @author  Scott Fraize
 */

class DataDropHandler extends MapDropTarget.DropHandler
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataDropHandler.class);

    /** the dropping DataNode -- note that these are not VUE nodes, they're nodes from the VUE DataTree JTree impl */
    private final DataNode droppingDataItem;
    private final DataTree dataTree;

    @Override public String toString() {
        return droppingDataItem.toString();
    }

    DataDropHandler(DataNode n, DataTree tree) {
        droppingDataItem = n;
        dataTree =tree;
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
    
    static final boolean AUTO_FIT = false;
    
            
    /** DropHandler */
    @Override public boolean handleDrop(final DropContext drop)
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
            if (DEBUG.Enabled) Log.debug("FILTER EXTRACTED: " + Util.tags(newNodes));
            
        } else {
            if (DEBUG.Enabled) Log.debug("PRODUCING NODES ON THE MAP (no drop target for filtering)");
                
            newNodes = produceAllDroppedNodes(droppingDataItem);

            // note: clusteringTargets will always be empty here
        }

        if (newNodes == null || newNodes.size() == 0)
            return false;

// We might as well run in right in the drop action and at least get a chance at having the drop cursor:
//         GUI.invokeAfterAWT(new Runnable() { public void run() {
//             GUI.activateWaitCursor(); // *** STILL isn't working even though the drop is complete & drag/drop cursor should be cleared
//             try {
                Log.info("servicing the drop: " + drop);
                serviceDrop(DataDropHandler.this, drop, newNodes, clusteringTargets);
                if (drop.items != null && drop.items.size() > 0)
                    MapDropTarget.addNodesToMap(drop);
                MapDropTarget.completeDrop(drop);
                String undoName = "Data Drop";
                if (droppingDataItem.getField() != null)
                    undoName += " (" + droppingDataItem.getField().getName() + ")";
                drop.viewer.getMap().getUndoManager().mark(undoName);
                
             
                //DEAL WITH MATRIX DATA AND ADD LINK WHEN APPROPRIATE
                if (droppingDataItem.getSchema().isMatrixDataSet)
                {
               
                	GUI.invokeAfterAWT(new Runnable() { public void run() {
                	dataTree.applyMatrixRelations(newNodes);
                	}});
                }
                            
        return true;
    }

    private static void serviceDrop
        (DataDropHandler handler,
         DropContext drop,
         List<LWComponent> newNodes,
         List<LWComponent> clusteringTargets)
    {
        final boolean doZoomFit = handler.createLinksAndLayoutNodes(drop, newNodes, clusteringTargets);

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

        final DataNode droppingDataItem = handler.droppingDataItem; // todo: need to refactor these methods to all be statics
            
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
    
        
    private boolean createLinksAndLayoutNodes
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
        // [I think this has already been done...]
        //-----------------------------------------------------------------------------

        boolean zoomFit = false;

        if (DEBUG.Enabled) {
            Log.debug("createLinksAndLayoutNodes:"
                      + "\n\tnewNodes: " + Util.tags(newNodes)
                      + "\n\tclusteringTargets: " + clusteringTargets.size());
            Util.dump(clusteringTargets);
        }
                
        //-----------------------------------------------------------------------------
        // First, locate all the nodes at the drop location -- so that any that
        // aren't later laid out elsewhere will at least appear there.
        //-----------------------------------------------------------------------------
        
        MapDropTarget.setCenterAt(newNodes, drop.location);

        final Object[] result =
            DataAction.addDataLinksForNodes(drop.viewer.getMap(),
                                            newNodes,
                                            droppingDataItem.getField());
        
        final Multiset<LWComponent> targetsUsed = (Multiset) result[0];
        final List<LWLink> linksAdded = (List) result[1];

        if (DEBUG.Enabled) {
            // TODO: targetsUsed is empty for targets found in cross-schema joins...
            Log.debug("targetsUsed: " + Util.tags(targetsUsed));
            Log.debug(" linksAdded: " + Util.tags(linksAdded));
            //Util.dump(targetsUsed.entrySet());
        }

        if (clusteringTargets.size() > 0) {
            //tufts.vue.Actions.MakeCluster.doClusterAction(clusterNode, newNodes);                
            for (LWComponent center : clusteringTargets) {
                tufts.vue.Actions.MakeCluster.doClusterAction(center, center.getClustered());
            }
        }
//         else if (drop.isLinkAction) {
//             //tufts.vue.Actions.MakeCluster.act(newNodes); // TODO: GET THIS WORKING -- needs to work w/out a center
//             tufts.vue.LayoutAction.filledCircle.act(newNodes, AUTO_FIT); // TODO: this goes into infinite loops sometimes!
//         }
        else {

            // TODO: pass isLinkAction to clusterNodes and sort out there 
            zoomFit = clusterNodes(drop, newNodes, linksAdded.size() > 0);

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

        if (newLinksAvailable) {
            // now we set this state ANY time new links are created: is screwing up too
            // many maps and disabling other times when we actually want a map-deformation
            // to happen.
            map.setState(LWMap.State.HAS_AUTO_CLUSTERED);
        }
        

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
                try {
                    tufts.vue.LayoutAction.cluster.act(nodes, AUTO_FIT);
                    map.setState(LWMap.State.HAS_AUTO_CLUSTERED);
                } catch (Throwable t) {
                    Log.warn("clustering failure on " + Util.tags(nodes), t);
                }

                //tufts.vue.Actions.MakeCluster.act(new tufts.vue.LWSelection(nodes));
                zoomFit = true;
            }
            else if (newLinksAvailable) {

                final boolean DEFORM_MAP_FOR_NEW_NODES = !drop.isLinkAction;
                
                DataAction.centroidCluster(map, nodes, DEFORM_MAP_FOR_NEW_NODES);
                    
            } else {
                // may want to do this any way for anything that didn't have a centroid
                tufts.vue.LayoutAction.filledCircle.act(nodes, AUTO_FIT);
            }
            
        } catch (Throwable t) {
            Log.error("clustering failure: " + Util.tags(nodes), t);
            zoomFit = false;
        }
        
        return zoomFit;
    }


}
        

