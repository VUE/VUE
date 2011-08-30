/*
 * This addition Copyright 2010-2011 Design Engineering Group, Imperial College London
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
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
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

	public LWWormhole() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Wormhole constructor.
	 * @param c, the LWComponent to which to add the wormhole.
	 * @param bNew, true if the wormhole is to be added to a new map,
	 * false if it is to be added to an existing map
	 */
	public LWWormhole(LWComponent c, boolean bNew) {
		init(c, bNew);
	}
	
	public LWWormhole(LWWormholeNode wn, WormholeResource wr) {
		init(wn, wr);
	}
	
	public LWWormhole(LWWormholeNode wn, WormholeResource wr, String prevURI, LWComponent newParent) {
		init(wn, wr, prevURI, newParent);
	}
	
	/* public LWWormhole(LWWormholeNode wn, WormholeResource wr, File beingSavedTo, LWComponent c) {
		init(wn, wr, beingSavedTo, c);
	} */
		
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
		// HO 08/02/2011 BEGIN ***************
		// Collection<LWMap> coll = findAndSaveAllOpenMaps(); 
		findAndSaveAllOpenMaps();
		// HO 08/02/2011 END ***************
		
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
    	
    	// against memory leaks
    	// coll = null;
    	
    	// flag end of construction process
    	flagEndOfConstruction(sourceMap);
    	flagEndOfConstruction(targetMap);
    }
	
	// HO 23/02/2011 BEGIN **********
	private void flagStartOfConstruction(LWMap map) {
		if (map != null)
			map.bConstructingWormholes = true;
	}
	
	private void flagEndOfConstruction(LWMap map) {
		if (map != null)
			map.bConstructingWormholes = false;
	}
	// HO 23/02/2011 END ************
	
	/**
	 * Initializes the wormhole by extrapolating the needed information
	 * from a LWWormholeNode and a WormholeResource.
	 * Created for use during a restore from a persisted map.
	 * @param wn, the LWWormholeNode we're going to use to extrapolate the wormhole
	 * @param wr, the WormholeResource in this LWWormholeNode
	 */
	public void init(LWWormholeNode wn, WormholeResource wr) 
    {
		// HO 08/02/2011 BEGIN ***************
		findAndSaveAllOpenMaps();
		// HO 08/02/2011 END ***************
		
		// we are cancelled until we have successfully
		// constructed the whole wormhole
		setBCancelled(true);
		// extrapolate the components, maps, and nodes
		// from the wormhole node and wormhole resource passed in
		boolean b = extrapolateComponentsMapsAndNodes(wn, wr);

		// if they stopped here, no need to go any further
		if (b == false)
			return;	

		resetResources();
    	
		// add the listeners
    	addAllListeners();

    	// if we got this far we're okay
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
		// HO 08/02/2011 BEGIN ***************
		findAndSaveAllOpenMaps();
		// HO 08/02/2011 END ***************
		
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

		resetResources();
    	
		// add the listeners
    	addAllListeners();

    	// if we got this far we're okay
    	setBCancelled(false);
    }	
	
	/**
	 * Initializes the wormhole by extrapolating the needed information
	 * from a LWWormholeNode and a WormholeResource.
	 * @param wn, the LWWormholeNode we're going to use to extrapolate the wormhole
	 * @param wr, the WormholeResource in this LWWormholeNode
	 * @param beingSavedTo, the new File that the source map is being saved to
	 * @param c, the LWComponent that is the source component
	 */
	/* public void init(LWWormholeNode wn, WormholeResource wr, File beingSavedTo, LWComponent c) 
    {
		// HO 08/02/2011 BEGIN ***************
		findAndSaveAllOpenMaps();
		// HO 08/02/2011 END ***************
		
		// yes this is during a save
		setBSaving(true);
		// we are cancelled until we have successfully
		// constructed the whole wormhole
		setBCancelled(true);
		// input validation
		if ((wn == null) || (wr == null) || (beingSavedTo == null) || (c == null))
			return;
		// extrapolate the components, maps, and nodes
		// from the wormhole node and wormhole resource passed in
		// and the new file to be saved to
		boolean b = extrapolateDuringSave(wn, wr, beingSavedTo, c);
		// if they stopped here, no need to go any further
		if (b == false)
			return;	

		resetResourcesDuringSave();
		
		// add the listeners
    	addAllListeners();
    	
    	// if we got this far we're okay
    	setBCancelled(false);
    } */	
	
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
    	// HO 16/02/2011 BEGIN ****************
    	// get the source component's parent map and make it the source map
    	// temporary - we'll change this after we get the file
    	setSourceMap(sourceComponent.getParentOfType(LWMap.class));
    	// HO 16/02/2011 END ****************
    	
    	// create a default-style target node
    	setTargetComponent(createDefaultTargetNode());
    	 	
    	// ask the user to save the current map
    	
    	// HO 15/02/2011 BEGIN ****************
    	//boolean b = askSaveSourceMap(sourceMap);
    	//File f = askSaveSourceMap(sourceMap);
    	// HO 11/05/2011
    	// HO 27/07/2011 BEGIN test ***********
    	MapViewer viewer = VUE.getCurrentTabbedPane().getViewerWithMap(sourceMap);
        // HO 27/07/2011 END ***********
    	Component comp = setScreen(sourceMap);
    	// HO 27/07/2011 BEGIN test ***********
    	viewer = VUE.getCurrentTabbedPane().getViewerWithMap(sourceMap);
        // HO 27/07/2011 END ***********
    	//LWMap srcMap = askSaveSourceMap(sourceMap);
    	// HO 02/07/2011 begin ************
    	// LWMap srcMap = askSaveSourceMap(sourceMap, comp);
    	// HO 22/08/2011 BEGIN **********
    	// LWMap srcMap = SaveAction.saveMapSpecial(sourceMap, false, false);
    	LWMap srcMap = SaveAction.saveMapSpecial(sourceMap, false, false, "Save Source Map");
    	// HO 22/08/2011 END **********
    	// HO 02/07/2011 END ************
    	// HO 27/07/2011 BEGIN test ***********
    	viewer = VUE.getCurrentTabbedPane().getViewerWithMap(sourceMap);
        // HO 27/07/2011 END ***********
    	
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
    	// HO 15/02/2011 END ****************
    	
    	// the target map
    	LWMap targMap = null;
    	
    	// ask them to choose a new or existing map for the target map
    	if (bNew == true) {
    		// HO 15/02/2011 BEGIN ****************
    		//theMap = askSelectNewTargetMap(sourceMap);
    		// HO 11/05/2011 BEGIN ****************
    		targMap = askSelectNewTargetMap(new LWMap(""), comp);
    		// reset the focus back to the source map 
    		MapViewer leftViewer = null;
    		MapTabbedPane leftPane = null;
            MapViewer rightViewer = null;
            MapTabbedPane rightPane = null;
                if (VUE.isActiveViewerOnLeft()) {
                	leftPane = VUE.getLeftTabbedPane();
                	int i = leftPane.findTabWithMap(srcMap);
                	//leftViewer = leftPane.getViewerAt(i);
                	//leftPane.setSelectedComponent(leftViewer);
                	//leftPane.setSelectedIndex(i);
                	leftPane.setSelectedMap(srcMap);
            		//VUE.getLeftTabbedPane().setSelectedMap(srcMap);
                }
                else {
                	rightPane = VUE.getRightTabbedPane();
                	int i = rightPane.findTabWithMap(srcMap);
                	//rightViewer = rightPane.getViewerAt(i);
                	//rightPane.setSelectedComponent(rightViewer);
                	rightPane.setSelectedIndex(i);
                	//VUE.getRightTabbedPane().setSelectedMap(srcMap);
                } 
    		// HO 11/05/2011 END ****************
    		// create a new map out of the target file
    		//theMap = LWMap.create(theFile.toURI().toString());
    		// HO 15/02/2011 END ****************
    	} else {
    			// ask them to choose a place to put the target node
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
	    	// HO 27/12/2010 BEGIN **************
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
	    	// HO 27/12/2010 END ****************
		}
		
		return hasFile;
	}
	
	// HO 25/03/2011 BEGIN ****************
	// enormous waste of time.
	private boolean pointsToSameMap(WormholeResource wr) {
		boolean bSameMap = false;
		String strSpec = wr.getSystemSpec();
		String strOriginatingFile = wr.getOriginatingFilename();
		
		String strPossPrefix = "file:";
		// HO 28/03/2011 BEGIN *************
		String strBackSlashPrefix = "\\\\";
		String strBackSlash = "\\";
		String strForwardSlashPrefix = "////";
		String strForwardSlash = "/";
		// HO 28/03/2011 END *************
		// if the spec was not set, replace it with the last known filename
		if (strSpec.equals(wr.SPEC_UNSET))
			strSpec = wr.getTargetFilename();
		
		// HO 12/05/2011 BEGIN *******
		// strip wrongly-formatted spaces so they don't gum up the comparison
		strSpec = stripHtmlSpaceCodes(strSpec);
		strOriginatingFile = stripHtmlSpaceCodes(strOriginatingFile);		
		// HO 12/05/2011 END *********
		
		if (strSpec.startsWith(strPossPrefix))
			strSpec = strSpec.substring(strPossPrefix.length(), strSpec.length());
		if (strOriginatingFile.startsWith(strPossPrefix))
			strOriginatingFile = strOriginatingFile.substring(strPossPrefix.length(), strOriginatingFile.length());
		
		// HO 28/03/2011 BEGIN *************
		if (strSpec.startsWith(strBackSlashPrefix))
			strSpec = strSpec.substring(strBackSlashPrefix.length(), strSpec.length());
		if (strOriginatingFile.startsWith(strBackSlashPrefix))
			strOriginatingFile = strOriginatingFile.substring(strBackSlashPrefix.length(), strOriginatingFile.length());
		
		if (strSpec.startsWith(strForwardSlashPrefix))
			strSpec = strSpec.substring(strForwardSlashPrefix.length(), strSpec.length());
		if (strOriginatingFile.startsWith(strForwardSlashPrefix))
			strOriginatingFile = strOriginatingFile.substring(strForwardSlashPrefix.length(), strOriginatingFile.length());
		// HO 28/03/2011 END *************
		
		if (strSpec.equals(strOriginatingFile))
			bSameMap = true;
		else if ((strSpec.contains(strForwardSlash)) && (strOriginatingFile.contains(strBackSlash))) {
			strSpec = strSpec.replaceAll(strForwardSlash, strBackSlashPrefix);
		}
		else if ((strSpec.contains(strBackSlash)) && (strOriginatingFile.contains(strForwardSlash))) {
			strOriginatingFile = strOriginatingFile.replaceAll(strForwardSlash, strBackSlashPrefix);
		}
		
		if (strSpec.equals(strOriginatingFile))
			bSameMap = true;
					
		return bSameMap;
	} 
	// HO 25/03/2011 END ****************

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
		
		// Start by extrapolating the source map from the node parent
		LWMap actualSourceMap = null;
		try {
			actualSourceMap = wn.getParentOfType(LWMap.class);
		} catch (NullPointerException e) {
			return false;
		}

		// if the node has a parent map, make it the source map
		if (actualSourceMap != null) {
			setSourceMap(actualSourceMap);
		} else {
			return false;
		}
		
		// if the resource's source and target are the same map, 
		// we set the target map to the source
		LWMap targMap = null;
		if (bSameMap)
			targMap = actualSourceMap;
		else // if not the same, go ahead and extrapolate the target map from the resource
			targMap = extrapolateTargetMap(wr);

		
		// if we now have a target map
		if (targMap != null)
			// set it as our target map component
			setTargetMap(targMap);
		else
			// but if we don't have a map, we failed
			return false;
		
		// now we extrapolate the target component
		// by getting the component URI string from the resource
		String compString = wr.getComponentURIString();
		// if the component URI string is null or empty,
		// we can't extrapolate enough information to create
		// the wormhole, so flag failure and return
		if ((compString == null) || (compString == ""))
			return false;
		
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
		
		// now instantiate the source component which we are about to extrapolate
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
		
		// if we didn't manage to set the source component,
		// we've failed so flag failure and return
		if (sourceComp == null)
			return false;

    	// if we got this far we have succeeded
    	return true;
	}
	
	/**
	 * Function to extract the target map from a given WormholeResource.
	 * Assumption is that we know the source map by this point.
	 * If the map can't be found in the stated target, we look in the local
	 * folder. Then give up.
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
		String targetSpec = "";
		try {
			targetSpec = wr.getSystemSpec();
			if (targetSpec == "")
				return null;
		} catch(NullPointerException e) {
			return null;
		}

		// if it's null or an empty string, we can't
		// extrapolate anything from it
		if ((targetSpec == null) || (targetSpec == ""))
			return null;
		
		// now we extrapolate the target component
		// by getting the component URI string from the resource
		String compString = wr.getComponentURIString();
		// if the resource doesn't have a component string it's broken
		if ((compString == null) || (compString == ""))
			return null;
		
		// to ward off FileNotFoundExceptions later on,
		// create a target file using the path extracted
		// from the resource
		// HO 12/05/2011 BEGIN *********
		targetSpec = stripHtmlSpaceCodes(targetSpec);
		// HO 12/05/2011 END *********
		File targFile = new File(targetSpec);
		// 24/12/2010 BEGIN ********
		//if we can't find the file, check for one with the same name
		// in the local folder
			try {
				if (!targFile.isFile()) {
					String strTargName = targFile.getName();
					// HO 15/04/2011 BEGIN **********
					//String strLocalParent = new File(sourceMap.getLabel()).getParent();
					String strLocalParent = "";
					if (sourceMap.getFile() != null)
						strLocalParent = new File(sourceMap.getFile().getAbsolutePath()).getParent();
					if ((strLocalParent != null) && (strLocalParent != ""))	
						targFile = new File(strLocalParent, strTargName);
					else
						return null;
						// HO 15/04/2011 END **********
				} 
				// 24/12/2010 END ***************
			} catch (Exception e) {
				// do nothing
				return null;
			}
			if (targFile.isFile()) {
				targetSpec = targFile.getAbsolutePath();
				targMap = OpenAction.loadMap(targetSpec);
			}
			else {// if we still can't find it, search all the subfolders				
				// HO 09/08/2011 BEGIN **********
				// Vector v = new Vector();
				// v = fileExistInPath(targFile.getParent(), targFile.getName(), v);
				targMap = fileExistInPath(targFile.getParent(), targFile.getParent(), targFile.getName(), compString);
				
			    // Iterator itr = v.iterator();

			    /* LWComponent targetComp = null;
			    while(itr.hasNext()) {
			    	File f = (File)itr.next();
			    	String s = f.getAbsolutePath();
			    	LWMap map = OpenAction.loadMap(s);
					// see if this is the right map
			    	// by looking to see if the target component is in it
					targetComp = map.findChildByURIString(compString);
					// if we found it, this is the right map
					if (targetComp != null) {
						targMap = map;
						break;
					}
			     } */
			}
			// HO 09/08/2011 END **********				
			return targMap;
	}
	
	private static LWMap checkIfRightMap(File f, String compString) {
    	if (f == null)
    		return null;
    	
    	LWMap targMap = null;
    	
    	String s = f.getAbsolutePath();
    	LWMap map = OpenAction.loadMap(s);
		// see if this is the right map
    	// by looking to see if the target component is in it
		LWComponent targetComp = map.findChildByURIString(compString);
		// if we found it, this is the right map
		if (targetComp != null) {
			targMap = map;
		}
		
		return targMap;
	}
	


		/**

		* @param root_dir, the top of the file structure where we first started looking for the file
		* @param top_level_dir, the parent directory we are searching in now

		* @param file_to_search, the file we are searching for

		* @param compString, the UURI of the target component

		* @return LWMap, the map if we find the one with the target component, null otherwise

		*/
		// HO 09/08/2011 BEGIN **********
		public static LWMap fileExistInPath(String root_dir, String top_level_dir, String file_to_search, String compString) {
			String strRootDir = root_dir;
			// HO 09/08/2011 END **********
			LWMap theMap = null;
			// get the files in the current directory
			File f = new File(top_level_dir);	
			File[] dir = f.listFiles();	
			// if we have a list of files, cycle through them
			if (dir != null) {	
				for (int i = 0; i < dir.length; i++) {
					File file_test = dir[i];
					// HO 09/08/2011 BEGIN *******
					// don't waste time on Subversion folders
					if (".svn".equals(dir[i].getName().toString())) {
						continue;
					}
					// don't waste time on the Trash folder
					if (".Trash".equals(dir[i].getName().toString())) {
						continue;
					}
					// HO 09/08/2011 END *********
					if (file_test.isFile()) {	
						if (file_test.getName().equals(file_to_search)) {
							System.out.println("File Name :" + file_test);
							//v.add(top_level_dir);	
							//v.add(new File(top_level_dir, file_test.getName()));
							theMap = checkIfRightMap(file_test, compString);
							if (theMap != null) 
								break;
						}
					} else if(file_test.isDirectory()){	
						// HO 09/08/2011 BEGIN *********
						// do not go more than 6 levels below the directory structure
						/* int count;
						File countFile = file_test;
						for (count = 0; count <=6; count++) {
							String stepUp = countFile.getParent();
							if (stepUp.equals(strRootDir)) {
								break;
							} else {
								countFile = new File(stepUp);
							}
						}
						if (count < 7) { */
							// HO 09/08/2011 END ***********
							theMap = fileExistInPath(strRootDir, file_test.getAbsolutePath(), file_to_search, compString);
							if (theMap != null)
								break;
						/* } else {
							continue;
						} */
					}
				}
			} else {	
				System.out.println("null list of files");
			}	
			return theMap;

		}
	
	private boolean extrapolateDuringReparent(LWWormholeNode wn, WormholeResource wr, String prevURI, LWComponent newParent) {
		// input validation
		if ((wn == null) || (wr == null) || (newParent == null))
			return false;
		
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
			// HO 08/02/2011 BEGIN ***************
			// HO 22/02/2011 BEGIN ***************
    		SaveAction.saveMap(actualSourceMap);
			//actualSourceMap = SaveAction.saveMapSpecial(actualSourceMap);
    		// HO 22/02/2011 END ***************
    		// HO 08/02/2011 END ***************
			setSourceMap(actualSourceMap);
		} else {
			return false;
		}
		
		// now instantiate a target map
		LWMap targMap = null;
		
		// HO 12/05/2011 BEGIN ******
		// if this points to the same map, easy
		if (pointsToSameMap(wr)) {
			targMap = actualSourceMap;
			setTargetMap(targMap);
		} else {			
			// HO 12/05/2011 END *******
			
			// first we extrapolate the target
			// which is easy because it's in the resource we have
			// get the target file path from the resource
			// HO 03/09/2010 BEGIN - just calling getSpec() can cause
			// any file.exists() tests to return false even when the file
			// blatantly exists
			String targetSpec = "";
			try {
				targetSpec = wr.getSystemSpec();
				// if the spec was not set, replace it with the last known filename
				if (targetSpec.equals(wr.SPEC_UNSET))
					targetSpec = wr.getTargetFilename();
				
				if (targetSpec == "")
					return false;

			} catch(NullPointerException e) {
				return false;
			}
	
			// if it's null or an empty string, we can't
			// extrapolate anything from it
			if ((targetSpec == null) || (targetSpec == ""))
				return false;
			
			// HO 12/05/2011 BEGIN *******
			// strip away wrongly-formatted spaces
			targetSpec = stripHtmlSpaceCodes(targetSpec);			
			// HO 12/05/2011 END ********
			
			// to ward off FileNotFoundExceptions later on,
			// create a target file using the path extracted
			// from the resource
			// HO
			File targFile = new File(targetSpec);
			
			// if the target file extracted from the wormhole
			// resource actually exists (which it should,
			// because we just created it), load the map
			// belonging to that file and make it the target map
			if (targFile.exists() == true) {
				try {
					targMap = OpenAction.loadMap(targetSpec);
				} catch (Exception e) {
					// do nothing
					return false;
				}
			}
			
			// if we now have a target map
			if (targMap != null)
				// set it as our target map component
				setTargetMap(targMap);
			else
				// but if we don't have a map, we failed
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
		
		wn.setWormholeType(LWWormholeNode.WormholeType.SOURCE.toString());
		setSourceWormholeNode(wn);
		//LWWormholeNode targNode = createTargetWormholeNode();
		
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
	 * A function to extrapolate the components and files of the wormhole during a save
	 * to a different source file.
	 * Gave up on this because the process of updating wormholes
	 * during a save was so unwieldy and error-prone.
	 * @param wn, the LWWormholeNode from which to extrapolate the info.
	 * @param wr, the WormholeResource from which to extrapolate the info.
	 * @param beingSavedTo, the File that the source file is being saved to.
	 * @param c, the source component
	 * @return
	 */
	/* private boolean extrapolateDuringSave(LWWormholeNode wn, WormholeResource wr, File beingSavedTo, LWComponent c) {
		// input validation
		if (beingSavedTo == null)
			return false;
		
		// HO 07/09/2010 BEGIN **********************
		LWMap newSourceMap = null;
		if (c != null) {
			newSourceMap = c.getParentOfType(tufts.vue.LWMap.class);
		}
		// HO 07/09/2010 END **********************

		// if the source map doesn't fully exist, the operation won't work properly
		if (newSourceMap != null) {
			setSourceMap(newSourceMap);
		} else {
			return false;
		}
		
		// the source component
		if (c != null) {
			setSourceComponent(c);
		}
		else {
			return false;
		}
		
		// first we extrapolate the target
		// which is easy because it's in the resource we have
		// get the target file path from the resource
		// HO 03/09/2010 BEGIN - just calling getSpec() can cause
		// any file.exists() tests to return false even when the file
		// blatantly exists
		String targetSpec = wr.getSystemSpec();
		//String targetSpec = wr.getSpec();

		// if it's null or an empty string, we can't
		// extrapolate anything from it
		if ((targetSpec == null) || (targetSpec == ""))
			return false;
		
		// to ward off FileNotFoundExceptions later on,
		// create a target file using the path extracted
		// from the resource
		File targFile = new File(targetSpec);

		// now instantiate a target map
		LWMap targMap = null;
		// if the target file extracted from the wormhole
		// resource actually exists (which it should,
		// because we just created it), load the map
		// belonging to that file and make it the target map
		// first record the directory we're in now
		targMap = OpenAction.loadMap(targetSpec);
		
		// if we now have a target map
		if (targMap != null)
			// set it as our target map component
			setTargetMap(targMap);
		else
			// but if we don't have a map, we failed
			return false;
		
		// now we extrapolate the target component
		// by getting the component URI string from the resource
		String compString = wr.getComponentURIString();
		// if the component URI string is null or empty,
		// we can't extrapolate enough information to create
		// the wormhole, so flag failure and return
		if ((compString == null) || (compString == ""))
			return false;
		
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
		
		// set the originally passed-in wormhole node
		// to be the source wormhole node
		wn.setWormholeType(LWWormholeNode.WormholeType.SOURCE.toString());
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
						// be pointing towards the map we're in now, 
						// because if there's more than one resource,
						// or if there was a resource but it went missing,
						// we could be changing the values on the wrong resource
						// Downcast the resource to a wormhole resource
						WormholeResource worm = (WormholeResource)r;
						// Get the component the resource is pointing to
						// by searching the source map for a component with
						// this URI string
						if (worm.getComponentURIString().equals(sourceComponent.getURIString())) {
							// set the target wormhole node to be the node
							// we just got the wormhole resource out of
							nextNode.setWormholeType(LWWormholeNode.WormholeType.TARGET.toString());
							setTargetWormholeNode(nextNode);
							// and break free
							break;
						}
					}
				
			}
		}		
		
		// now is the time to set the files
		File newSourceMapFile = sourceMap.getFile();
		if (newSourceMapFile == null)
			return false;
		
		setSourceMapFile(newSourceMapFile);
		setTargetMapFile(targFile);

    	// if we got this far we have succeeded
    	return true;
	} */
	
	/**
	 * Ask the user to save the source map.
	 * @param map, the map we are trying to save
	 * @return true if the map was saved successfully,
	 * false otherwise
     * Returns true if either they save it or say go ahead and close w/out saving.
     */
	// HO 11/05/2011
    //private LWMap askSaveSourceMap(LWMap map) {
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

        //Component c = setScreen(map);
        
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
        	// HO 15/02/2011 BEGIN ***********
        	// HO 25/05/2011 swapped
            //return SaveAction.saveMap(map);
        	// HO 20/06/2011 BEGIN ***********
        	// HO 22/08/2011 BEGIN **********
        	// return SaveAction.saveMapSpecial(map, false, false);
        	return SaveAction.saveMapSpecial(map, false, false, "Save Source Map");
        	// HO 22/08/2011 END **********
        	// return SaveAction.saveMapSpecial(map, true, false);
        	// HO 20/06/2011 END ***********
            // HO 15/02/2011 END ***********
        } else if (response == JOptionPane.NO_OPTION) { // Save As
            // save not necessarily in the default location
        	// HO 15/02/2011 BEGIN ***********
        	// HO 25/05/2011 swapped
        	//return SaveAction.saveMap(map);
        	// HO 22/08/2011 BEGIN **********
            // return SaveAction.saveMapSpecial(map, true, false);
        	return SaveAction.saveMapSpecial(map, true, false, "Save Source Map");
            // HO 22/08/2011 END **********
            // HO 15/02/2011 END ***********
        } else // anything else (Cancel or dialog window closed)
            return null;
    }
    
    /**
     * A function to prompt the user to select a new map
     * into which to target this wormhole
     * @param map, the LWMap we are currently in
     * @return the map selected by the user
     */
 // HO 11/05/2011 BEGIN ************
   //private LWMap askSelectNewTargetMap(LWMap map, boolean b) {
    private LWMap askSelectNewTargetMap(LWMap map, Component focusComp) {
	// HO 11/05/2011 END ************
    	// HO 22/08/2011 BEGIN ***************
    	//final Object[] defaultOrderButtons = { "Choose Target Map", "Cancel"};
    	//final Object[] macOrderButtons = { "Cancel", "Choose Target Map"};

        // HO 04/01/2011 BEGIN *************
    	// to ward off IllegalComponentStateException
    	// HO 11/05/2011 BEGIN ************
    	//Component c = setScreen(map);
    	//Component c = setScreen(focusComp);
    	// HO 11/05/2011 END ************
    	// HO 04/01/2011 END *************
    	// HO 22/08/2011 END ***************
    	// HO 27/07/2011 BEGIN test ***********
    	// HO 22/08/2011 BEGIN ***************
    	// MapViewer viewer = VUE.getCurrentTabbedPane().getViewerWithMap(sourceMap);
        // HO 27/07/2011 END ***********
    	
    	//int response = VueUtil.option
        // (// HO 11/05/2011 BEGIN ************
        		//viewer,
        		// VUE.getDialogParent(),
        		//c,
        		// HO 11/05/2011 END ************
         //"Please choose a target map.",
         //"Choose Target Map",
         //JOptionPane.OK_CANCEL_OPTION,
         //JOptionPane.PLAIN_MESSAGE,
         //Util.isMacPlatform() ? macOrderButtons : defaultOrderButtons,             
         //"Choose Target"
         //);
    	// HO 28/02/2011 END ****************

        //if (!Util.isMacPlatform()) {
            //switch (response) {
            // HO 28/02/2011 BEGIN ************
            // the OK_OPTION (choose target map)
            //case 0: response = 0; break;
            // the CANCEL option
            //case 1: response = 1; break;
            // HO 28/02/2011 END ***************
           // }
        //} else { 
            //switch (response) {
            // the OK_OPTION (choose a target map)
            //case 0: response = 1; break;
            // the CANCEL option
            //case 1: response = 0; break;
            //}
        //}
        
        //if (response == JOptionPane.OK_OPTION) { // Save
        	// HO 22/08/2011 END ***************
    		// HO 22/08/2011 BEGIN **********
        	// LWMap newTargetMap = SaveAction.saveMapSpecial(map, true, false);
    		LWMap newTargetMap = SaveAction.saveMapSpecial(map, true, false, "Save Target Map");
        	// HO 22/08/2011 END **********
        	// prompt to save
        	if (newTargetMap != null) {
        		// now we have our target map
        		return newTargetMap;
        	} else {
        		return null;
        	}
       //} else // anything else (Cancel or dialog window closed) 
        	//{
            //return null;
        //}

    }	    
    
    /**
     * A function to prompt the user to select a new map
     * into which to target this wormhole
     * @param map, the LWMap we are currently in
     * @return the map selected by the user
     */
   // HO 11/05/2011 BEGIN ************
   /* private LWMap askSelectNewTargetMap(LWMap map) {
    	final Object[] defaultOrderButtons = { "Choose Target Map", "Cancel"};
    	final Object[] macOrderButtons = { "Cancel", "Choose Target Map"};

        // HO 04/01/2011 BEGIN *************
    	// to ward off IllegalComponentStateException
    	Component c = setScreen(map);
    	// HO 04/01/2011 END *************
        
        int response = VueUtil.option
            (c,
             "Please choose a target map.",
             "Choose Target Map",
             JOptionPane.OK_CANCEL_OPTION,
             JOptionPane.PLAIN_MESSAGE,
             Util.isMacPlatform() ? macOrderButtons : defaultOrderButtons,             
             VueResources.getString("Choose Target")
             );
        
     
        if (!Util.isMacPlatform()) {
            switch (response) {
            // the OK_OPTION (choose target map)
            case 0: response = 0; break;
            // the CANCEL option
            case 1: response = 1; break;
            }
        } else { 
            switch (response) {
            // the OK_OPTION (choose a target map)
            case 0: response = 1; break;
            // the CANCEL option
            case 1: response = 0; break;
            }
        }
        
        if (response == JOptionPane.OK_OPTION) { // Save
        	// HO 22/02/2011 BEGIN **************
        	LWMap newTargetMap = new LWMap("new target map");
        	//newTargetMap = SaveAction.saveMapSpecial(newTargetMap, true, false);
        	// prompt to save
        	if (SaveAction.saveMap(newTargetMap, true, false) == true) {
        	//if (newTargetMap != null) {
    		// HO 22/02/2011 END **************
        		// now we have our target map
        		return newTargetMap;
        	} else {
        		return null;
        	}
        } else // anything else (Cancel or dialog window closed) 
        	{
            return null;
        }

    } */	
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
            // HO 04/01/2011 BEGIN **************
        	Point p = null;
            try {
            	//p = c.getLocationOnScreen();
            	p = c.getLocation(null);
            } catch (Exception e) {
            	c = null;
            }
                // HO 04/01/2011 END **************
        	
        	if ((p.x + c.getWidth() > screenSize.width) ||
        			(p.y + c.getHeight() > screenSize.height))
        	{
        		c = null;
        	}
        }
        
        /* final String debug;

        if (DEBUG.EVENTS || DEBUG.UNDO)
            debug = "\n[modifications="+comp.getModCount()+"]";
        else
            debug = ""; */
        
        return c;
    }
   // HO 11/05/2011 END ************
    
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
            // HO 04/01/2011 BEGIN **************
        	Point p = null;
            try {
            	//p = c.getLocationOnScreen();
            	p = c.getLocation(null);
            } catch (Exception e) {
            	c = null;
            }
                // HO 04/01/2011 END **************
        	
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

    	Component c = setScreen(map);
        
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
        	// HO 22/02/2011 BEGIN **************
            SaveAction.saveMap(map, false, false);
        	//map = SaveAction.saveMapSpecial(map, false, false);
            // HO 22/02/2011 END **************
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
    	// HO 18/02/2011 BEGIN ******************
    	// default file type is now .vpk
    	// File file = ActionUtil.openFile("Open Map", VueFileFilter.VUE_DESCRIPTION);
    	File file = ActionUtil.openFile("Open Map", VueFileFilter.VPK_DESCRIPTION);
    	// HO 18/02/2011 END ******************
        
    	// if they didn't open a map for any reason
        if (file == null)
            return null;
        
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
    	// HO 19/12/2010 BEGIN *******************
    	//setComponentLabels("source node", "target node");
    	//setWormholeNodeLabels("source wormhole", "target wormhole");
    	setWormholeNodeLabel(getTargetWormholeNode(), VueResources.getString("wormhole.node.target.label.default"));
    	// HO 19/12/2010 END *******************
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
		// HO 19/12/2010 BEGIN ***************
		String strLabel = VueResources.getString("wormhole.node.label.default");
		LWNode theNode = new LWNode(strLabel);
		// HO 19/12/2010 END ***************
		return theNode;
	}
	
	// HO 25/08/2011 BEGIN ********
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
	// HO 30/08/2011 BEGIN *************
	// private LWSelection selectTargetAndOneOtherNode(LWNode nextNode) {
	private LWSelection selectTargetAndOneOtherNode(LWComponent nextNode) {
		// HO 30/08/2011 END *************
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
		// HO 30/08/2011 BEGIN *************
		Iterator iter = targetMap.getChildIterator();
		// cycle through all the nodes in the target map
		while (iter.hasNext()) {			
			// LWNode nextNode = (LWNode) iter.next();
			LWComponent nextComp = (LWComponent) iter.next();			
			// if the next component isn't the target node
			if (nextComp != targetComponent) {
					rippleOutOverlap(nextComp, targetMap);
				}
		}

	}
	
	private void rippleOutOverlap(LWComponent nextNode, LWMap theMap) {
		if (nextNode.getClass() == LWMap.Layer.class) {
			LWMap.Layer theLayer = (LWMap.Layer) nextNode;
			rippleOutLayer(theLayer, theMap);
			return;
		} 
		
		// check that it isn't overlapping with the target node
		boolean bOverlapping = VueUtil.checkCollision(nextNode, targetComponent);
		// HO 30/08/2011 BEGIN *********
		if (!bOverlapping)
			bOverlapping = VueUtil.checkCollision(targetComponent, nextNode);
		// HO 30/08/2011 END *********
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
	// HO 25/08/2011 END **********
	
	/**
	 * A stub for functionality to position the target component
	 * in the target map.
	 * @param parentMap, the map into which to paste this component.
	 */
	public void positionTargetComponent(LWMap parentMap) {
		// HO 25/08/2011 BEGIN ******
		pasteTargetComponent(parentMap);
		makeSureTargetNodeDoesNotOverlap(parentMap);		
		// HO 25/08/2011 END ********
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
    	
    	return theWormholeNode;
	}
	
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
			theComponent.addChild(theWormholeNode);
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
		setSourceResourceMapURI(targetMapFile.toURI());
		setTargetResourceMapURI(sourceMapFile.toURI());
		setSourceResourceComponentURI(targetComponent.getURI());
		setTargetResourceComponentURI(sourceComponent.getURI());
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		}
	}
	
	/**
	 * Convenience method to set the source and target resources.
	 */
	public void createResources() {
		//if (sourceWormholeNode != null)
			//setSourceResource(sourceWormholeNode.getResourceFactory().get(sourceResourceMapURI, sourceResourceComponentURI));
		// HO we could use the following two lines of code
		// if we ever want to implement a kind of wormhole resource
		// that knows about both source and target.
		setSourceResource(sourceWormholeNode.getResourceFactory().get(sourceResourceMapURI, sourceResourceComponentURI,
					targetResourceMapURI, targetResourceComponentURI));
		//if (targetWormholeNode != null)
			//setTargetResource(targetWormholeNode.getResourceFactory().get(targetResourceMapURI, targetResourceComponentURI));
		// HO we could use the following two lines of code
		// if we ever want to implement a kind of wormhole resource
		// that knows about both source and target.	
		setTargetResource(targetWormholeNode.getResourceFactory().get(targetResourceMapURI, targetResourceComponentURI,
					sourceResourceMapURI, sourceResourceComponentURI));
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
					
					// reset only those resources
					// that need to be reset
					// i.e. if only one map has changed,
					// there's no need to repaint both
					// HO 22/09/2010 BEGIN *************
					// this only works if the resources don't contain
					// info about the originating map and component,
					// and now they do have to contain that
					//selectivelyResetResources();
					resetResources();
					// HO 22/09/2010 END *************
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
		// HO 22/02/2011 BEGIN **************
		//SaveAction.saveMap(sourceMap);
		//SaveAction.saveMap(targetMap);
		SaveAction.saveMapSpecial(sourceMap);
		// HO 11/03/2011 BEGIN *************
		// if the source and target maps are the same,
		// saving the same map twice is unnecessary
		if (!sourceMap.equals(targetMap))
			SaveAction.saveMapSpecial(targetMap);
		// HO 11/03/2011 END ***************
		// HO 22/02/2011 END **************
		
		// HO 22/12/2010 BEGIN ***********
		// HO 16/03/2011 BEGIN ***********
		// this should be redundant because saveMapSpecial includes a call to this
			//OpenAction.displayMapSpecial(targetMapFile);
			// HO 16/03/2011 END ***********
		// HO 22/12/2010 END *************
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
			setTargetMap(targetComponent.getParentOfType(LWMap.class));
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
		replaceExistingWormholeResource(targetWormholeNode, targetResource, targetMap);

		// save both maps
		// HO 22/02/2011 BEGIN *************
		// HO 15/03/2011 BEGIN *************
		SaveAction.saveMap(sourceMap);
		//SaveAction.saveMapSpecial(sourceMap);
		// HO 15/03/2011 END *************
		// HO 11/03/2011 BEGIN *************
		// if the source and target maps are the same,
		// saving the same map twice is unnecessary
		// HO 15/03/2011 BEGIN *************
		if (!sourceMap.equals(targetMap)) {
			SaveAction.saveMap(targetMap);
			//SaveAction.saveMapSpecial(targetMap);
		}
			// HO 15/03/2011 END *************
		// HO 11/03/2011 END ******************
		//SaveAction.saveMapSpecial(sourceMap);
		//SaveAction.saveMapSpecial(targetMap);
		// HO 22/02/2011 END *************
		// now reload and redisplay both maps (necessary when
		// making changes to an open map that is not also
		// the active map)
		// HO 15/03/2011 BEGIN *************
		OpenAction.displayMapSpecial(sourceMapFile);
		//OpenAction.displayMap(sourceMapFile);
		// HO 15/03/2011 END *************
		// HO 11/03/2011 BEGIN *************
		// if the source and target maps are the same,
		// saving the same map twice is unnecessary
		// HO 15/03/2011 BEGIN *************
		if (!sourceMap.equals(targetMap)) {
			OpenAction.displayMapSpecial(targetMapFile);
			//OpenAction.displayMap(targetMapFile);	
		}
			// HO 15/03/2011 END *************
		// HO 11/03/2011 END
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
		// HO 22/02/2011 BEGIN ************
		SaveAction.saveMap(sourceMap, false, false);
		// HO 11/03/2011 BEGIN *************
		// if the source and target maps are the same,
		// saving the same map twice is unnecessary
		if (!sourceMap.equals(targetMap))
			SaveAction.saveMap(targetMap, false, false); 
		// HO 11/03/2011 END ***********
		//SaveAction.saveMapSpecial(sourceMap, false, false);
		//SaveAction.saveMapSpecial(targetMap, false, false); 
		// HO 22/02/2011 END ***************
		// now reload and redisplay both maps (necessary when
		// making changes to an open map that is not also
		// the active map)
		OpenAction.displayMapSpecial(sourceMapFile);
		// HO 11/03/2011 BEGIN *************
		// if the source and target maps are the same,
		// saving the same map twice is unnecessary
		if (!sourceMap.equals(targetMap))
			OpenAction.displayMapSpecial(targetMapFile);	
		// HO 11/03/2011 END *****************
	}	
	
	/**
	 * A method to reset only those resources which are necessary
	 * (e.g. if several maps are open but only one has changed,
	 * reset only the resources pertaining to that map)
	 * This only works if you are using the kind of resource
	 * which only knows about one side of the wormhole,
	 * not both
	 */
	/* public void selectivelyResetResources() {
		// first check which map has changed, and reset that
		// if the source map file has changed, reset it
		// and reset its file
		if (!sourceMap.getFile().equals(sourceMapFile)) {
			setSourceMap(sourceComponent.getParentOfType(LWMap.class));
			// now reset the file
			setSourceMapFile(sourceMap.getFile());
			// if the source map file has changed,
			// that needs to be reflected in the target resource
			// so if the URI the source map file has doesn't equal
			// the one the target has got for it, reset the target one
			if (!sourceMapFile.toURI().equals(targetResourceMapURI)) {
				setTargetResourceMapURI(sourceMapFile.toURI());
			}
			// now use this information to recreate the resources
			if (targetWormholeNode != null) {
				// HO 22/09/2010 BEGIN *****************
				setTargetResource(targetWormholeNode.getResourceFactory().get(targetResourceMapURI, targetResourceComponentURI));
				//setTargetResource(targetWormholeNode.getResourceFactory().get(targetResourceMapURI, targetResourceComponentURI,
						//sourceResourceMapURI, sourceResourceComponentURI, false));
				// HO 22/09/2010 END *****************
				
			}
			// and replace the old resource with the new one
			replaceExistingWormholeResource(targetWormholeNode, targetResource, targetMap);
			// save both maps
			// HO 22/02/2011 BEGIN ************
			SaveAction.saveMap(sourceMap);
			// HO 11/03/2011 BEGIN *************
			// if the source and target maps are the same,
			// saving the same map twice is unnecessary
			if (!sourceMap.equals(targetMap))
				SaveAction.saveMap(targetMap); 
			// HO 11/03/2011 END *****************
			//SaveAction.saveMapSpecial(sourceMap);
			//SaveAction.saveMapSpecial(targetMap);
			// HO 22/02/2011 END **************
			// now reload and redisplay the *other* map (necessary when
			// making changes to an open map that is not also
			// the active map)
				OpenAction.displayMapSpecial(targetMapFile);	
		}
		// if the target map file has changed, reset it
		if (!targetMap.getFile().equals(targetMapFile)) {
			setTargetMap(targetComponent.getParentOfType(LWMap.class));
			setTargetMapFile(targetMap.getFile());
			// if the target map file has changed,
			// that needs to be reflected in the source resource
			// so if the URI the target map file has doesn't equal
			// the one the source has got for it, reset the source one
			if (!targetMapFile.toURI().equals(sourceResourceMapURI))
				setSourceResourceMapURI(targetMapFile.toURI());
			// now use this information to recreate the resources
			if (sourceWormholeNode != null) {
				// HO 22/09/2010 BEGIN *****************
				setSourceResource(sourceWormholeNode.getResourceFactory().get(sourceResourceMapURI, sourceResourceComponentURI));
				//setSourceResource(sourceWormholeNode.getResourceFactory().get(sourceResourceMapURI, sourceResourceComponentURI,
						//targetResourceMapURI, targetResourceComponentURI, false));
				// HO 22/09/2010 END *****************
			}
			// and replace the old resource with the new one
			replaceExistingWormholeResource(sourceWormholeNode, sourceResource, sourceMap);
			// save both maps
			// HO 22/02/2011 BEGIN **************
			SaveAction.saveMap(sourceMap);
			// HO 11/03/2011 BEGIN *************
			// if the source and target maps are the same,
			// saving the same map twice is unnecessary
			if (!sourceMap.equals(targetMap))
				SaveAction.saveMap(targetMap); 
			// HO 11/03/2011 END ***********
			//SaveAction.saveMapSpecial(sourceMap);
			//SaveAction.saveMapSpecial(targetMap); 
			// HO 22/02/2011 END ****************
			// now reload and redisplay the *other* map (necessary when
			// making changes to an open map that is not also
			// the active map)
				OpenAction.displayMapSpecial(sourceMapFile);	
		}
	} */
	
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
	
	// HO 12/05/2011 BEGIN *********
	/**
	 * @param stripThis, a String representing a filename that has its spaces
	 * in the HTML format
	 * @return the same String, html space codes replaced with single spaces
	 */
	private String stripHtmlSpaceCodes(String stripThis) {
		String strStripped = "";
		String strPeskySpace = "%20";
		String strCleanSpace = " ";

		strStripped = stripThis.replaceAll(strPeskySpace, strCleanSpace);
		
		return strStripped;		
	}
	// HO 12/05/2011 END ***********
	
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
	
	// HO 08/02/2011 BEGIN *********************
	/**
	 * A routine to find and save all the open
	 * maps
	 */
	// HO 02/08/2011 BEGIN *************
	//public Collection<LWMap> findAndSaveAllOpenMaps() {
	public void findAndSaveAllOpenMaps() {
		// HO 02/08/2011 END *************
		
		// get all the open maps
		Collection<LWMap> coll = VUE.getAllMaps();
		for (LWMap map: coll) {
			if ((map.isModified()) && (map.getFile() != null)) {
				// here's the current map, save it
				// HO 22/02/2011 BEGIN ************
				SaveAction.saveMap(map);
				//SaveAction.saveMapSpecial(map);
				// HO 22/02/2011 END **************
			}
			map = null;
		}
		
		coll = null;
		//return coll;
	}
	// HO 08/02/2011 END *********************
	
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
		// get the resource that that component already has
        Resource activeResource = theComponent.getResource();
        // if the resource we passed in isn't null,
        // replace the resource we just got from the
        // component with the one we passed in
        // start by assuming the two resources are  the same
        boolean bTheSame = true;
        if (newResource != null) {
        	// HO 14/04/2011 BEGIN *********
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
        	// HO 14/04/2011 END *********
            activeResource = newResource;
        }
	
        // but if both resources are null,
        // we've got nothing to work with so return
        if (activeResource == null)
            return;
        
        // HO 14/04/2011 BEGIN *********
        // if both resources are the same,
        // we also have nothing to work with so return
        if (bTheSame == true)
        	return;
        // HO 14/04/2011 END *********

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
