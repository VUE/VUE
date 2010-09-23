package tufts.vue;

import java.awt.geom.RectangularShape;
import java.io.*;
import java.net.*;

import tufts.Util;
import tufts.vue.XLinkResource.XLinkType;

// HO 15/07/2010 BEGIN *********************
// a new class, which implements wormhole linking
// between two nodes
public class LWWormholeNode extends LWNode {
	
	/**
	 * The URI String of this wormhole's source map file
	 */
	private String sourceMapURIString;
	
	/**
	 * The URI String of the source component which is
	 * the point of origin for this wormhole.
	 */
	private String sourceComponentURIString;
	
	/**
	 * The URI String of the target component which is
	 * the endpoint for this wormhole.
	 */
	private String targetComponentURIString;
	
	/**
	 * The URI String of this wormhole's target map file
	 */
	private String targetMapURIString;
	
	/**
	 * The type of wormhole this is
	 */
	private String wormholeType;
	
	/**
	 * @param sourceComponentURIString, the URI Stringfor the starting point of the wormhole
	 * @param sourceMapURIString, the URI String for the map containing the source component
	 * @param targetComponentURIString, the URI String for the endpoint of the wormhole
	 * @param targetMapURIString, the URI String for the map containing the target component
	 * @param wormholeType, flags this as a source or a target wormhole
	 */
	public LWWormholeNode(String sourceComponentURIString, String sourceMapURIString,
			String targetComponentURIString, String targetMapURIString, WormholeType wormholeType) {
		init(sourceComponentURIString, sourceMapURIString,
			targetComponentURIString, targetMapURIString, wormholeType);
	}
	
	/**
	 * @param sourceComponentURIString, the URI String
	 * for the LWComponent which is the point of origin for the wormhole
	 * @param sourceMapURIString, the URI String for the LWMap containing the source component
	 * @param targetComponentURIString, the URI String for the LWComponent 
	 * which is the endpoint for the wormhole
	 * @param targetMapURIString, the URI String for theLWMap 
	 * containing the endpoint component for the wormhole
	 * @param wormholeType, a WormholeType saying what type of wormhole this is for
	 * initializes all the variables for the file locations and URIs of the
	 * source and target components and maps.
	 */
	private void init(String sourceComponentURIString, String sourceMapURIString,
			String targetComponentURIString, String targetMapURIString, WormholeType wormholeType) {
		setSourceComponentURIString(sourceComponentURIString);
		setSourceMapURIString(sourceMapURIString);
		setTargetComponentURIString(targetComponentURIString);
		setTargetMapURIString(targetMapURIString);
		setWormholeType(wormholeType.toString());

	}	

	public LWWormholeNode(String label, Resource resource) {
		super(label, resource);
		// TODO Auto-generated constructor stub
	}
	
	public LWWormholeNode() {
		
	}
	
	/**
	 * Sets the URI Stringfor the source map.
	 * @param theURIString, the URI Stringfor the source map.
	 */
	public void setSourceMapURIString(String theURIString) {
		sourceMapURIString = theURIString;
	}
	
	/**
	 * @return sourceMapURIString, the URIString of the
	 * wormhole's source map.
	 */
	public String getSourceMapURIString() {
		return sourceMapURIString;
	} 
	
	/**
	 * Sets the URI String for the component which is the
	 * point of origin for this wormhole.
	 * @param theURIString, the URI String for the source component.
	 */
	public void setSourceComponentURIString (String theURIString) {
		sourceComponentURIString = theURIString;
	}
	
	/**
	 * @return sourceComponentURIString, the URI String for the component
	 * which is the point of origin for this wormhole.
	 */
	public String getSourceComponentURIString() {
		return sourceComponentURIString;
	}
	
	/**
	 * Sets the URI String for the target map.
	 * @param theURIString, the URI String for the target map
	 */
	public void setTargetMapURIString(String theURIString) {
		targetMapURIString = theURIString;
	} 
	
	/**
	 * @return targetMapURIString, the URI String of the
	 * wormhole's target map.
	 */
	public String getTargetMapURIString() {
		return targetMapURIString;
	} 	
	
	/**
	 * Sets the URI String for the component which is the
	 * endpoint for this wormhole.
	 * @param theURIString, the URI String for the target component.
	 */
	public void setTargetComponentURIString(String theURIString) {
		targetComponentURIString = theURIString;
	}
	
	/**
	 * @return targetComponentURIString, the URI String for the component
	 * which is the endpoint for this wormhole.
	 */
	public String getTargetComponentURIString() {
		return targetComponentURIString;
	}
	
	public enum WormholeType {
		SOURCE("source"), TARGET("target"); 
		
		WormholeType(String strType) {
			this.type = strType;
		}
		
		private String type;
		
		public String getType() {
			return type;
		}
	}
	
	/**
	 * @param theType, a WormholeType which will become the type of this wormhole node
	 */
	public void setWormholeType(String theType) {
		this.wormholeType = theType;
	}
	
	/**
	 * @return wormholeType, a String representation of the WormholeType representing the type of this wormhole node
	 */
	public String getWormholeType() {
		return wormholeType.toString();
	} 	
	
    

}

//HO 15/07/2010 END *********************
