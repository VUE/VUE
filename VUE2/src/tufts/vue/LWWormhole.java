/*
 * This addition Copyright 2010-2012 Design Engineering Group, Imperial College London
 * Licensed under the
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
* @author  Helen Oliver, Imperial College London 
*/

package tufts.vue;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.JOptionPane;

import edu.tufts.vue.layout.Layout;

import tufts.Util;
import tufts.vue.LWComponent.ChildKind;
import tufts.vue.NodeTool.NodeModeTool;
import tufts.vue.action.ActionUtil;
import tufts.vue.action.OpenAction;
import tufts.vue.action.SaveAction;

public class LWWormhole implements VueConstants {
	
	private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWWormhole.class);
	
	/**
	 * The component that will be the starting point
	 * of this wormhole.
	 */
	private LWComponent sourceComponent;
	
	/**
	 * The map that contains the source component
	 * of this wormhole.
	 */
	private LWMap sourceMap;
	
	/**
	 * The component that will be the end point
	 * of this wormhole.
	 */
	private LWComponent targetComponent;
	
	/**
	 * The map that contains the target component
	 * of this wormhole.
	 */
	private LWMap targetMap;
	
	/**
	 * The wormhole node that is the starting point
	 * for this wormhole.
	 */
	private LWWormholeNode sourceWormholeNode;
	
	/**
	 * The wormhole node that is the end point
	 * for this wormhole.
	 */
	private LWWormholeNode targetWormholeNode;	
	
	/**
	 * The file object for the source map.
	 */
	private File sourceMapFile;
	
	/**
	 * The filename for the source map.
	 */
	private String sourceMapFileName;
	
	/**
	 * The file object for the target map.
	 */
	private File targetMapFile;
	
	/**
	 * The filename for the target map.
	 */
	private String targetMapFileName;	
	
	/**
	 * The URI object for the source node map resource.
	 */
	private URI sourceResourceMapURI;
	
	/**
	 * The URI String object for the source node map resource.
	 */
	private String sourceResourceMapURIString;	
	
	/**
	 * The URI object for the target node map resource.
	 */
	private URI targetResourceMapURI;
	
	/**
	 * The URI String object for the target node map resource.
	 */
	private String targetResourceMapURIString;		
	
	/**
	 * The URI object for the source node component resource.
	 */
	private URI sourceResourceComponentURI;
	
	/**
	 * The URI String object for the source node component resource.
	 */
	private String sourceResourceComponentURIString;	
	
	/**
	 * The URI object for the target node component resource.
	 */
	private URI targetResourceComponentURI;
	
	/**
	 * The URI String object for the target node component resource.
	 */
	private String targetResourceComponentURIString;	
	
	/**
	 * The source resource, funnily enough
	 * Consists of the source resource map URI (pointing to the target map),
	 * and the source resource component URI (pointing to the target component)
	 */
	private Resource sourceResource;
	
	/**
	 * The target resource, funnily enough
	 * Consists of the target resource map URI (pointing to the source map),
	 * and the target resource component URI (pointing to the source component)
	 */
	private Resource targetResource;
	
	/**
	 * Flags whether this wormhole was cancelled at any point
	 * during its creation
	 */
	private boolean bCancelled;
	
	/**
	 * Flags whether we are constructing this wormhole
	 * before saving, or not
	 * default is false
	 */
	private boolean bSaving = false;

    // daft string manipulation constants
	private String strPossPrefix = "file:";
	private String strBackSlashPrefix = "\\\\";
	private String strBackSlash = "\\";
	private String strForwardSlashPrefix = "////";
	private String strForwardSlash = "/";
	private String strPossPrefixPlusForwardSlash = strPossPrefix + strForwardSlash;


	public LWWormhole() {
		// TODO Auto-generated constructor stub
	}
	
	// HO 28/05/2012 BEGIN ***********
	/**
	 * rhb hack
	 */
	public LWWormhole(LWComponent c, LWComponent t) {
		init(c, t);
	}
	
	public LWWormhole(LWComponent c, LWComponent t, float x1, float y1, float x2, float y2) {
		init(c, t, x1, y1, x2, y2);
	}	
	// HO 28/05/2012 END ************
	
	/**
	 * Wormhole constructor.
	 * @param c, the LWComponent to which to add the wormhole.
	 * @param bNew, true if the wormhole is to be added to a new map,
	 * false if it is to be added to an existing map
	 */
	public LWWormhole(LWComponent c, boolean bNew) {
		init(c, bNew);
	}
	
	/**
	 * Wormhole constructor.
	 * @param wn, the wormhole node from which to extrapolate a Wormhole
	 * @param wr, the wormhole resource contained in the wormhole node
	 */
	public LWWormhole(LWWormholeNode wn, WormholeResource wr) {
		init(wn, wr);
	}
	
	/**
	 * Wormhole constructor.
	 * @param wn, the wormhole node from which to extrapolate a Wormhole
	 * @param wr, the wormhole resource contained in the wormhole node
	 * @param prevURI, the previous URI of the parent component of the wormhole node
	 * @param newParent, the new parent LWComponent of the wormhole node
	 */
	public LWWormhole(LWWormholeNode wn, WormholeResource wr, String prevURI, LWComponent newParent) {
		init(wn, wr, prevURI, newParent);
	}
	
	// HO 28/05/2012 BEGIN ********
	/**
	 * rhb hack
	 */
	public void init(LWComponent c, LWComponent t) 
    {
    	// we are cancelled until we have successfully
		// constructed the whole wormhole
		setBCancelled(true);
		findAndSaveAllOpenMaps();
		setSourceComponent(c);
    	setSourceMap(sourceComponent.getParentOfType(LWMap.class));
		setTargetComponent(t);
		setTargetMap(targetComponent.getParentOfType(LWMap.class));
		setSourceAndTargetMapFiles();

		
		// create the wormhole nodes
		//createWormholeNodes();
		setSourceWormholeNode(createSourceWormholeNode());
    	setTargetWormholeNode(createTargetWormholeNodeRHB());    	

    	
		// position the components in the maps
		placeComponents();

		// set the resources
    	setWormholeResources();
    	
    	// add the listeners
    	addAllListeners();
		
    	// if we got this far we're okay and not cancelled
    	setBCancelled(false);
    	
    	// flag end of construction process
    	flagEndOfConstruction(sourceMap);
    	flagEndOfConstruction(targetMap);
    }
	
	public void init(LWComponent c, LWComponent t, float x1, float y1, float x2, float y2) 
    {
    	// we are cancelled until we have successfully
		// constructed the whole wormhole
		setBCancelled(true);
		findAndSaveAllOpenMaps();
		setSourceComponent(c);
    	setSourceMap(sourceComponent.getParentOfType(LWMap.class));
		setTargetComponent(t);
		setTargetMap(targetComponent.getParentOfType(LWMap.class));
		setSourceAndTargetMapFiles();
    
		setSourceWormholeNode(createSourceWormholeNode());
    	setTargetWormholeNode(createTargetWormholeNodeRHB());    	
    	
		sourceWormholeNode.setLocation(x1, y1);
		sourceMap.addNode(sourceWormholeNode);
		sourceWormholeNode.setShape(java.awt.geom.Ellipse2D.Float.class);
		sourceWormholeNode.setFillColor(null);
		sourceWormholeNode.setStrokeWidth(1);
		LWLink k1 = new LWLink(sourceComponent, sourceWormholeNode);
		sourceMap.addLink(k1);
		targetWormholeNode.setLocation(x2, y2);
		targetMap.addNode(targetWormholeNode);
		targetWormholeNode.setShape(java.awt.geom.Ellipse2D.Float.class);
		targetWormholeNode.setFillColor(null);
		targetWormholeNode.setStrokeWidth(1);
		LWLink k2 = new LWLink(targetWormholeNode, targetComponent);
		targetMap.addLink(k2);
    	
		//makeSureTargetNodeDoesNotOverlap(targetMap);		

		// set the resources
    	setWormholeResources();
    	
    	// add the listeners
    	addAllListeners();
		
    	// if we got this far we're okay and not cancelled
    	setBCancelled(false);
    	
    	// flag end of construction process
    	flagEndOfConstruction(sourceMap);
    	flagEndOfConstruction(targetMap);
    }	
	
	// HO 28/05/2012 END **********
		
	/**
	 * Initializes the wormhole.
	 * Constructor created for use when instantiating a new wormhole
	 * from scratch.
	 * @param c, the LWComponent to which to add the wormhole.
	 * @param bNew, true if the wormhole is to be added to a new map,
	 * false if it is to be added to an existing map.
	 */
	public void init(LWComponent c, boolean bNew) 
    {
    	// we are cancelled until we have successfully
		// constructed the whole wormhole
		setBCancelled(true);
		// any maps that are already open, auto-save them
		// HO 27/03/2012 BEGIN *********
		// findAndSaveAllOpenMaps();
		// HO 27/03/2012 END *********
		
		// create the components and maps
		boolean b = createComponentsAndMaps(c, bNew);
		// if they stopped here, no need to go any further
		if (b == false) {
			// flag construction process as over for both maps
			flagEndOfConstruction(sourceMap);
			flagEndOfConstruction(targetMap);
			return;	
		}
		
		// create the wormhole nodes
		createWormholeNodes();
    	
		// position the components in the maps
		placeComponents();

		// set the resources
    	setWormholeResources();
    	
    	// add the listeners
    	addAllListeners();
		
    	// if we got this far we're okay and not cancelled
    	setBCancelled(false);
    	
    	// flag end of construction process
    	flagEndOfConstruction(sourceMap);
    	flagEndOfConstruction(targetMap);
    }
	
	/**
	 * A routine to flag, at module and global levels,
	 * that wormholes are being constructed in a particular map.
	 * @param map, the LWMap in which wormholes are being constructed.
	 */
	private void flagStartOfConstruction(LWMap map) {
		if (map != null) {
			map.bConstructingWormholes = true;
			VUE.bConstructingWormholes = true;
		}
	}
	
	/**
	 * A routine to flag, at module and global levels,
	 * that wormhole construction has completed in a particular map.
	 * @param map, the LWMap in which wormholes have completed construction.
	 */
	private void flagEndOfConstruction(LWMap map) {
		if (map != null) {
			map.bConstructingWormholes = false;
			VUE.bConstructingWormholes = false;
		}
	}
	
	/**
	 * Initializes the wormhole by extrapolating the needed information
	 * from a LWWormholeNode and a WormholeResource.
	 * Created for use during a restore from a persisted map.
	 * @param wn, the LWWormholeNode we're going to use to extrapolate the wormhole
	 * @param wr, the WormholeResource in this LWWormholeNode
	 */
	public void init(LWWormholeNode wn, WormholeResource wr) 
    {
		// all maps that are already open must be saved
		// HO 16/04/2012 BEGIN ********
		//findAndSaveAllOpenMaps();
		// HO 16/04/2012 END ******
		
		// we are cancelled until we have successfully
		// constructed the whole wormhole
		setBCancelled(true);
		// extrapolate the components, maps, and nodes
		// from the wormhole node and wormhole resource passed in
		boolean b = extrapolateComponentsMapsAndNodes(wn, wr);

		// if they stopped here, no need to go any further
		if (b == false)
			return;	
		
		// reset the newly-reconstructed resources
		resetResources();
    	
		// add the listeners
    	addAllListeners();

    	// if we got this far we're okay
    	// and the process was not cancelled before completion
    	setBCancelled(false);
    }	
	
	/**
	 * Initializes the wormhole by extrapolating the needed information
	 * from a LWWormholeNode and a WormholeResource.
	 * Created for use during a restore from a persisted map.
	 * @param wn, the LWWormholeNode we're going to use to extrapolate the wormhole
	 * @param wr, the WormholeResource in this LWWormholeNode
	 * @param newParent, the component that will be the new source component,
	 * false otherwise
	 */
	public void init(LWWormholeNode wn, WormholeResource wr, String prevURI, LWComponent newParent) 
    {
		// any open maps, auto-save them
		// HO 16/04/2012 BEGIN *******
		// findAndSaveAllOpenMaps();
		// HO 16/04/2012 END *********
		
		// we are cancelled until we have successfully
		// constructed the whole wormhole
		setBCancelled(true);
		
		// input validation
		if ((wn == null) || (wr == null) || (prevURI == null) || (prevURI.equals("")) || (newParent == null))
			return;
		
		// extrapolate the components, maps, and nodes
		// from the wormhole node and wormhole resource passed in
		boolean b = extrapolateDuringReparent(wn, wr, prevURI, newParent);
		// if they stopped here, no need to go any further
		if (b == false)
			return;	

		// reset the newly-reconstructed resources
		resetResources();
    	
		// add the listeners
    	addAllListeners();

    	// if we got this far we're okay
    	setBCancelled(false);
    }	
	
	/**
	 * Adds the listeners to the maps on instantiation
	 * of a wormhole.
	 * Will not be persisted on restoring a saved map.
	 */
	public void addAllListeners() {
    	// add a listener so we can notify the wormhole
    	// if the map changes
    	addListenerToMap(sourceMap);
    	// add a listener so we can notify the wormhole
    	// if the map changes
    	addListenerToMap(targetMap);
	}
	
	/**
	 * Convenience method for creating the components and the maps
	 * given a source component.
	 * @param c, the LWComponent that will be the source component.
	 * @param bNew, true if this wormhole is to be targeted into
	 * a new map, false if it is to be targeted into an existing map.
	 * @return false if the components and maps are successfully
	 * created, true otherwise.
	 */
	private boolean createComponentsAndMaps(LWComponent c, boolean bNew) {
		// input validation
		if (c == null)
			return false;
		// get the current node/link/group/component/whatever
    	// and save it as the source component
    	setSourceComponent(c);
    	// get the source component's parent map and make it the source map
    	// temporary - we'll change this after we get the file
    	setSourceMap(sourceComponent.getParentOfType(LWMap.class));
    	
    	// create a default-style target node
    	setTargetComponent(createDefaultTargetNode());
    	 	
    	// get the MapViewer that goes with the source map
    	MapViewer viewer = VUE.getCurrentTabbedPane().getViewerWithMap(sourceMap);
        // now silently save the source map
    	// HO 27/03/2012 BEGIN *********
    	// LWMap srcMap = SaveAction.saveMapSpecial(sourceMap, false, false, "Save Source Map");
    	LWMap srcMap = sourceMap;
    	// HO 27/03/2012 END *********
    	// get the right viewer again (would it have changed??)
    	viewer = VUE.getCurrentTabbedPane().getViewerWithMap(sourceMap);
    	
    	// if the current map wasn't saved for any reason,
    	// return
    	if (srcMap == null) {
    		return false;
    	} else {
    		// set what we got as the new source map
    		setSourceMap(srcMap);
    		// now that we have the map, flag that we are
    		// in the process of creating its wormholes
    		flagStartOfConstruction(srcMap);
    	}
    	
    	// the target file
    	File targFile = null;
    	
    	// the target map
    	LWMap targMap = null;
    	
    	// ask them to choose a new or existing map for the target map
    	if (bNew == true) { // if they want the target to be in a new map
    		targMap = askSelectNewTargetMap(new LWMap(""));
    		// reset the focus back to the source map 
    		// the viewer and pane on the left
    		MapViewer leftViewer = null;
    		MapTabbedPane leftPane = null;
    		// the viewer and pane on the right
            MapViewer rightViewer = null;
            MapTabbedPane rightPane = null;
            // if the active viewer is on the left, get them
            // from the left side
            if (VUE.isActiveViewerOnLeft()) {
            	leftPane = VUE.getLeftTabbedPane();
            	// find the tab that contains the source map
            	int i = leftPane.findTabWithMap(srcMap);
            	// make sure the source map tab is selected
            	leftPane.setSelectedMap(srcMap);
            }
            else {
            	// if the active viewer is on the right,
            	// get the viewer and pane from the right side
            	rightPane = VUE.getRightTabbedPane();
            	// find the tab that contains the source map
            	int i = rightPane.findTabWithMap(srcMap);
            	// make sure the source map tab is selected
            	rightPane.setSelectedIndex(i);
            } 
    	} else { // if they want the target to be in an existing map
			// ask them to choose a file to be the target file
			targMap = askSelectExistingTargetMap(sourceMap);
		}
    	
    	// if we haven't got a target map at this point,
    	// we've failed, so return
    	if (targMap == null)
    		return false;
    	// otherwise, set what we got as the new target map
    	else {
    		setTargetMap(targMap);
    		// now that we have the map, flag that we are
    		// in the process of creating its wormholes
    		flagStartOfConstruction(targMap);
    	}
    	
        // set the file objects for the source and target maps
    	setSourceAndTargetMapFiles();
    	// HO 16/04/2012 BEGIN ******
    	if ((sourceMapFile == null) || (targetMapFile == null))
    		return false;
    	// HO 16/04/2012 END *******
    	// if we got this far we've succeeded
    	return true;
	}
	
	/**
	 * A function to check whether the map has a data file yet, and,
	 * if not, alert the user.
	 * @param theMap
	 * @return true if the map has a data file, false otherwise
	 */
	private boolean makeSureMapHasFile(LWMap theMap) {
		boolean hasFile = true;
		
		File theFile = null;
		if (theMap != null) {
			theFile = theMap.getFile();
	    	// If at this point, we have a null target map file,
	    	// that probably means somebody tried to save it straight
	    	// into .vpk format, which means that at the point we need it,
	    	// we don't have a file object. For now, alert them to
	    	// save as .vue and start again.
	    	if (theFile == null) {
	    		VueUtil.alert((Component)VUE.getApplicationFrame(),
	                    VueResources.local("dialog.savewormholetovpk.message"), 
	                    VueResources.local("dialog.savewormholetovpk.title"), 
	                    JOptionPane.ERROR_MESSAGE);
	    		
	    		hasFile = false;
	    	} 	    		
		}
		
		return hasFile;
	}
	
	/**
	 * A function to work out whether the target map is the same
	 * as the source map.
	 * @param wr, the WormholeResource containing the source and target maps.
	 * @return true if the source and target maps are the same,
	 * false otherwise.
	 */
	private boolean pointsToSameMap(WormholeResource wr) {
		// assume it's not the same map until proven otherwise
		boolean bSameMap = false;
		// get the target path from the WormholeResource
		String strSpec = wr.getSystemSpec();
		// get the source path from the WormholeResource
		String strOriginatingFile = wr.getOriginatingFilename();

		// if the spec was not set, replace it with the last known filename
		if (strSpec.equals(wr.SPEC_UNSET))
			strSpec = wr.getTargetFilename();
		
		bSameMap = VueUtil.pointsToSameMap(strSpec, strOriginatingFile);
								
		return bSameMap;
	} 
	
	/**
	 * A function for extrapolating and setting the source map.
	 * @param wn, the LWWormholeNode located in the source map
	 * @return the LWMap that is the source map.
	 * @author Helen Oliver
	 */
	private LWMap extrapolateAndSetSourceMap(LWWormholeNode wn) {
		// validate input
		if (wn == null) 
			return null;
		
		// Start by extrapolating the source map from the node parent
		LWMap actualSourceMap = null;
		try {
			actualSourceMap = wn.getParentOfType(LWMap.class);
		} catch (NullPointerException e) {
			return null;
		}

		// if the node has a parent map, make it the source map
		if (actualSourceMap != null) {
			setSourceMap(actualSourceMap);
			return actualSourceMap;
		} else {
			// if it doesn't have a parent map, we're hosed.
			return null;
		}
	}
	
	/**
	 * A function for extrapolating and setting the target map.
	 * @param bSameMap, a boolean which is true if the source and target maps
	 * are the same, false otherwise.
	 * @param wr, the WormholeResource that is contained in the target map.
	 * @return the LWMap that is the target map.
	 * @author Helen Oliver
	 */
	private LWMap extrapolateAndSetTargetMap(boolean bSameMap, WormholeResource wr) {
		// input validation
		if (wr == null)
			return null;
		
		LWMap targMap = null;
		
		if (bSameMap) // if target and source maps are the same, problem solved
			targMap = (LWMap) getSourceMap();
		else // if not the same, go ahead and extrapolate the target map from the resource
			targMap = extrapolateTargetMap(wr);

		
		// if we now have a target map
		if (targMap != null) {
			// set it as our target map component
			setTargetMap(targMap);
			return targMap;
		}
		else
			// but if we don't have a map, we failed
			return null;
		
	}
	
	/**
	 * A function to search for the source component in the source map.
	 * @param actualSourceMap, the LWMap that we are searching
	 * @param wr, the WormholeResource identifying the source component
	 * @return the source LWComponent found in the source map.
	 * @author Helen Oliver
	 */
	private LWComponent searchForSourceComponentInSourceMap(LWMap actualSourceMap, WormholeResource wr) {
		// input validation
		if ((actualSourceMap == null) || (wr == null))
			return null;
		
		LWComponent sourceComp = actualSourceMap.findChildByURIString(wr.getOriginatingComponentURIString());
		// if we found the component, set it to be the source component
		if (sourceComp != null) {
			setSourceComponent(sourceComp);
			// return the source node
			return sourceComp;
		} else {
			// we have neither a source nor a target,
			// so we can't do a thing with it
			return null;
		}

	}
	
	/**
	 * A function to extrapolate the target component
	 * @param wr, the WormholeResource that identifies the target component
	 * @param targMap, the target LWMap
	 * @return the target LWComponent
	 * @author Helen Oliver
	 */
	private LWComponent extrapolateTargetComponent(WormholeResource wr, LWMap targMap) {
		// input validation
		if ((wr == null) || (targMap == null))
			return null;
		
		// now we extrapolate the target component
		// by getting the component URI string from the resource
		String compString = wr.getComponentURIString();
		// if the component URI string is null or empty,
		// we can't extrapolate enough information to create
		// the wormhole, so flag failure and return
		if ((compString == null) || (compString == ""))
			return null;
		
		// but if we do have a component URI string, we can
		// use it to find the target component in the target map
		LWComponent targetComp = targMap.findChildByURIString(compString);
		return targetComp;
	}
	
	/**
	 * A function to extrapolate, and set, the source LWComponent,
	 * and both the source and target wormhole nodes.
	 * The object is to look at the target component and see if we can extrapolate
	 * whether it's the right one, in case the component has more
	 * than one wormhole node, by cycling through all the wormhole nodes that are contained
	 * within the target component.
	 * @param targetComp, the target LWComponent
	 * @param actualSourceMap, the source LWMap
	 * @param wn, the LWWormholeNode associated with the source node
	 * @return the LWComponent that is the source component
	 * @author Helen Oliver
	 */
	private LWComponent extrapolateAndSetSourceComponentAndWormholeNodes(LWComponent targetComp, LWMap actualSourceMap,
			LWWormholeNode wn) {
		// input validation
		if ((targetComp == null) || (actualSourceMap == null) || (wn == null))
			return null;
		
		// the putative source component
		LWComponent sourceComp = null;
		
		// now look at the target component and see if we can extrapolate
		// whether it's the right one (just in case the component has more
		// than one wormhole node
		// Start by getting all the wormhole nodes that are contained
		// within the target component
		Collection<LWWormholeNode> wormholeNodes = targetComp.getAllWormholeNodes();
		// iterate through all the wormhole nodes
		for(LWWormholeNode nextNode :wormholeNodes) {
			// if the node contains a resource
			if (nextNode.hasResource()) {
				// get the resource contained in the node
				Resource r = nextNode.getResource();
					// check and see if the resource in this node is a wormhole resource
					if (r.getClass().equals(tufts.vue.WormholeResource.class)) {
						// okay now we have to make sure that it was supposed to
						// be pointing towards the map we're in now, 
						// because if there's more than one resource,
						// or if there was a resource but it went missing,
						// we could be changing the values on the wrong resource
						// Downcast the resource to a wormhole resource
						WormholeResource worm = (WormholeResource)r;
						// Get the component the resource is pointing to
						// by searching the source map for a component with
						// this URI string
						sourceComp = actualSourceMap.findChildByURIString(worm.getComponentURIString());
						// if we found the component, set it to be the source component
						if (sourceComp != null) {
							setSourceComponent(sourceComp);
							// set the originally passed-in wormhole node
							// to be the source wormhole node
							wn.setWormholeType(LWWormholeNode.WormholeType.SOURCE.toString());
							setSourceWormholeNode(wn);
							// set the target wormhole node to be the node
							// we just got the wormhole resource out of
							nextNode.setWormholeType(LWWormholeNode.WormholeType.TARGET.toString());
							setTargetWormholeNode(nextNode);
							// and break free
							break;
						}
					}
				
				// if the source component is not null, it's the
				// last component to be successfully created so we're done
				if (sourceComp != null)
					break;
				
			}
		}
		
		return sourceComp;
	}
	
	/**
	 * A function to extrapolate, and set, the target wormhole node
	 * if (as in a DRed map being reopened) it isn't nested in the target component.
	 * @param actualTargetMap, the target LWMap
	 * @param wr, the WormholeResource whose LWWormholeNode we are looking for
	 * @return the LWWormholeNode that contains the target wormhole resource
	 * @author Helen Oliver
	 */
	private LWWormholeNode extrapolateAndSetTargetWormholeNodeFromTargetMap(LWMap actualTargetMap,
			WormholeResource wr) {
		// input validation
		if ((actualTargetMap == null) || (wr == null))
			return null;
		
		// the putative target wormhole node
		LWWormholeNode targetWormholeNode = null;
		
		// now look at the target map and see if we can find
		// the target wormhole node
		// Start by getting all the wormhole nodes that are contained
		// within the target map
		Collection<LWWormholeNode> wormholeNodes = actualTargetMap.getAllWormholeNodes();
		// iterate through all the wormhole nodes
		for(LWWormholeNode nextNode :wormholeNodes) {
			// if the node contains a resource
			if (nextNode.hasResource()) {
				// get the resource contained in the node
				Resource r = nextNode.getResource();
					// check and see if the resource in this node is a wormhole resource
					if (r.getClass().equals(tufts.vue.WormholeResource.class)) {
						// so we need to see if that resource is the one we're looking for
						WormholeResource worm = (WormholeResource)r;
						// compare the values - if the UUIDs match in both
						// directions, then the resources are at least pointing
						// to each other
						String strWeAreLookingFor = wr.getComponentURIString();
						String strWeAreLookingAt = worm.getOriginatingComponentURIString();
						// now the other way
						String strTheyAreLookingFor = wr.getOriginatingComponentURIString();
						String strTheyAreLookingAt = worm.getComponentURIString();
						
						// if one target node has several wormholes,
						// it is possible that we could have selected the wrong
						// LWWormholeNode. There is not much we can do about that
						// in the current state. One possible way - store wormhole
						// node ID as part of the resource? Painful.
						if ((strWeAreLookingFor.equals(strWeAreLookingAt)) 
							&& (strTheyAreLookingFor.equals(strTheyAreLookingAt))) {
							nextNode.setWormholeType(LWWormholeNode.WormholeType.TARGET.toString());
							setTargetWormholeNode(nextNode);
							// the value to return
							targetWormholeNode = nextNode;
							break;
						}

					}
				
			} // end of nextNode.hasResource() if block
		} // end of nextNode loop
		
		return targetWormholeNode;
	}

	/**
	 * A function for extrapolating source and target components,
	 * maps, and nodes from a single LWWormholeNode and the
	 * WormholeResource in that node.
	 * This works even if the WormholeResource doesn't know about both
	 * sides of the wormhole.
	 * It does not work if you do this just before saving.
	 * @param wn, the LWWormholeNode that we will use to extrapolate the wormhole
	 * @param wr, the WormholeResource in wn that we will use to extrapolate the wormhole
	 * @return false if the components, maps, and nodes weren't successfully created,
	 * true if they were.
	 * @author Helen Oliver
	 */
	private boolean extrapolateComponentsMapsAndNodes(LWWormholeNode wn, WormholeResource wr) {
		// validate input
		if ((wn == null) || (wr == null))
			return false;
		// check and see if the original source and target were
		// actually pointing to the same map
		boolean bSameMap = pointsToSameMap(wr);
		
		// HO 29/05/2012 BEGIN ********
		// set the source and target maps
		LWMap actualSourceMap = extrapolateAndSetSourceMap(wn);
		// if there's no source map, we're hosed.
		if (actualSourceMap == null)
			return false;
		
		LWMap targMap = extrapolateAndSetTargetMap(bSameMap, wr);
		// if there's no target map, we're hosed.
		if (targMap == null)
			return false;
		
		LWComponent targetComp = extrapolateTargetComponent(wr, targMap);

		// now instantiate the source component which we are about to extrapolate
		// HO 29/05/2012 BEGIN ********
		LWComponent sourceComp = searchForSourceComponentInSourceMap(actualSourceMap, wr);
		// HO 06/06/2012 BEGIN **********
		// in case we have to look for this again
		LWComponent sourceCompInMap = sourceComp;
		// HO 06/06/2012 END ************
		if (targetComp == null) {
			if (sourceComp != null) {
				// we have a source component but no target,
				// and we can work with this
				setSourceWormholeNode(wn);
				return true;
			} else {
				return false;	// we have neither a source nor a target component
			}
		}		
		// HO 29/05/2012 END ********
		
		// if we got this far, we have a target component		
		setTargetComponent(targetComp);
		
		// HO 29/05/2012 BEGIN ********
		sourceComp = extrapolateAndSetSourceComponentAndWormholeNodes(targetComp, actualSourceMap, wn);
		// HO 29/05/2012 END ********
		
		// if we didn't manage to set the source component,
		// we've failed so flag failure and return
		// HO 29/05/2012 BEGIN **********
		// either that or we have a DRed map on our hands
		// in which case the sourceComp won't have been found
		// because we've been searching it on the assumption it has a parent
		if (sourceComp == null) {
			// return false;
			// check if it's there but just doesn't have a parent
			// HO 06/06/2012 BEGIN *********
			// use the one we found in the map
			if (sourceCompInMap != null) 
				sourceComp = sourceCompInMap;
			// HO 06/06/2012 END *********
			if (sourceComp == null)
				return false;
			else {	// after all that, if it's there but doesn't have a parent, this is probably a DRed map
				// so, we need to make sure the source and target wormhole nodes are set and they both have parents
				System.out.println("placeholder");
				// need to set the source component
				setSourceComponent(sourceComp);
				// set the originally passed-in wormhole node
				// to be the source wormhole node
				wn.setWormholeType(LWWormholeNode.WormholeType.SOURCE.toString());
				setSourceWormholeNode(wn);
				// at this point we most likely don't have a target wormhole node either because
				// we most likely are reading in a DRedMap, so, let's see if we can find the
				// target wormhole node in the map
				// assuming of course that we actually have a target component
				// which we should if we have got this far
				if (targetComp != null) {
					LWWormholeNode targetWormholeNode = extrapolateAndSetTargetWormholeNodeFromTargetMap(targMap, wr);
					// if we have a target component but no wormhole node, disaster! irretrievably damned
					if (targetWormholeNode == null) 
						return false;
				}
			}
		}
		
		// HO 29/05/2012 END ************

    	// if we got this far we have succeeded

    	return true;
	}
	
	/** 
	 * A function to take in a WormholeResource and return a 
	 * target file path String
	 * @param wr, the WormholeResource containing the target path
	 * @return a String representing the target file path
	 * @author Helen Oliver
	 */
	private String getTargetFilePathFromResource(WormholeResource wr) {
		// get the target file path from the resource
		String targetSpec = "";
		try {
			// get the recorded file spec from the resource
			targetSpec = wr.getSystemSpec();
		} catch(NullPointerException e) {
			return targetSpec;
		}
		
		return targetSpec;
	}
	
	/**
	 * A function to get the URI of the parent path of the source map.
	 * @return the URI of the parent path of the source map.
	 * @author Helen Oliver
	 */
	private URI getParentURIOfSourceMap() {
		// if the source map actually has a file,
		// get its parent path
		String strSourceParent = "";
		URI sourceParent = null;
		
		// as long as the source map has a file, get its parent path
		if (sourceMap.getFile() != null) {
			strSourceParent = new File(sourceMap.getFile().getAbsolutePath()).getParent();
		}
		// if the source file has a parent path, turn it into a URI
		if ((strSourceParent != null) && (strSourceParent != ""))	{
			// make sure spaces are replaced with HTML codes	
			// HO 24/02/2012 BEGIN *******
				sourceParent = VueUtil.getURIFromString(strSourceParent);
			}
		
		return sourceParent;
	}
	
	/**
	 * A function to take a possibly-absolute path for the target map,
	 * and relativize it to the source map.
	 * Use this local method when we don't necessarily
	 * know where the target map is.
	 * @param targetSpec, the possibly-absolute path for the target map
	 * @return a String representing the relative path
	 * @author Helen Oliver
	 */
	private String relativizeTargetSpecWhenTargetMapNotKnown(String targetSpec) {
		
		// HO 09/03/2012 BEGIN ******
		String strParentPath = getParentPathOfSourceMap();
		URI sourceBaseURI = getParentURIOfSourceMap();
		String strRelativizedSpec = VueUtil.relativizeUnknownTargetSpec(strParentPath, sourceBaseURI, targetSpec);
		
		return strRelativizedSpec;
	}
	

	
	/**
	 * Convenience method to return the parent path of
	 * the source map as a String.
	 * @return a String representation of the parent path
	 * of the source map.
	 * @author Helen Oliver
	 */
	private String getParentPathOfSourceMap() {
		// the source parent path-to-be
		String strSourceParent = "";
		// if the source map actually has a file,
		// get its parent path
		URI sourceParent = getParentURIOfSourceMap();
		
		// so if we have a parent, re-encode it as a String
		if (sourceParent != null) {
			strSourceParent = sourceParent.toString();
			// HO 27/02/2012 BEGIN ********			
			strSourceParent = VueUtil.decodeURIStringToString(strSourceParent);
			// HO 27/02/2012 END ********
			// HO 24/02/2012 BEGIN ******
			// HO 24/02/2012 BEGIN ********
		}
		
		return strSourceParent;
	}
 	
	/**
	 * Function to extract the target map from a given WormholeResource.
	 * Assumption is that we know the source map by this point.
	 * If the map can't be found in the stated target, we look in the local
	 * folder. Then some folders above that. Then give up.
	 * @param wr, the WormholeResource from which to extract the target map.
	 * @return targMap, the target map if successfully extrapolated, null if not.
	*/
	private LWMap extrapolateTargetMap(WormholeResource wr) {
		// validate input
		if (wr == null)
			return null;
		
		// now instantiate a target map
		LWMap targMap = null;

		// get the target file path from the resource
		String targetSpec = getTargetFilePathFromResource(wr);
		// create a URI from it
		URI targetURI = VueUtil.getURIFromString(targetSpec);

		// if it's null or an empty string, we can't
		// extrapolate anything from it
		if ((targetSpec == null) || (targetSpec == ""))
			return null;
		
		// record the original versions of the target file path and spec
		String originalTargetSpec = targetSpec;
		URI originalTargetURI = targetURI;
		
		// now we extrapolate the target component
		// by getting the component URI string from the resource
		String compString = wr.getComponentURIString();
		// if the resource doesn't have a component string it's broken
		if ((compString == null) || (compString == ""))
			return null;
		
		targMap = findTargetMap(targetSpec, compString);
			
		return targMap;
	}
	
	/**
	 * A function to find the target map, relativized,
	 * and searching within several folders above and below the source
	 * @param targetSpec
	 * @param compString
	 * @return the LWMap that is the target map
	 */
	private LWMap findTargetMap(String targetSpec, String compString) {
		// input validation
		if ((targetSpec == null) || (targetSpec == ""))
			return null;
		
		if ((compString == null) || (compString == ""))
			return null;
		
		// the target map to return
		LWMap targMap = null;
		
		// get the source file's parent path
		String strSourceParent = getParentPathOfSourceMap();	
		
		// HO 09/03/2012 BEGIN *******
		// make sure the spec is formatted as a String
		targetSpec = VueUtil.decodeURIStringToString(targetSpec);
		// make sure the slashes are going in the same direction
		targetSpec = VueUtil.switchSlashDirection(strSourceParent, targetSpec);
		// HO 09/03/2012 END *********
		
		// create a URI from it
		URI targetURI = VueUtil.getURIFromString(targetSpec);
				
		// record the original versions of the target file path and spec
		String originalTargetSpec = targetSpec;
		URI originalTargetURI = targetURI;
		
		// FIRST ATTEMPT
		// try to open the file in situ
		File targFile = new File(targetSpec);
		// HO 07/03/2012 BEGIN *******
		// the file in the next-most-likely location
		File nextLikelyFile = targFile;
		// HO 07/03/2012 END *********
		// just the name of the target
		String strTargName = targFile.getName();
		
		// Start with the file in the originally-stated target location.
		if (targFile.isFile()) {
			// if there's a file there, see if it contains the target node.
			targMap = VueUtil.checkIfMapContainsTargetNode(targFile, compString);
			// if we found the map, return it
			if (targMap != null)
				return targMap;
		}
		
		// SECOND ATTEMPT
		// if we couldn't find the file with the target node in
		// the originally-stated location,
		// try to relativize the target path based on the source path
		// HO 02/03/2012 BEGIN *********	
		String basePath = sourceMap.getFile().getAbsolutePath();
		String pathSeparator = System.getProperty("file.separator");
		// HO 09/03/2012 BEGIN *********
		// String relativeTargetSpec = VueUtil.getRelativePathByStringManipulation(targetSpec, basePath, pathSeparator);
		String strRelativeTargetSpec = relativizeTargetSpecWhenTargetMapNotKnown(targetSpec);
		// HO 09/03/2012 END *********
		// if we actually got back a relative location, use it
		if (strRelativeTargetSpec != "") {
			// update the target spec and URI
			// to reflect the relativization
			targetSpec = strRelativeTargetSpec;
			targetURI = VueUtil.getURIFromString(targetSpec);
			// now try to instantiate the file in situ,
			// which won't likely work
			// HO 07/03/2012 BEGIN ********
			//targFile = new File(targetSpec);
			try {
				nextLikelyFile = new File(targetSpec);
			} catch (IllegalArgumentException e) {
				nextLikelyFile = null;
			}
			// HO 07/03/2012 END ********
		}
		// if it's a valid file, check and see if the target node
		// exists in it
		// HO 07/03/2012 BEGIN ********
		//if (targFile.isFile()) {
		if ((nextLikelyFile != null) && (nextLikelyFile.isFile())) {
			// targMap = VueUtil.checkIfMapContainsTargetNode(targFile, compString);
			targMap = VueUtil.checkIfMapContainsTargetNode(nextLikelyFile, compString);
			// HO 07/03/2012 END ********			
			// if we found the map, return it
			if (targMap != null) {
				return targMap;
			} else {
				// if the previous file in the originally
				// stated location was valid but didn't contain
				// the target node, it's more likely to be the
				// right file than this relativized one which
				// probably couldn't even be instantiated.
				// But if there is no file from the originally
				// stated location, prioritize the one in
				// this location.
				if (!targFile.isFile()) {
					targFile = nextLikelyFile;
				}
			}
		}
				
		// THIRD ATTEMPT
		// if that file can't be found after relativizing it
		// to the source, try resolving it against the source
		if ((!targFile.isFile()) || (targMap == null)) {
			// HO 07/03/2012 BEGIN ********
			// targFile = VueUtil.resolveTargetRelativeToSource(targetURI, getParentURIOfSourceMap());
			nextLikelyFile = VueUtil.resolveTargetRelativeToSource(targetURI, getParentURIOfSourceMap());
			// HO 07/03/2012 END ********
		}
		
		// if it's a valid file, check and see if the target node
		// exists in it
		// HO 07/03/2012 BEGIN ********
		//if (targFile.isFile()) {
		if ((nextLikelyFile != null) && (nextLikelyFile.isFile())) {		
			// targMap = VueUtil.checkIfMapContainsTargetNode(targFile, compString);
			targMap = VueUtil.checkIfMapContainsTargetNode(nextLikelyFile, compString);
			// HO 07/03/2012 END ********
			// if we found the map, return it
			if (targMap != null) {
				return targMap;
			} else {
				// this is the most likely place to find
				// a valid file
				targFile = nextLikelyFile;
			}
		}
		
		// FOURTH ATTEMPT	
		//if we still can't find the file, check for one with the same name
		// in the local folder
		try {
			if ((!targFile.isFile()) || (targMap == null)) {
				if ((strSourceParent != null) && (strSourceParent != "")) {	
					// if we got the parent path, create a file out of it
					// HO 07/03/2012 BEGIN ********
					//targFile = new File(strSourceParent, strTargName);
					nextLikelyFile = new File(strSourceParent, strTargName);
					// if it's a valid file, check and see if the target node
					// exists in it
					//if (targFile.isFile()) {
					if ((nextLikelyFile != null) && (nextLikelyFile.isFile())) {
						// targMap = VueUtil.checkIfMapContainsTargetNode(targFile, compString);
						targMap = VueUtil.checkIfMapContainsTargetNode(nextLikelyFile, compString);
						// HO 07/03/2012 END *********
						// if we found the map, return it
						if (targMap != null) {
							return targMap;
						} else { // the relativized target file is more likely to be valid
							if (!targFile.isFile()) // but if it doesn't exist, this one becomes the priority
								targFile = nextLikelyFile;
						}
					}
				}
				else // but if there isn't a parent path, we can't do anything, so return null
					return null;
			} 
		} catch (Exception e) {
			// do nothing, just return null
			return null;
		}
		
		// only proceed now if we don't have a valid target file at all
		if (!targFile.isFile()) {
			// FIFTH ATTEMPT
			// if we still can't find it in the local folder, 
			// search all the subfolders,
			// but do it without opening a can of worms
			// HO 07/03/2012 BEGIN *******
			// targMap = VueUtil.findTargetInSubfolders(strSourceParent, strSourceParent, strTargName, compString);
			nextLikelyFile = VueUtil.lazyFindTargetInSubfolders(strSourceParent, strSourceParent, strTargName);
			if ((nextLikelyFile != null) && (nextLikelyFile.isFile())) {
				targFile = nextLikelyFile;
				String s = nextLikelyFile.getAbsolutePath();
				// HO 03/05/2012 BEGIN ******
				VueUtil.saveMapIfAlreadyOpen(targFile);
				// HO 03/05/2012 END ********
				targMap = OpenAction.loadMap(s);
				// HO 07/03/2012 END *******					
				// if we found the map, return it
				if (targMap != null)
					return targMap;
			}
		}
		
		// only proceed now if we don't have a valid target file at all
		if (!targFile.isFile()) {
			// SIXTH ATTEMPT
			// if we couldn't find it in the subfolders,
			// search the above-folders (will not look through any of their subfolders)
			// HO 07/03/2012 BEGIN *******
			// targMap = VueUtil.findTargetAboveCurrentPath(strSourceParent, strSourceParent, strTargName, compString);
			nextLikelyFile = VueUtil.lazyFindTargetAboveCurrentPath(strSourceParent, strSourceParent, strTargName);
			if ((nextLikelyFile != null) && (nextLikelyFile.isFile())) {
				targFile = nextLikelyFile;
				String s = nextLikelyFile.getAbsolutePath();
				// HO 03/05/2012 BEGIN ******
				VueUtil.saveMapIfAlreadyOpen(targFile);
				// HO 03/05/2012 END ********
				targMap = OpenAction.loadMap(s);
				// HO 07/03/2012 END *******
				// if we found the map, return it
				if (targMap != null)
					return targMap;
			}
		}

		// if we're still null at this point, we're hosed
		return targMap;
	}
	
	/**
	 * A function to extrapolate a wormhole when a WormholeNode
	 * is being reparented from one Node to another
	 * @param wn, the WormholeNode that is being reparented
	 * @param wr, the WormholeResource in the wormhole node
	 * @param prevURI, the URI of the previous parent node
	 * @param newParent, the new parent LWComponent
	 * @return
	 */
	private boolean extrapolateDuringReparent(LWWormholeNode wn, WormholeResource wr, String prevURI, LWComponent newParent) {
		// input validation
		if ((wn == null) || (wr == null) || (newParent == null))
			return false;
		
		// set the new parent to be the source component
		setSourceComponent(newParent);
		
		// Start by extrapolating the source map from the node parent
		LWMap actualSourceMap = null;
		try {
			actualSourceMap = wn.getParentOfType(LWMap.class);
		} catch (NullPointerException e) {
			return false;
		}
		// if the node has a parent map, make it the source map
		if (actualSourceMap != null) {
			// make sure the source map is saved
			// HO 16/04/2012 BEGIN *******
    		// SaveAction.saveMap(actualSourceMap);
    		// HO 16/04/2012 END *******
			// set it as the source map
			setSourceMap(actualSourceMap);
		} else {
			return false;
		}
		
		// now we extrapolate the target component
		// by getting the component URI string from the resource
		String compString = wr.getComponentURIString();
		// if the component URI string is null or empty,
		// we can't extrapolate enough information to create
		// the wormhole, so flag failure and return
		if ((compString == null) || (compString == ""))
			return false;
		
		// now instantiate a target map
		LWMap targMap = null;
		
		// if this points to the same map, easy
		if (pointsToSameMap(wr)) {
			targMap = actualSourceMap;
			setTargetMap(targMap);
		} else {						
			// first we extrapolate the target
			// just by using the resource we have
			// (assume this is correct)
			String targetSpec = "";
			try {
				targetSpec = wr.getSystemSpec();
				// if the spec was not set, replace it with the last known filename
				if (targetSpec.equals(wr.SPEC_UNSET))
					targetSpec = wr.getTargetFilename();
								
				// if it's null or an empty string, we can't
				// extrapolate anything from it
				if ((targetSpec == null) || (targetSpec == ""))
					return false;

			} catch(NullPointerException e) {
				return false;
			}

			// strip away wrongly-formatted spaces
			targetSpec = VueUtil.stripHtmlSpaceCodes(targetSpec);	
			targMap = findTargetMap(targetSpec, compString);

			// if we now have a target map
			if (targMap != null)
				// set it as our target map component
				setTargetMap(targMap);
			else
				// but if we don't have a map, we failed
				return false;
		}
				
		// but if we do have a component URI string, we can
		// use it to find the target component in the target map
		LWComponent targetComp = targMap.findChildByURIString(compString);
		// if we didn't get a target component out of this, 
		// we can't build a wormhole, so flag failure
		// and return
		if (targetComp == null)
			return false;
		
		// if we got this far, we have a target component
		setTargetComponent(targetComp);
		
		// not sure it helps to set wormhole types any more
		wn.setWormholeType(LWWormholeNode.WormholeType.SOURCE.toString());
		// but set the passed-in node as the source node
		setSourceWormholeNode(wn);
		
		// now look at the target component and see if we can extrapolate
		// whether it's the right one (just in case the component has more
		// than one wormhole node
		// Start by getting all the wormhole nodes that are contained
		// within the target component
		Collection<LWWormholeNode> wormholeNodes = targetComp.getAllWormholeNodes();
		// iterate through all the wormhole nodes
		for(LWWormholeNode nextNode :wormholeNodes) {
			// if the node contains a resource
			if (nextNode.hasResource()) {
				// get the resource contained in the node
				Resource r = nextNode.getResource();
					// check and see if the resource in this node is a wormhole resource
					if (r.getClass().equals(tufts.vue.WormholeResource.class)) {
						// okay now we have to make sure that it was supposed to
						// be pointing towards the original parent component,
						// because if there's more than one resource,
						// or if there was a resource but it went missing,
						// we could be changing the values on the wrong resource
						// Downcast the resource to a wormhole resource
						WormholeResource worm = (WormholeResource)r;
						// Get the component the resource is pointing to
						// by searching the source map for a component with
						// this URI string
						//sourceComp = actualSourceMap.findChildByURIString(worm.getComponentURIString());
						if (worm.getComponentURIString().equals(prevURI)) {
							//setSourceComponent(sourceComp);
							// set the originally passed-in wormhole node
							// to be the source wormhole node
							//setSourceWormholeNode(wn);
							nextNode.setWormholeType(LWWormholeNode.WormholeType.TARGET.toString());
							// set the target wormhole node to be the node
							// we just got the wormhole resource out of
							setTargetWormholeNode(nextNode);
							// and break free
							break;
						}
					}
			}
		} 
		
		// if we didn't manage to set the target node,
		// we've failed so flag failure and return
		if (targetWormholeNode == null)
			return false;

    	// if we got this far we have succeeded
    	return true;
	}
	
	/**
	 * Ask the user to save the source map.
	 * @param map, the map we are trying to save
	 * @return true if the map was saved successfully,
	 * false otherwise
     * Returns true if either they save it or say go ahead and close w/out saving.
     */
	private LWMap askSaveSourceMap(LWMap map, Component c) {
    	// Give them a choice of Yes (0), or a No (1) labelled Cancel (2) 
    	final Object[] defaultOrderButtons = { "Save As...", VueResources.getString("optiondialog.revertlastsave.cancel"),VueResources.getString("optiondialog.savechages.save")};
    	final Object[] macOrderButtons = { VueResources.getString("optiondialog.savechages.save"),VueResources.getString("optiondialog.revertlastsave.cancel"), "Save As..."};

        // HO 13/08/2010 BEGIN
    	// we don't care if they have made changes or not,
    	// the point is that we make sure they know they're saving this as the
    	// source map by forcing them to go through the save process 	
    	// if (!map.isModified() || !map.hasContent())
            // return true;
    	// HO 13/08/2010 END
        
        int response = VueUtil.option
            (c,
             "Do you want to save "
             + " '" + map.getLabel() + "' as your source map?",
             "Save source map?",
             JOptionPane.YES_NO_CANCEL_OPTION,
             JOptionPane.PLAIN_MESSAGE,
             Util.isMacPlatform() ? macOrderButtons : defaultOrderButtons,             
             VueResources.getString("optiondialog.savechages.save")
             );
        
     
        if (!Util.isMacPlatform()) {
            switch (response) {
            // the NO_OPTION (add to same map)
            case 0: response = 1; break;
            // the CANCEL option
            case 1: response = 2; break;
            // the YES_OPTION (choose a target map)
            case 2: response = 0; break;
            }
        } else {
            switch (response) {
            // the YES_OPTION (choose a target map)
            case 0: response = 0; break;
            // the CANCEL option
            case 1: response = 2; break;
            // the NO_OPTION (add to same map)
            case 2: response = 1; break;
            }
        }
        
        if (response == JOptionPane.YES_OPTION) { // Save
        	// save the map
        	return SaveAction.saveMapSpecial(map, false, false, "Save Source Map");
        } else if (response == JOptionPane.NO_OPTION) { // Save As
            // save not necessarily in the default location
        	return SaveAction.saveMapSpecial(map, true, false, "Save Source Map");
        } else // anything else (Cancel or dialog window closed)
            return null;
    }
    
    /**
     * A function to prompt the user to select a new map
     * into which to target this wormhole
     * @param map, the LWMap we are currently in
     * @return the map selected by the user
     */
	private LWMap askSelectNewTargetMap(LWMap map) {
		// save silently
		LWMap newTargetMap = SaveAction.saveMapSpecial(map, true, false, "Save Target Map");
		// if the map exists
    	if (newTargetMap != null) {
    		// now we have our target map
    		return newTargetMap;
    	} else {
    		return null;
    	}

    }	    
	
    /**
     * A routine to prepare the screen to show a dialog box
     * @param comp, the LWComponent we are currently in
     * @return c, the Component that is a parent for a dialog box
     */
    private Component setScreen(LWComponent comp) {
    	// input validation
    	if (comp == null)
    		return null;
    	
        // todo: won't need this if full screen is child of root frame
        if (VUE.inNativeFullScreen())
            VUE.toggleFullScreen();
        
        Component c = VUE.getDialogParent();
        
        if (VUE.getDialogParent() != null)
        {
        	//Get the screen size
        	Toolkit toolkit = Toolkit.getDefaultToolkit();
        	Dimension screenSize = toolkit.getScreenSize();
        	Point p = null;
            try {
            	p = c.getLocation(null);
            } catch (Exception e) {
            	c = null;
            }
        	
        	if ((p.x + c.getWidth() > screenSize.width) ||
        			(p.y + c.getHeight() > screenSize.height))
        	{
        		c = null;
        	}
        }
        
        return c;
    }
    
    /**
     * A routine to prepare the screen to show a dialog box
     * @param map, the LWMap we are currently in
     * @return c, the Component that is a parent for a dialog box
     */
    private Component setScreen(LWMap map) {
    	// input validation
    	if (map == null)
    		return null;
    	
        // todo: won't need this if full screen is child of root frame
        if (VUE.inNativeFullScreen())
            VUE.toggleFullScreen();
        

        Component c = VUE.getDialogParent();
        
        if (VUE.getDialogParent() != null)
        {
        	//Get the screen size
        	Toolkit toolkit = Toolkit.getDefaultToolkit();
        	Dimension screenSize = toolkit.getScreenSize();

        	Point p = null;
            try {
            	p = c.getLocation();
            } catch (Exception e) {
            	c = null;
            }
        	
        	if ((p.x + c.getWidth() > screenSize.width) ||
        			(p.y + c.getHeight() > screenSize.height))
        	{
        		c = null;
        	}
        }
        
        final String debug;

        if (DEBUG.EVENTS || DEBUG.UNDO)
            debug = "\n[modifications="+map.getModCount()+"]";
        else
            debug = "";
        
        return c;
    }
    
    /**
     * A function to get the user to select an existing map into which
     * to target the wormhole.
     * @param map, the LWMap we are in now
     * @return the map selected by the user.
     */
    private LWMap askSelectExistingTargetMap(LWMap map) {
    	final Object[] defaultOrderButtons = { "Same Map", "Cancel", "Choose Target Map"};
    	final Object[] macOrderButtons = { "Choose Target Map", "Cancel", "Same Map"};

    	Component c = setScreen(sourceComponent);
        
        int response = VueUtil.option
            (c,
             "Please choose a target map.",
             "Choose Target Map",
             JOptionPane.YES_NO_CANCEL_OPTION,
             JOptionPane.PLAIN_MESSAGE,
             Util.isMacPlatform() ? macOrderButtons : defaultOrderButtons,             
             VueResources.getString("Choose Target")
             );
        
     
        if (!Util.isMacPlatform()) {
            switch (response) {
            // the NO_OPTION (add to same map)
            case 0: response = 1; break;
            // the CANCEL option
            case 1: response = 2; break;
            // the YES_OPTION (choose a target map)
            case 2: response = 0; break;
            }
        } else {
            switch (response) {
            // the YES_OPTION (choose a target map)
            case 0: response = 0; break;
            // the CANCEL option
            case 1: response = 2; break;
            // the NO_OPTION (add to same map)
            case 2: response = 1; break;
            }
        }
        
        if (response == JOptionPane.YES_OPTION) { // Save
            // choose a target map
        	LWMap newTargetMap = openExistingMap();
        	return newTargetMap;
        } else if (response == JOptionPane.NO_OPTION) { // Same Map
            // place the target component in the current map
            SaveAction.saveMap(map, false, false);
        	return map;
        } else // anything else (Cancel or dialog window closed)
            return null;
    }	
    
    /**
     * A function to prompt the user to select and open an existing map.
     * @return the LWMap the user chose to open.
     */
    private LWMap openExistingMap() {
    	// prompt the user to open the map
    	// default file type is now .vpk
    	// HO 23/03/2012 BEGIN *******
    	// default file type is now .vdk
    	// File file = ActionUtil.openFile("Open Map", VueFileFilter.VPK_DESCRIPTION);
    	File file = ActionUtil.openFile("Open Map", VueFileFilter.VDK_DESCRIPTION);
    	// HO 23/03/2012 END ********
        
    	// if they didn't open a map for any reason
        if (file == null)
            return null;
        
        // HO 03/05/2012 BEGIN *******
        // see if this existing map is already open
        // and if it is, save it
        VueUtil.saveMapIfAlreadyOpen(file);        
        // HO 03/05/2012 END *********
        
        // load the map file
        LWMap theMap = OpenAction.loadMap(file.getAbsolutePath());

        // return the map file we have loaded    
        return theMap; 
        
    } 
    
	/**
	 * A method to create and set the wormhole nodes 
	 * and give them temporary labels
	 */
    private void createWormholeNodes() {
    	// create the source wormhole node
    	setSourceWormholeNode(createSourceWormholeNode());
    	// create the target wormhole node
    	setTargetWormholeNode(createTargetWormholeNode());    	
    	
    	// label them (dummy labels for now)
    	// HO 16/04/2012 BEGIN ********
    	// setWormholeNodeLabel(getTargetWormholeNode(), VueResources.getString("wormhole.node.target.label.default"));
    	labelWormholeNodes();
    	// HO 16/04/2012 END ********
	}
    
    /**
     * A method to label the source and target wormholes
     * with the labels of their respective target maps
     */
    private void labelWormholeNodes() {
    	if ((sourceWormholeNode != null) && (targetMapFile != null))
    		sourceWormholeNode.setLabel(targetMapFile.getName());
    	if ((targetWormholeNode != null) && (sourceMapFile != null))
    		targetWormholeNode.setLabel(sourceMapFile.getName());
    }
	
	/**
	 * A method to add the wormhole nodes to their components
	 * and place the target node in the target map
	 */
    private void placeComponents() {

    	// add the wormhole nodes to their components;
    	placeWormholeNodesAsChildren();
    	
    	// okay, now place the target node in the target map
    	positionTargetComponent(targetMap);		
	}
	
	/**
	 * Creates the target component by duplicating
	 * the source component.
	 * @return the LWComponent that will be the target component.
	 */
	public LWComponent createDuplicateTargetComponent() {
		// just duplicate the source component
    	LWComponent theComponent = sourceComponent.duplicate();
    	return theComponent;
	}
	
	/**
	 * Creates the target component by instantiating a default LWNode
	 * @return the LWNode that will be the target component.
	 */
	public LWNode createDefaultTargetNode() {
		String strLabel = VueResources.getString("wormhole.node.label.default");
		LWNode theNode = new LWNode(strLabel);
		return theNode;
	}
	
	// HO 28/05/2012 BEGIN *********
	public LWNode createDefaultTargetNodeRHB() {
		LWNode theNode = new LWNode("Element Decomposed");
		return theNode;
	}	
	// HO 28/05/2012 END ***********
	
	/**
	 * A routine to paste the target component in the
	 * default location in the target map.
	 * @param theMap, the target LWMap.
	 * @author Helen Oliver
	 */
	private void pasteTargetComponent(LWMap theMap) {
		// input validation
		if (theMap != null) {
			// paste the target component in the default location
			// in the target map
			theMap.pasteChild(targetComponent);
		}
	}
	
	/**
	 * A routine to check and see if a given map is open,
	 * and if it is, to repaint it.
	 * @param theMap, the LWMap to repaint if it's open.
	 * @author Helen Oliver
	 */
	private void repaintMapIfOpen(LWMap theMap) {
		// input validation
		if (theMap == null)
			return;
        
		// get the viewer the map is in, if any
		MapViewer viewer = VUE.getCurrentTabbedPane().getViewerWithMap(theMap);
		// if there is a viewer, repaint it
        if (viewer != null)
        	viewer.repaint();
	}
	
	/**
	 * A routine to ripple out a selection of nodes.
	 * @param selection, an LWSelection containing the set of nodes to ripple out.
	 * @author Helen Oliver
	 */
	private void rippleOutNodes(LWSelection selection) {
		// input validation
		if (selection == null)
			return;
		
		// get the layout
		Layout layout = new edu.tufts.vue.layout.ListRandomLayout();
		
        // cycle through the set of nodes, which will be an artificial
		// selection consisting of the target node and one other node
		Iterator i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            try {
            	// ripple out the nodes
                layout.layout(selection);
            } catch(Throwable t) {
                Log.debug("LWWormhole.rippleOutNodes: "+t.getMessage());
                 tufts.Util.printStackTrace(t);
            }

        }
	}
	
	/**
	 * A function to create an LWSelection containing the target node
	 * and one other node.
	 * @param nextNode, the other LWNode to add to the selection.
	 * @return selection, the LWSelection containing the target node and one other node.
	 * @author Helen Oliver
	 */
	private LWSelection selectTargetAndOneOtherNode(LWComponent nextNode) {
		// instantiate selection
        LWSelection selection = new LWSelection();
        // if the other node is valid, add it to the selection
        if (nextNode != null)
        	selection.add(nextNode);
        // if the target node exists, add it to the selection
        if (targetComponent != null)
        	selection.add(targetComponent);
        
        // return the selection
        return selection;
	}
	
	/**
	 * A routine to make sure the target node doesn't overlap
	 * with any existing nodes
	 * @param theMap, the target map
	 * @author Helen Oliver
	 */
	private void makeSureTargetNodeDoesNotOverlap(LWMap theMap) {
		// input validation
		if (theMap == null)
			return;
		
		// get all the nodes in the target map
		Iterator iter = targetMap.getChildIterator();
		// cycle through all the nodes in the target map
		while (iter.hasNext()) {			
			LWComponent nextComp = (LWComponent) iter.next();			
			// if the next component isn't the target node
			if (nextComp != targetComponent) {
					rippleOutOverlap(nextComp, targetMap);
				}
		}

	}
	
	/**
	 * A routine to detect components overlapping another
	 * component, and ripple them out.
	 * @param nextNode, the component that may be overlapping
	 * @param theMap
	 */
	private void rippleOutOverlap(LWComponent nextNode, LWMap theMap) {
		if (nextNode.getClass() == LWMap.Layer.class) {
			LWMap.Layer theLayer = (LWMap.Layer) nextNode;
			rippleOutLayer(theLayer, theMap);
			return;
		} 
		
		// check that it isn't overlapping with the target node
		boolean bOverlapping = VueUtil.checkCollision(nextNode, targetComponent);
		if (!bOverlapping)
			bOverlapping = VueUtil.checkCollision(targetComponent, nextNode);
		// but if it does overlap
		if (bOverlapping) {
			// create an artificial selection containing this node and the target node
            LWSelection selection = selectTargetAndOneOtherNode(nextNode);
            // ripple them out
            rippleOutNodes(selection);
            // if the target map is open, repaint it
            repaintMapIfOpen(theMap);
		}
	}
	
	/**
	 * A routine to iterate through the components in a layer
	 * and make sure they're not overlapping the target node
	 * @param theLayer
	 * @param theMap
	 */
	private void rippleOutLayer(LWMap.Layer theLayer, LWMap theMap) {
		// input validation
		if ((theLayer == null) || (theMap == null))
			return;
		
		// iterate through all the components in the layer
		Iterator iter = theLayer.getChildIterator();
		while (iter.hasNext()) {
			LWComponent nextComp = (LWComponent) iter.next();
			// we don't want to bother rippling out links at this stage
			if (nextComp.getClass() != LWLink.class) {
				rippleOutOverlap(nextComp, targetMap);
			}
		}
	}
	
	/**
	 * A stub for functionality to position the target component
	 * in the target map.
	 * @param parentMap, the map into which to paste this component.
	 */
	public void positionTargetComponent(LWMap parentMap) {
		pasteTargetComponent(parentMap);
		makeSureTargetNodeDoesNotOverlap(parentMap);		
	}
	
	/**
	 * Creates the target map by 
	 * finding which map the target component is in.
	 * @return theMap, the target map.
	 */
	public LWMap createTargetMap() {
		// get the map the target component is in
    	LWMap theMap = targetComponent.getParentOfType(LWMap.class);
    	return theMap;
	}
	
	/**
	 * Creates the source wormhole node.
	 * @return theWormholeNode, the wormhole node that will be the source wormhole.
	 */
	public LWWormholeNode createSourceWormholeNode() {
		// Use these to construct the source wormhole node
    	LWWormholeNode theWormholeNode = new LWWormholeNode(sourceComponent.getURI().toString(),
    			sourceMap.getURI().toString(), targetComponent.getURI().toString(),
    			targetMap.getURI().toString(), LWWormholeNode.WormholeType.SOURCE);
    	
    	// HO 20/04/2012 BEGIN *****
    	theWormholeNode.setFillColor(sourceComponent.getFillColor());
    	// HO 20/04/2012 END *******
    	
    	return theWormholeNode;
	}
	
	/**
	 * Creates the target wormhole node.
	 * @return theWormholeNode, the wormhole node that will be the target wormhole.
	 */
	public LWWormholeNode createTargetWormholeNode() {
		// Use these to construct the target wormhole node
    	LWWormholeNode theWormholeNode = new LWWormholeNode(sourceComponent.getURI().toString(),
    			sourceMap.getURI().toString(), targetComponent.getURI().toString(),
    			targetMap.getURI().toString(), LWWormholeNode.WormholeType.TARGET);
    	
    	// HO 20/04/2012 BEGIN *****
    	theWormholeNode.setFillColor(VueResources.getColor("node.fillColor"));
    	// HO 20/04/2012 END *******
    	
    	return theWormholeNode;
	}
	
	// HO 28/05/2012 BEGIN *********
	public LWWormholeNode createTargetWormholeNodeRHB() {
		// Use these to construct the target wormhole node
    	LWWormholeNode theWormholeNode = new LWWormholeNode(sourceComponent.getURI().toString(),
    			sourceMap.getURI().toString(), targetComponent.getURI().toString(),
    			targetMap.getURI().toString(), LWWormholeNode.WormholeType.TARGET);
    	
    	// HO 20/04/2012 BEGIN *****
    	theWormholeNode.setFillColor(targetComponent.getFillColor());
    	// HO 20/04/2012 END *******
    	
    	return theWormholeNode;
	}
	// HO 28/05/2012 END ***********
	
	/**
	 * Convenience method for setting the labels on the source and target
	 * components.
	 * @param sourceLabel the label to set on the source component.
	 * @param targetLabel the label to set on the target component.
	 */
	public void setComponentLabels(String sourceLabel, String targetLabel) {
		setComponentLabel(sourceComponent, sourceLabel);
		setComponentLabel(targetComponent, targetLabel);
	}
	
	/**
	 * Convenience method for setting the labels on the source and target
	 * wormhole nodes.
	 * @param sourceLabel the label to set on the source wormhole node.
	 * @param targetLabel the label to set on the target wormhole node.
	 */
	public void setWormholeNodeLabels(String sourceLabel, String targetLabel) {
		setWormholeNodeLabel(sourceWormholeNode, sourceLabel);
		setWormholeNodeLabel(targetWormholeNode, targetLabel);
	}	
	
	/**
	 * @param theComponent, the component we want to label
	 * @param theLabel, the label we want to put on the component
	 */
	public void setComponentLabel(LWComponent theComponent, String theLabel) {
		if(!theComponent.equals(null)) {
			theComponent.setLabel(theLabel);
		}
	}
	
	/**
	 * @param theWormholeNode, the wormhole node we want to label
	 * @param theLabel, the label we want to put on the wormhole node
	 */
	public void setWormholeNodeLabel(LWWormholeNode theWormholeNode, String theLabel) {
		if(!theWormholeNode.equals(null)) {
			theWormholeNode.setLabel(theLabel);
		}
	}	
	
	/**
	 * Convenience method for placing the source and target wormhole nodes as children
	 * within the source and target parent components respectively.
	 */
	public void placeWormholeNodesAsChildren() {
		placeWormholeNodeAsChild(sourceComponent, sourceWormholeNode);
		placeWormholeNodeAsChild(targetComponent, targetWormholeNode);
	}
	
	/**
	 * Positions a wormhole node as a child within a component.
	 * @param theComponent, the parent component for this wormhole node.
	 * @param theWormholeNode, the wormhole node to place within a parent component.
	 */
	public void placeWormholeNodeAsChild(LWComponent theComponent, LWWormholeNode theWormholeNode) {
		if ((!theComponent.equals(null)) && (!theWormholeNode.equals(null))) {
			// HO 20/04/2012 BEGIN *******
			// theComponent.addChild(theWormholeNode);
			theComponent.dropChild(theWormholeNode);
			// HO 20/04/2012 END *******
		}
	}
	
    /**
     * Convenience method for setting the source and target map files.
     */
	public void setSourceAndTargetMapFiles() {
    	setSourceMapFile(sourceMap.getFile());
    	setTargetMapFile(targetMap.getFile());	
    }
	
	/**
	 * Convenience method to set the source and target wormhole node resource URIs.
	 * Remember that the source node's resource contains the URI of the target map file,
	 * and vice versa.
	 */
	public void setResourceURIs() {
		try {
			// relativize both in terms of the source
			// HO 02/03/2012 BEGIN ************
			//String strRelativeTarget = VueUtil.relativizeTargetSpec(sourceMap, targetMap);
			String strSeparator = System.getProperty("file.separator");
			String strTargetPath = targetMap.getFile().getAbsolutePath();
			String strSourcePath = sourceMap.getFile().getAbsolutePath();
			// HO 09/03/2012 BEGIN ******
			// String strRelativeTarget = VueUtil.getRelativePathByStringManipulation(strTargetPath, strSourcePath, strSeparator);			
			String strRelativeTarget = VueUtil.relativizeTargetSpec(sourceMap, targetMap);			
			String strRelativeSource = VueUtil.relativizeTargetSpec(targetMap, sourceMap);
			// String strRelativeSource = VueUtil.getRelativePathByStringManipulation(strSourcePath, strTargetPath, strSeparator);
			// HO 09/03/2012 END ******
			// HO 02/03/2012 END **************
			// encode as URIs
			// the URIs-to-be
			URI relativeTargetURI = null;
			URI relativeSourceURI = null;
			
			if (strRelativeTarget != "") {
				strRelativeTarget = VueUtil.encodeStringForURI(strRelativeTarget);
				try {
					relativeTargetURI = new URI(strRelativeTarget);
				} catch (URISyntaxException e) {
					e.printStackTrace();				
				}
			}
			if (strRelativeSource != "") {
				strRelativeSource = VueUtil.encodeStringForURI(strRelativeSource);
				try {
					relativeSourceURI = new URI(strRelativeSource);
				} catch (URISyntaxException e) {
					e.printStackTrace();				
				}
			}
			
			// if we didn't get a URI out of this,
			// just use the absolute files
			if (relativeTargetURI == null) {
				setSourceResourceMapURI(targetMapFile.toURI());
				
			} else {
				// otherwise use the relative ones
				setSourceResourceMapURI(relativeTargetURI);
			}
			
			// if we didn't get a URI out of this,
			// just use the absolute files
			if (relativeSourceURI == null) {
				setTargetResourceMapURI(sourceMapFile.toURI());
			} else {
				// otherwise use the relative ones
				setTargetResourceMapURI(relativeSourceURI);
			} 
			

		// now set the component URIs
		// we don't want to abandon the wormhole
		// if all that's missing is the target 
		// component, we want to instead give
		// an informative error message.
		if (targetComponent != null) {
			setSourceResourceComponentURI(targetComponent.getURI());
		} else {
			String strMissingTargetNode = "NOTFOUND";
			URI missingURI = null;
			try {
				missingURI = new URI(strMissingTargetNode);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setSourceResourceComponentURI(missingURI);
		}
			
		setTargetResourceComponentURI(sourceComponent.getURI());
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		}
	}
	
	/**
	 * Convenience method to set the source and target resources.
	 */
	public void createResources() {

		setSourceResource(sourceWormholeNode.getResourceFactory().get(sourceResourceMapURI, sourceResourceComponentURI,
				sourceMapFile.toURI(), targetResourceComponentURI));

		if (targetWormholeNode != null) {
			setTargetResource(targetWormholeNode.getResourceFactory().get(targetResourceMapURI, targetResourceComponentURI,
					targetMapFile.toURI(), sourceResourceComponentURI));
		} else {
			// have to use the source wormhole node's resource factory
			// if the real one is not available
			setTargetResource(sourceWormholeNode.getResourceFactory().get(targetResourceMapURI, targetResourceComponentURI,
					sourceResourceMapURI, sourceResourceComponentURI));			
		}
	}
	
	/**
	 * Convenience method to add the resources to the wormhole nodes.
	 */
	public void setResources() {
		VUE.setActive(LWComponent.class, this, null);
		sourceWormholeNode.setResource(sourceResource);
		VUE.setActive(LWComponent.class, this, sourceComponent);
		VUE.setActive(LWComponent.class, this, null);
		targetWormholeNode.setResource(targetResource);
		VUE.setActive(LWComponent.class, this, targetComponent);
	}
	
	/**
	 * @param theMap, the map to which to add the listener
	 * Listens for changes to the map's filename so that
	 * the resources can be updated to reflect such
	 * Note: this listener will not be added to a map
	 * that is restored from persisted
	 */
	private void addListenerToMap(LWMap theMap) {
		if (theMap == null)
			return;
		
		theMap.addLWCListener(new LWComponent.Listener() {
			public void LWCChanged(LWCEvent e) {
					// check we're not getting into an infinite loop here
					// if the latest file matches the stored file,
					// nothing has really changed
					boolean bFileChanged = isMapFileChanged();
					if(bFileChanged == false)
						return;
					
					resetResources();
			}}); 
	}
	
	/**
	 * resets the wormhole node resources by
	 * setting the resource URIs, creating the resources,
	 * and setting them in the wormhole nodes
	 */
	public void setWormholeResources() {
		
		// set the resource URIs
		setResourceURIs();
		// create the resources
		createResources();
		// set the resources in the right components
		setResources();
		// auto-save both maps
		// HO 27/03/2012 BEGIN *********
		// note: creating wormholes is not undoable
		// because of the auto-save
		SaveAction.saveMapSpecial(sourceMap);
		
		// if the source and target maps are the same,
		// saving the same map twice is unnecessary
		if (!sourceMap.equals(targetMap))
			SaveAction.saveMapSpecial(targetMap);
		// HO 27/03/2012 END *********
		
	}
	
	/**
	 * Method to reset the resources after one of the files changes.
	 * Because the change has to be made to a non-active map, it will only
	 * display if the nonactive map file is reloaded and the MapViewers
	 * removed and replaced.
	 */
	public void resetResources() {
		// first reset the maps
		try {
			setSourceMap(sourceComponent.getParentOfType(LWMap.class));
			if (targetComponent != null)
				setTargetMap(targetComponent.getParentOfType(LWMap.class));

			else
				// this seems redundant, but
				// the targetMap should actually have
				// been set by now so this will
				// throw the appropriate exception if it hasn't.
				setTargetMap(targetMap);

			
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		}
		// now reset the files
		setSourceMapFile(sourceMap.getFile());
		setTargetMapFile(targetMap.getFile());
		// now reset the map and component URIs
		// (not that the component URIs would have changed)
		setResourceURIs();
		// now use this information to recreate the resources
		createResources();
		replaceExistingWormholeResource(sourceWormholeNode, sourceResource, sourceMap);

		if (targetWormholeNode != null)
			replaceExistingWormholeResource(targetWormholeNode, targetResource, targetMap);
		else
			replaceExistingWormholeResource(null, targetResource, targetMap);
		
		// HO 16/04/2012 BEGIN ****
		labelWormholeNodes();
		// HO 16/04/2012 END ******

		SaveAction.saveMap(sourceMap);
		// if the source and target maps are the same,
		// saving the same map twice is unnecessary
		if (!sourceMap.equals(targetMap)) {
			SaveAction.saveMap(targetMap);
		}
		// now reload and redisplay both maps (necessary when
		// making changes to an open map that is not also
		// the active map)
		OpenAction.displayMapSpecial(sourceMapFile);

		// if the source and target maps are the same,
		// saving the same map twice is unnecessary
		if (!sourceMap.equals(targetMap)) {
			OpenAction.displayMapSpecial(targetMapFile);	
		}
	}
	
	/**
	 * Method to reset the resources after one of the files changes.
	 * Because the change has to be made to a non-active map, it will only
	 * display if the nonactive map file is reloaded and the MapViewers
	 * removed and replaced.
	 */
	public void resetResourcesDuringSave() {
		// now reset the map and component URIs
		// (not that the component URIs would have changed)
		setResourceURIs();
		// now use this information to recreate the resources
		createResources();
		replaceExistingWormholeResource(sourceWormholeNode, sourceResource, sourceMap);
		replaceExistingWormholeResource(targetWormholeNode, targetResource, targetMap);

		// re-save the maps (unfortunately this has to be done, clumsy as it is)

		SaveAction.saveMap(sourceMap, false, false);

		// if the source and target maps are the same,
		// saving the same map twice is unnecessary
		if (!sourceMap.equals(targetMap))
			SaveAction.saveMap(targetMap, false, false); 

		// now reload and redisplay both maps (necessary when
		// making changes to an open map that is not also
		// the active map)
		OpenAction.displayMapSpecial(sourceMapFile);

		// if the source and target maps are the same,
		// saving the same map twice is unnecessary
		if (!sourceMap.equals(targetMap))
			OpenAction.displayMapSpecial(targetMapFile);	

	}	
	
	/**
	 * @return false if the source and target map files have not changed,
	 * true otherwise.
	 */
	public boolean isMapFileChanged() {
		
		// if they're all null, they're all equal
		if((sourceMap.getFile() == null) && (sourceMapFile == null)) {
			if((targetMap.getFile() == null) && (targetMapFile == null)) {
				return false;
			}
		} // if any one of the pairs is half-null and half-instantiated,
		// there's been a change
		else if (((sourceMap.getFile() != null) && (sourceMapFile == null))
			|| ((sourceMap.getFile() == null) && (sourceMapFile != null))
			|| ((targetMap.getFile() != null) && (targetMapFile == null))
			|| ((targetMap.getFile() == null) && (targetMapFile != null))) {
			return true;
		}
		// if the maps' latest File objects are
		// the same as the class member files,
		// the files have not changed
		if (sourceMap.getFile().equals(sourceMapFile)) {
			if (targetMap.getFile().equals(targetMapFile)) {
				return false;
			}
		}
		// otherwise the files have changed
		return true;
	}
	
	/**
	 * @param c, the source component for this wormhole.
	 */
	public void setSourceComponent(LWComponent c) {
		sourceComponent = c;
	}
	
	/**
	 * @return sourceComponent, the source component
	 * for this wormhole.
	 */
	public LWComponent getSourceComponent() {
		return sourceComponent;
	}
	
	/**
	 * @param theMap, the source map for this wormhole.
	 */
	public void setSourceMap(LWMap theMap) {
		sourceMap = theMap;
	}
	
	/**
	 * @return sourceMap, the source map
	 * for this wormhole.
	 */
	public LWComponent getSourceMap() {
		return sourceMap;
	}	
	
	/**
	 * @param t, the target component for this wormhole.
	 */
	public void setTargetComponent(LWComponent t) {
		targetComponent = t;
	}
	
	/**
	 * @return targetComponent, the target component
	 * for this wormhole.
	 */
	public LWComponent getTargetComponent() {
		return targetComponent;
	}	
	
	/**
	 * @param theMap, the target map for this wormhole.
	 */
	public void setTargetMap(LWMap theMap) {
		targetMap = theMap;
	}
	
	/**
	 * @return targetMap, the target map
	 * for this wormhole.
	 */
	public LWComponent getTargetMap() {
		return targetMap;
	}	
	
	/**
	 * @param theWormholeNode, the wormhole node that will be
	 * the starting point for this wormhole.
	 */
	public void setSourceWormholeNode(LWWormholeNode theWormholeNode) {
		sourceWormholeNode = theWormholeNode;
	}
	
	/**
	 * @return sourceWormholeNode, the wormhole node that will be
	 * the starting point for this wormhole.
	 */
	public LWWormholeNode getSourceWormholeNode() {
		return sourceWormholeNode;
	}
	
	/**
	 * @param theWormholeNode, the wormhole node that will be
	 * the end point for this wormhole.
	 */
	public void setTargetWormholeNode(LWWormholeNode theWormholeNode) {
		targetWormholeNode = theWormholeNode;
	}
	
	/**
	 * @return targetWormholeNode, the wormhole node that will be
	 * the starting point for this wormhole.
	 */
	public LWWormholeNode getTargetWormholeNode() {
		return targetWormholeNode;
	}	
	
	/**
	 * @param theFile, the File object for the source map.
	 */
	public void setSourceMapFile(File theFile) {
		// make sure the file is saved properly before setting it
		sourceMapFile = checkFileSavedAndReturn(theFile, sourceMap);
	}
	
	/**
	 * @return sourceMapFile, the File object for the source map.
	 */
	public File getSourceMapFile() {
		return sourceMapFile;
	}
	
	/**
	 * @param theFile, the File object for the target map.
	 */
	public void setTargetMapFile(File theFile) {
		// set the file after making sure it's saved
		targetMapFile = checkFileSavedAndReturn(theFile, targetMap);
	}
	
	/**
	 * @return targetMapFile, the File object for the target map.
	 */
	public File getTargetMapFile() {
		return targetMapFile;
	}	
	
	/**
	 * @param theURI, a URI which will become the source wormhole node's 
	 * map resource URI (don't forget that it will point to the target node)
	 */
	public void setSourceResourceMapURI(URI theURI) {
		sourceResourceMapURI = theURI;
	}
	
	/**
	 * @return sourceResourceMapURI, the source wormhole node's map resource URI
	 * (don't forget that it points to the target node)
	 */
	public URI getSourceResourceMapURI() {
		return sourceResourceMapURI;
	}
	
	/**
	 * @param theURI, a URI which will become the target wormhole node's 
	 * resource map URI (don't forget that it will point to the source node)
	 */
	public void setTargetResourceMapURI(URI theURI) {
		targetResourceMapURI = theURI;
	}
	
	/**
	 * @return targetResourceMapURI, the target wormhole node's resource map URI
	 * (don't forget that it points to the source node)
	 */
	public URI getTargetResourceMapURI() {
		return targetResourceMapURI;
	}
	
	/**
	 * @param theURI, a URI which will become the source wormhole node's 
	 * component resource URI (don't forget that it will point to the target node)
	 */
	public void setSourceResourceComponentURI(URI theURI) {
		sourceResourceComponentURI = theURI;
	}
	

	
	/**
	 * @return sourceResourceComponentURI, the source wormhole node's component resource URI
	 * (don't forget that it points to the target node)
	 */
	public URI getSourceResourceComponentURI() {
		return sourceResourceComponentURI;
	}
	
	/**
	 * @param theURI, a URI which will become the target wormhole node's 
	 * resource component URI (don't forget that it will point to the source node)
	 */
	public void setTargetResourceComponentURI(URI theURI) {
		targetResourceComponentURI = theURI;
	}
	
	/**
	 * @return targetResourceComponentURI, the target wormhole node's resource component URI
	 * (don't forget that it points to the source node)
	 */
	public URI getTargetResourceComponentURI() {
		return targetResourceComponentURI;
	}	
	
	/**
	 * Sets the source resource, which consists of:
	 * the source resource map URI, pointing to the target map
	 * and the source resource component URI, pointing to the target component
	 * @param theResource, the Resource to set as the source resource
	 */
	public void setSourceResource(Resource theResource) {
		sourceResource = theResource;
	}
	
	/**
	 * @return sourceResource, which consists of
	 * the source resource map URI, pointing to the target map
	 * and the source resource component URI, pointing to the target component
	 */
	public Resource getSourceResource() {
		return sourceResource;
	}
	
	/**
	 * Sets the target resource, which consists of:
	 * the target resource map URI, pointing to the source map
	 * and the target resource component URI, pointing to the source component
	 * @param theResource, the Resource to set as the target resource
	 */
	public void setTargetResource(Resource theResource) {
		targetResource = theResource;
	}
	
	/**
	 * @return targetResource, which consists of
	 * the target resource map URI, pointing to the source map
	 * and the target resource component URI, pointing to the source component
	 */
	public Resource getTargetResource() {
		return targetResource;
	}	
	
	/**
	 * @return sourceMapFileName, the String representing the full pathname
	 * of the source map.
	 */
	public String getSourceMapFileName() {
		if (sourceMapFile != null)
			return sourceMapFile.getAbsolutePath();
		else
			return null;
	}
	
	/**
	 * @return targetMapFileName, the String representing the full pathname
	 * of the target map.
	 */
	public String getTargetMapFileName() {
		if (targetMapFile != null)
			return targetMapFile.getAbsolutePath();
		else
			return null;
	}
	
	/**
	 * @return sourceResourceMapURIString, the String representing the URI
	 * of the source resource map.
	 */
	public String getSourceResourceMapURIString() {
		if (sourceResourceMapURI != null)
			return sourceResourceMapURI.toString();
		else
			return null;
	}
	
	/**
	 * @return targetResourceMapURIString, the String representing the URI
	 * of the target resource map.
	 */
	public String getTargetResourceMapURIString() {
		if (targetResourceMapURI != null)
			return targetResourceMapURI.toString();
		else
			return null;
	}	
	
	/**
	 * @return sourceResourceComponentURIString, the String representing the URI
	 * of the source resource component.
	 */
	public String getSourceResourceComponentURIString() {
		if (sourceResourceComponentURI != null)
			return sourceResourceComponentURI.toString();
		else
			return null;
	}
	
	/**
	 * @return targetResourceComponentURIString, the String representing the URI
	 * of the target resource component.
	 */
	public String getTargetResourceComponentURIString() {
		if (targetResourceComponentURI != null)
			return targetResourceComponentURI.toString();
		else
			return null;
	}	
	
	/**
	 * @param b, true if the wormhole's construction
	 * was cancelled at any point, false otherwise
	 */
	public void setBCancelled(boolean b) {
		bCancelled = b;
	}
	
	/**
	 * @return true of the wormhole's construction
	 * was cancelled at any point, false otherwise
	 */
	public boolean getBCancelled() {
		return bCancelled;
	}
	
	/**
	 * @param b, true if we are constructing the wormhole
	 * while saving a file, false otherwise
	 */
	public void setBSaving(boolean b) {
		bSaving = b;
	}
	
	/**
	 * @return true if we are constructing the wormhole
	 * while saving one of its files, false otherwise
	 */
	public boolean getBSaving() {
		return bSaving;
	}
	
	/**
	 * A routine to look for a given component among all the open
	 * maps, and select it
	 * @param theComponent, the LWComponent we're looking for
	 */
	public void findAndSelectComponentAmongOpenMaps(LWComponent theComponent) {
		// input validation
		if (theComponent == null)
			return;
		
		// get all the open maps
		Collection<LWMap> coll = VUE.getAllMaps();
		for (LWMap map: coll) {
			// find the one with our quarry's URI string
			LWComponent c = map.findChildByURIString(theComponent.getURIString());
			// if we found it, select it
			if (c != null) {
				c.setSelected(true);
				break;
			}
			map = null;
		}	
		coll = null;
	}
	
	/**
	 * A routine to find and save all the open
	 * maps
	 */
	public void findAndSaveAllOpenMaps() {
		
		// get all the open maps
		Collection<LWMap> coll = VUE.getAllMaps();
		for (LWMap map: coll) {
			if ((map.isModified()) && (map.getFile() != null)) {
				// here's the current map, save it
				SaveAction.saveMap(map);
			}
			map = null;
		}
		
		coll = null;
	}
	
	/**
	 * A routine to reset the wormhole resource of a component that is not necessarily
	 * in the active map, following a change to one of the wormhole's filenames or
	 * locations.
	 * @param theComponent, the component that contains the WormholeResource to be changed
	 * @param newResource, the WormholeResource that will replace the existing one
	 * @param theMap, the map that contains the component that contains the resource that swallowed the fly
	 */
	public void replaceExistingWormholeResource(LWComponent theComponent, Resource newResource, LWMap theMap) {
		// make sure the other map can't take the focus away from this one
		VUE.setActive(LWMap.class, this, theMap);
		
		// select the component that's getting the resource
		findAndSelectComponentAmongOpenMaps(theComponent);

		Resource activeResource = null;
		if (theComponent != null) {
			// get the resource that that component already has
			 activeResource = theComponent.getResource();
		} else {
			return;
		}

        // if the resource we passed in isn't null,
        // replace the resource we just got from the
        // component with the one we passed in
        // start by assuming the two resources are  the same
        boolean bTheSame = true;
        if (newResource != null) {
        	if (activeResource != null) {
        		WormholeResource ar = (WormholeResource) activeResource;
        		WormholeResource nr = (WormholeResource) newResource;
        		if (!ar.getComponentURIString().equals(nr.getComponentURIString()))
        			bTheSame = false;
        		else if (!ar.getOriginatingComponentURIString().equals(nr.getOriginatingComponentURIString()))
        			bTheSame = false;
        		else if (!ar.getOriginatingFilename().equals(nr.getOriginatingFilename()))
        			bTheSame = false;
        		else if (!ar.getSpec().equals(nr.getSpec()))
        			bTheSame = false;
        	}
            activeResource = newResource;
        }
	
        // but if both resources are null,
        // we've got nothing to work with so return
        if (activeResource == null)
            return;
        
        // if both resources are the same,
        // we also have nothing to work with so return
        if (bTheSame == true)
        	return;

        // Make sure nothing can refer to this component.
        VUE.setActive(LWComponent.class, this, null);
        // set the component's resource to the most recent
        // valid resource.
        theComponent.setResource(activeResource);        
        // make sure nothing can take the focus from this component.
        VUE.setActive(LWComponent.class, this, theComponent);                                                 	
    
	}
	
	/**
	 * A function to get the file belonging to a map,
	 * prompting the user to save if necessary.
	 * @param theFile, the file we want to set the map to
	 * @param theMap, the map whose file we want to get
	 * @return theFile, which will either be the File we originally meant
	 * to set the map to, or if that's null, a new File taken directly
	 * from the map
	 */
	public File checkFileSavedAndReturn(File theFile, LWMap theMap) {
		// validate input - if we have no map,
		// there's nothing to save
		if (theMap == null)
			return null;
		
    	// flags if the file be saved or not
		// start by assuming not
		boolean bSaved = false;
		// if the file we're setting it to is null,
		// save it
		if (theFile == null) {
			// or that is, ask them to save it if it's changed
			// since last saving (which it would have if it were new)
    		bSaved = VUE.askSaveIfModified(theMap);
			// and now if the file be saved,
			// get the newly-saved file from the map
    		if (bSaved == true) {
    			theFile = theMap.getFile();
    		}
    	} 
		
		return theFile;
	}

}
