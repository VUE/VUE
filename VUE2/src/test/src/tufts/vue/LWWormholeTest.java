package test.src.tufts.vue;

import java.io.*;
import java.lang.*;
import java.net.*;

import tufts.vue.*;
import tufts.vue.action.ExitAction;
import tufts.vue.action.SaveAction;
import junit.framework.*;

public class LWWormholeTest extends TestCase {
	
	private LWWormhole lwWormhole;
	private LWNode lwSourceNode;
	private LWNode lwTargetNode;
	private LWMap lwSourceMap;
	private LWMap lwTargetMap;

	protected void setUp() {
		lwWormhole = new LWWormhole();
		lwSourceNode = new LWNode("test source node");
		lwSourceMap = new LWMap("test source map");
		lwTargetNode = new LWNode("test target node");
		lwTargetMap = new LWMap("test target map");
	}
	
	protected void tearDown() {
		lwWormhole = null;
		lwSourceNode = null;
		lwSourceMap = null;
		lwTargetNode = null;
		lwTargetMap = null;
	}
	
	public void testLWWormhole () {
		// make sure we have an instance of VUE running
		String[] theargs = new String[1];
		theargs[0] = "-debug";		
		VUE.main(theargs); 
		// first try the no-arg constructor, which should
		// make nothing happen
		assertTrue(!lwWormhole.equals(null));
		// now pass a component in, making sure that
		// the component has a map and the map has a file
		// which put together should be enough to create a wormhole
		lwSourceMap.addChild(lwSourceNode);
		File sourceFile = new File(System.getProperty("user.dir") + "//sourceMapFile.vue");
		lwSourceMap.setFile(sourceFile);
		// construct it properly, it should work
		LWWormhole theWormhole = new LWWormhole(lwSourceNode, false);
		assertTrue(theWormhole.getBCancelled() == false); 
		// construct it properly again, it should work
		theWormhole = new LWWormhole(lwSourceNode, true);
		assertTrue(theWormhole.getBCancelled() == false);
		// pass in a null component, it should cancel itself
		theWormhole = new LWWormhole(null, false);
		assertTrue(theWormhole.getBCancelled() == true);

    	// now try the second constructor
		// reset the wormhole
		theWormhole = new LWWormhole(lwSourceNode, false);
		LWWormholeNode wn = theWormhole.getSourceWormholeNode();
		WormholeResource wr = (WormholeResource)theWormhole.getSourceResource();
		theWormhole = null;
		theWormhole = new LWWormhole(wn, wr);
		assertTrue(theWormhole.getBCancelled() == false);
		// now pass in a duff WormholeNode
		theWormhole = new LWWormhole(null, wr);
		assertTrue(theWormhole.getBCancelled() == true);
		// reset the wormhole
		theWormhole = new LWWormhole(wn, wr);
		assertTrue(theWormhole.getBCancelled() == false);
		// now pass in a duff WormholeResource
		theWormhole = new LWWormhole(wn, null);
		assertTrue(theWormhole.getBCancelled() == true);
		
		// now try the third constructor
		// reset the wormhole
		theWormhole = new LWWormhole(wn, wr);
		File beingSavedTo = new File(System.getProperty("user.dir") + "//sourceMapFile2.vue");
		wn = theWormhole.getSourceWormholeNode();
		wr = (WormholeResource)theWormhole.getSourceResource();
		theWormhole = null;
		theWormhole = new LWWormhole(wn, wr, beingSavedTo, lwSourceNode);
		assertTrue(theWormhole.getBCancelled() == false); 
		// give it a null WormholeNode
		theWormhole = new LWWormhole(null, wr, beingSavedTo, lwSourceNode);
		assertTrue(theWormhole.getBCancelled() == true); 
		// give it a null WormholeResource
		theWormhole = new LWWormhole(wn, null, beingSavedTo, lwSourceNode);
		assertTrue(theWormhole.getBCancelled() == true); 
		// give it a null file
		theWormhole = new LWWormhole(wn, wr, "", lwSourceNode);
		assertTrue(theWormhole.getBCancelled() == true); 
		// give it a null source node
		theWormhole = new LWWormhole(wn, wr, beingSavedTo, null);
		assertTrue(theWormhole.getBCancelled() == true); 

	} 

	public void testInit() {
		lwSourceMap.addChild(lwSourceNode);
		File sourceFile = new File(System.getProperty("user.dir") + "//sourceMapFile.vue");
		lwSourceMap.setFile(sourceFile);
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.init(lwSourceNode, false);
		assertTrue(lwWormhole.getBCancelled() == false);
		// reset the wormhole
		lwWormhole = null;
		lwWormhole = new LWWormhole();
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.init(lwSourceNode, true);
		assertTrue(lwWormhole.getBCancelled() == false);
		// reset the wormhole
		lwWormhole = null;
		lwWormhole = new LWWormhole();
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.init(null, false);
		assertTrue(lwWormhole.getBCancelled() == true);
		// reset the wormhole
		lwWormhole = null;
		lwWormhole = new LWWormhole();
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.init(null, true);
		assertTrue(lwWormhole.getBCancelled() == true);
		// reset the wormhole
		lwWormhole.init(lwSourceNode, false);
		
		// now test the second version of init()
		LWWormholeNode wn = lwWormhole.getSourceWormholeNode();
		WormholeResource wr = (WormholeResource)lwWormhole.getSourceResource();
		lwWormhole = null;
		lwWormhole = new LWWormhole();
		lwWormhole = new LWWormhole(wn, wr);
		assertTrue(lwWormhole.getBCancelled() == false);
		// reset the wormhole
		lwWormhole = null;
		lwWormhole = new LWWormhole();
		// give it a duff node
		lwWormhole.init(null, wr);
		assertTrue(lwWormhole.getBCancelled() == true);
		// reset the wormhole
		lwWormhole = null;
		lwWormhole = new LWWormhole();
		// give it a duff resource
		lwWormhole.init(wn, null);
		assertTrue(lwWormhole.getBCancelled() == true);
		// reset the wormhole
		lwWormhole.init(wn, wr);
		
		// now try the third version
		File beingSavedTo = new File(System.getProperty("user.dir") + "//sourceMapFile2.vue");
		wn = lwWormhole.getSourceWormholeNode();
		wr = (WormholeResource)lwWormhole.getSourceResource();
		lwWormhole = null;
		lwWormhole = new LWWormhole();
		lwWormhole.init(wn, wr, beingSavedTo, lwSourceNode);
		assertTrue(lwWormhole.getBCancelled() == false);
		
		// reset the wormhole
		lwWormhole.init(wn, wr);
		// give it a duff parameter
		lwWormhole = null;
		lwWormhole = new LWWormhole();
		lwWormhole.init(null, wr, beingSavedTo, lwSourceNode);
		assertTrue(lwWormhole.getBCancelled() == true);

		// give it a duff parameter
		lwWormhole = null;
		lwWormhole = new LWWormhole();
		lwWormhole.init(wn, null, beingSavedTo, lwSourceNode);
		assertTrue(lwWormhole.getBCancelled() == true);
		
		// give it a duff parameter
		lwWormhole = null;
		lwWormhole = new LWWormhole();
		lwWormhole.init(wn, wr, "", lwSourceNode);
		assertTrue(lwWormhole.getBCancelled() == true);
		
		// give it a duff parameter
		lwWormhole = null;
		lwWormhole = new LWWormhole();
		lwWormhole.init(wn, wr, beingSavedTo, null);
		assertTrue(lwWormhole.getBCancelled() == true);
	} 
	
	public void testSetWormholeResources() {
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwSourceMap.setFile(sourceFile);
		lwWormhole.setSourceMap(lwSourceMap);
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwTargetMap.setFile(targetFile);
		lwWormhole.setTargetMap(lwTargetMap);
		lwWormhole.setSourceAndTargetMapFiles();
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setSourceResourceComponentURI(lwTargetNode.getURI());
		lwWormhole.setTargetResourceComponentURI(lwSourceNode.getURI());
    	// create the source wormhole node
    	LWWormholeNode sourceWormholeNode = new LWWormholeNode(lwSourceNode.getURI().toString(),
    			lwSourceMap.getURI().toString(), lwTargetNode.getURI().toString(),
    			lwTargetMap.getURI().toString(), LWWormholeNode.WormholeType.SOURCE);
    	// create the target wormhole node
    	LWWormholeNode targetWormholeNode = new LWWormholeNode(lwSourceNode.getURI().toString(),
    			lwSourceMap.getURI().toString(), lwTargetNode.getURI().toString(),
    			lwTargetMap.getURI().toString(), LWWormholeNode.WormholeType.TARGET);
    	lwWormhole.setSourceWormholeNode(sourceWormholeNode);
    	// create the target wormhole node
    	lwWormhole.setTargetWormholeNode(targetWormholeNode);    	
    	lwWormhole.setComponentLabels("source node", "target node");
    	lwWormhole.setWormholeNodeLabels("source wormhole", "target wormhole");
    	// add the wormhole nodes to their components;
    	lwWormhole.placeWormholeNodesAsChildren();
    	// okay, now place the target node in the target map
    	lwWormhole.positionTargetComponent(lwTargetMap);	
		lwWormhole.setWormholeResources();
		assertTrue(lwWormhole.getSourceResource() != null);
		assertTrue(lwWormhole.getTargetResource() != null);
		assertTrue(lwWormhole.getSourceResource().getSpec().equals(targetFile.toURI().toString()));
		assertTrue(lwWormhole.getTargetResource().getSpec().equals(sourceFile.toURI().toString()));
		assertTrue(lwWormhole.getSourceResource().getClass().equals(WormholeResource.class));
		assertTrue(lwWormhole.getTargetResource().getClass().equals(WormholeResource.class));
		WormholeResource wrSource = (WormholeResource)lwWormhole.getSourceResource();
		assertTrue(wrSource.getComponentURIString().equals(lwTargetNode.getURI().toString()));
		WormholeResource wrTarget = (WormholeResource)lwWormhole.getTargetResource();
		assertTrue(wrTarget.getComponentURIString().equals(lwSourceNode.getURI().toString()));
		// test that a null file throws a null pointer exception
		lwSourceMap.setFile(null);
		try {
			lwWormhole.setWormholeResources();
		} catch (Exception e) {
			assertTrue(e.getClass().equals(java.lang.NullPointerException.class));
		}
		// reset the source map file
		lwSourceMap.setFile(sourceFile);
		// try a null component - that should also get us a null pointer exception
		lwWormhole.setTargetComponent(null);
		try {
			lwWormhole.setWormholeResources();
		} catch (Exception e) {
			assertTrue(e.getClass().equals(java.lang.NullPointerException.class));
		}
		// reset the component
		lwWormhole.setTargetComponent(lwTargetNode);
		// now try a null map URI - that shouldn't matter because it should be reset
		lwWormhole.setSourceResourceMapURI(null);
		lwWormhole.setWormholeResources();
		assertTrue(lwWormhole.getSourceResourceMapURI().equals(targetFile.toURI()));
		// the same should be true of the component URIs
		lwWormhole.setSourceResourceComponentURI(null);
		lwWormhole.setWormholeResources();
		assertTrue(lwWormhole.getSourceResourceComponentURI().equals(lwTargetNode.getURI()));
		// give it a null wormhole node
		lwWormhole.setTargetWormholeNode(null);
		// we should now get a null pointer exception
		try {
			lwWormhole.setWormholeResources();
		} catch (Exception e) {
			assertTrue(e.getClass().equals(java.lang.NullPointerException.class));
		}
	}
	
	// tricky to test this as it requires a map viewer
	public void testResetResources() {
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwSourceMap.setFile(sourceFile);
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwTargetMap.setFile(targetFile);
		lwWormhole.setSourceComponent(lwSourceNode);
		lwSourceMap.addChild(lwSourceNode);
		lwWormhole.setTargetComponent(lwTargetNode);
    	// create the source wormhole node
    	LWWormholeNode sourceWormholeNode = new LWWormholeNode(lwSourceNode.getURI().toString(),
    			lwSourceMap.getURI().toString(), lwTargetNode.getURI().toString(),
    			lwTargetMap.getURI().toString(), LWWormholeNode.WormholeType.SOURCE);
    	// create the target wormhole node
    	LWWormholeNode targetWormholeNode = new LWWormholeNode(lwSourceNode.getURI().toString(),
    			lwSourceMap.getURI().toString(), lwTargetNode.getURI().toString(),
    			lwTargetMap.getURI().toString(), LWWormholeNode.WormholeType.TARGET);
    	lwWormhole.setSourceWormholeNode(sourceWormholeNode);
    	// create the target wormhole node
    	lwWormhole.setTargetWormholeNode(targetWormholeNode);    	
    	lwWormhole.setComponentLabels("source node", "target node");
    	lwWormhole.setWormholeNodeLabels("source wormhole", "target wormhole");
    	// add the wormhole nodes to their components;
    	lwWormhole.placeWormholeNodesAsChildren();
    	// okay, now place the target node in the target map
    	lwWormhole.positionTargetComponent(lwTargetMap);	
		lwWormhole.resetResources();
		assertTrue(lwWormhole.getSourceResource() != null);
		assertTrue(lwWormhole.getTargetResource() != null);
		// now mess with it - removing the map files
		// should affect nothing
		lwWormhole.setSourceMapFile(null);
		lwWormhole.setTargetMapFile(null);
		lwWormhole.resetResources();
		assertTrue(lwWormhole.getSourceResource() != null);
		assertTrue(lwWormhole.getTargetResource() != null);
		// nullifying the maps should also do nothing
		lwWormhole.setSourceMap(null);
		lwWormhole.setTargetMap(null);
		lwWormhole.resetResources();
		assertTrue(lwWormhole.getSourceResource() != null);
		assertTrue(lwWormhole.getTargetResource() != null);
		// nullifying the components should screw things up nicely though!
		lwWormhole.setSourceComponent(null);
		// we should now get a null pointer exception
		try {
			lwWormhole.resetResources();
		} catch (Exception e) {
			assertTrue(e.getClass().equals(java.lang.NullPointerException.class));
		}
		// reset the source component
		lwWormhole.setSourceComponent(lwSourceNode);
		// now try giving it the "wrong" (non-node) kind of component
		LWComponent c = new LWMap("testing");
		lwWormhole.setSourceComponent(c);
		// that should be okay
		assertTrue(lwWormhole.getSourceResource() != null);
		assertTrue(lwWormhole.getTargetResource() != null);
		
	} 
	
	public void testSetBSaving() {
		lwWormhole.setBSaving(true);
		assertTrue(lwWormhole.getBSaving() == true);
	}
	
	public void testGetBSaving() {
		lwWormhole.setBSaving(true);
		assertTrue(lwWormhole.getBSaving() == true);
	}
	
	public void testSetBCancelled() {
		lwWormhole.setBCancelled(false);
		assertTrue(lwWormhole.getBCancelled() == false);
	}
	
	public void testGetBCancelled() {
		lwWormhole.setBCancelled(false);
		assertTrue(lwWormhole.getBCancelled() == false);
	}
	
	// difficult to test this as it requires a mapviewer
	public void testResetResourcesDuringSave() {
		lwWormhole.setSourceMap(lwSourceMap);
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwSourceMap.setFile(sourceFile);
		lwWormhole.setSourceMapFile(sourceFile);
		lwWormhole.setTargetMap(lwTargetMap);
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwTargetMap.setFile(targetFile);
		lwWormhole.setTargetMapFile(targetFile);
		lwWormhole.setSourceComponent(lwSourceNode);
		lwSourceMap.addChild(lwSourceNode);
		lwWormhole.setTargetComponent(lwTargetNode);
    	// create the source wormhole node
    	LWWormholeNode sourceWormholeNode = new LWWormholeNode(lwSourceNode.getURI().toString(),
    			lwSourceMap.getURI().toString(), lwTargetNode.getURI().toString(),
    			lwTargetMap.getURI().toString(), LWWormholeNode.WormholeType.SOURCE);
    	// create the target wormhole node
    	LWWormholeNode targetWormholeNode = new LWWormholeNode(lwSourceNode.getURI().toString(),
    			lwSourceMap.getURI().toString(), lwTargetNode.getURI().toString(),
    			lwTargetMap.getURI().toString(), LWWormholeNode.WormholeType.TARGET);
    	lwWormhole.setSourceWormholeNode(sourceWormholeNode);
    	// create the target wormhole node
    	lwWormhole.setTargetWormholeNode(targetWormholeNode);    	
    	lwWormhole.setComponentLabels("source node", "target node");
    	lwWormhole.setWormholeNodeLabels("source wormhole", "target wormhole");
    	// add the wormhole nodes to their components;
    	lwWormhole.placeWormholeNodesAsChildren();
    	// okay, now place the target node in the target map
    	lwWormhole.positionTargetComponent(lwTargetMap);   	
    	lwWormhole.resetResourcesDuringSave();
		assertTrue(lwWormhole.getSourceResource() != null);
		assertTrue(lwWormhole.getTargetResource() != null);
	} 
	
	// difficult to test this as it requires a mapviewer
	public void testSelectivelyResetResources() {
		lwWormhole.setSourceMap(lwSourceMap);
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwSourceMap.setFile(sourceFile);
		lwWormhole.setSourceMapFile(sourceFile);
		lwWormhole.setTargetMap(lwTargetMap);
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwTargetMap.setFile(targetFile);
		lwWormhole.setTargetMapFile(targetFile);
		lwWormhole.setSourceComponent(lwSourceNode);
		lwSourceMap.addChild(lwSourceNode);
		lwWormhole.setTargetComponent(lwTargetNode);
    	// create the source wormhole node
    	LWWormholeNode sourceWormholeNode = new LWWormholeNode(lwSourceNode.getURI().toString(),
    			lwSourceMap.getURI().toString(), lwTargetNode.getURI().toString(),
    			lwTargetMap.getURI().toString(), LWWormholeNode.WormholeType.SOURCE);
    	// create the target wormhole node
    	LWWormholeNode targetWormholeNode = new LWWormholeNode(lwSourceNode.getURI().toString(),
    			lwSourceMap.getURI().toString(), lwTargetNode.getURI().toString(),
    			lwTargetMap.getURI().toString(), LWWormholeNode.WormholeType.TARGET);
    	lwWormhole.setSourceWormholeNode(sourceWormholeNode);
    	// create the target wormhole node
    	lwWormhole.setTargetWormholeNode(targetWormholeNode);    	
    	lwWormhole.setComponentLabels("source node", "target node");
    	lwWormhole.setWormholeNodeLabels("source wormhole", "target wormhole");
    	// add the wormhole nodes to their components;
    	lwWormhole.placeWormholeNodesAsChildren();
    	// okay, now place the target node in the target map
    	lwWormhole.positionTargetComponent(lwTargetMap); 
    	// now set the uris
    	lwWormhole.setSourceResourceMapURI(targetFile.toURI());
    	lwWormhole.setTargetResourceMapURI(sourceFile.toURI());
    	lwWormhole.setSourceResourceComponentURI(lwTargetNode.getURI());
    	lwWormhole.setTargetResourceComponentURI(lwSourceNode.getURI());
    	lwWormhole.selectivelyResetResources();
    	// at this point, neither file has changed, so
    	// the resources shouldn't ever have been reset (or set)
		assertTrue(lwWormhole.getSourceResource() == null);
		assertTrue(lwWormhole.getTargetResource() == null);
		// difficult to test the opposite case as it's hard to 
		// reset either map file from here
	} 
	
	public void testAddAllListeners() {
		lwWormhole.addAllListeners();
	}
	
	public void testCreateTargetComponent() {
		lwWormhole.setSourceComponent(lwSourceNode);
		LWComponent theComponent = lwWormhole.createDuplicateTargetComponent();
		assertTrue(!theComponent.equals(null));
	}
	
	public void testPositionTargetComponent() {
		lwWormhole.setTargetMap(lwTargetMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.positionTargetComponent(lwTargetMap);
		assertTrue(lwWormhole.getTargetMap().hasChildren());
	}
	
	public void testSetSourceComponent() {
		lwWormhole.setSourceComponent(lwSourceNode);
		assertTrue(lwWormhole.getSourceComponent().equals(lwSourceNode));
	}
	
	public void testSetSourceMap() {
		lwWormhole.setSourceMap(lwSourceMap);
		assertTrue(lwWormhole.getSourceMap().equals(lwSourceMap));
	}
	

	
	public void testSetTargetComponent() {
		lwWormhole.setTargetComponent(lwTargetNode);
		assertTrue(!lwWormhole.getTargetComponent().equals(null));
	}
	

	
	public void testCreateTargetMap() {
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.positionTargetComponent(lwSourceMap);
		LWMap theMap = lwWormhole.createTargetMap();
		assertTrue(theMap.equals(lwSourceMap));
	}
	
	public void testSetTargetMap() {
		lwWormhole.setTargetMap(lwTargetMap);
		assertTrue(lwWormhole.getTargetMap().equals(lwTargetMap));
	}
	
	public void testSetSourceMapFile() {
		File sourceFile = new File(System.getProperty("user.dir") + "//sourceMapFile.vue");
		assertTrue(sourceFile != null);
		lwSourceMap.setFile(sourceFile);
		assertTrue(lwSourceMap.getFile().equals(sourceFile));
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setSourceMapFile(sourceFile);
		assertTrue(lwWormhole.getSourceMapFile().equals(sourceFile));
	} 
	
	public void testGetSourceMapFile() {
		File sourceFile = new File(System.getProperty("user.dir") + "//sourceMapFile.vue");
		assertTrue(sourceFile != null);
		lwSourceMap.setFile(sourceFile);
		assertTrue(lwSourceMap.getFile().equals(sourceFile));
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setSourceMapFile(sourceFile);
		assertTrue(lwWormhole.getSourceMapFile().equals(sourceFile));
	} 
	
	public void testSetTargetMapFile() {
		File targetFile = new File(System.getProperty("user.dir") + "//targetMapFile.vue");
		lwTargetMap.setFile(targetFile);
		lwWormhole.setTargetMap(lwTargetMap);
		lwWormhole.setTargetMapFile(targetFile);
		assertTrue(lwWormhole.getTargetMapFile().equals(targetFile));
	} 
	
	public void testGetTargetMapFile() {
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwTargetMap.setFile(targetFile);
		lwWormhole.setTargetMap(lwTargetMap);
		lwWormhole.setTargetMapFile(targetFile);
		assertTrue(lwWormhole.getTargetMapFile().equals(targetFile));
	} 	
	
	
	public void testSetSourceAndTargetMapFiles() {
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwSourceMap.setFile(sourceFile);
		lwWormhole.setSourceMap(lwSourceMap);
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwTargetMap.setFile(targetFile);
		lwWormhole.setTargetMap(lwTargetMap);
		lwWormhole.setSourceAndTargetMapFiles();
		assertTrue(!lwWormhole.getSourceMapFile().equals(null));
		assertTrue(!lwWormhole.getTargetMapFile().equals(null));
	}
	
	public void testCreateSourceWormholeNode() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode theWormholeNode = lwWormhole.createSourceWormholeNode();
		assertTrue(!theWormholeNode.equals(null));
	}
	
	public void testSetSourceWormholeNode() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode theWormholeNode = lwWormhole.createSourceWormholeNode();
		lwWormhole.setSourceWormholeNode(theWormholeNode);
		assertTrue(lwWormhole.getSourceWormholeNode().equals(theWormholeNode));
	}
	
	public void testGetSourceWormholeNode() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode theWormholeNode = lwWormhole.createSourceWormholeNode();
		lwWormhole.setSourceWormholeNode(theWormholeNode);
		assertTrue(lwWormhole.getSourceWormholeNode().equals(theWormholeNode));
	}	
	
	public void testCreateTargetWormholeNode() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode theWormholeNode = lwWormhole.createTargetWormholeNode();
		assertTrue(!theWormholeNode.equals(null));
	}	
	
	public void testSetTargetWormholeNode() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode theWormholeNode = lwWormhole.createTargetWormholeNode();
		lwWormhole.setTargetWormholeNode(theWormholeNode);
		assertTrue(lwWormhole.getTargetWormholeNode().equals(theWormholeNode));
	}	
	
	public void testGetTargetWormholeNode() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode theWormholeNode = lwWormhole.createTargetWormholeNode();
		lwWormhole.setTargetWormholeNode(theWormholeNode);
		assertTrue(lwWormhole.getTargetWormholeNode().equals(theWormholeNode));
	}	
	
	public void testSetComponentLabel() {
		lwWormhole.setComponentLabel(lwSourceNode, "test label");
		assertTrue(lwSourceNode.getLabel().equals("test label"));
	}
	
	public void testSetComponentLabels() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setTargetComponent(lwTargetNode);
		String strSourceLabel = "test source node";
		String strTargetLabel = "test target node";
		lwWormhole.setComponentLabels(strSourceLabel, strTargetLabel);
		assertTrue(lwWormhole.getSourceComponent().getLabel().equals(strSourceLabel));
		assertTrue(lwWormhole.getTargetComponent().getLabel().equals(strTargetLabel));
	}
	
	public void testSetWormholeNodeLabel() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode sourceWormholeNode = lwWormhole.createSourceWormholeNode();
		lwWormhole.setSourceWormholeNode(sourceWormholeNode);
		String strSourceLabel = "test source wormhole";
		lwWormhole.setWormholeNodeLabel(sourceWormholeNode, strSourceLabel);
		assertTrue(lwWormhole.getSourceWormholeNode().getLabel().equals(strSourceLabel));
	}
	
	public void testSetWormholeNodeLabels() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode sourceWormholeNode = lwWormhole.createSourceWormholeNode();
		lwWormhole.setSourceWormholeNode(sourceWormholeNode);
		LWWormholeNode targetWormholeNode = lwWormhole.createTargetWormholeNode();
		lwWormhole.setTargetWormholeNode(targetWormholeNode);
		String strSourceLabel = "test source wormhole";
		String strTargetLabel = "test target wormhole";
		lwWormhole.setWormholeNodeLabels(strSourceLabel, strTargetLabel);
		assertTrue(lwWormhole.getSourceWormholeNode().getLabel().equals(strSourceLabel));
		assertTrue(lwWormhole.getTargetWormholeNode().getLabel().equals(strTargetLabel));
	}
	
	public void testPlaceWormholeNodeAsChild() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode targetWormholeNode = lwWormhole.createTargetWormholeNode();
		lwWormhole.placeWormholeNodeAsChild(lwSourceNode, targetWormholeNode);
		assertTrue(lwSourceNode.hasChild(targetWormholeNode));
	}
	
	public void testPlaceWormholeNodesAsChildren() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode sourceWormholeNode = lwWormhole.createSourceWormholeNode();
		lwWormhole.setSourceWormholeNode(sourceWormholeNode);
		LWWormholeNode targetWormholeNode = lwWormhole.createTargetWormholeNode();
		lwWormhole.setTargetWormholeNode(targetWormholeNode);
		lwWormhole.placeWormholeNodesAsChildren();
		assertTrue(lwSourceNode.hasChild(sourceWormholeNode));
		assertTrue(lwTargetNode.hasChild(targetWormholeNode));
	}	
	
	public void testSetSourceResourceMapURI() {
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwSourceMap.setFile(sourceFile);
		URI theURI = lwSourceMap.getFile().toURI();
		lwWormhole.setSourceResourceMapURI(theURI);
		assertTrue(lwWormhole.getSourceResourceMapURI().equals(theURI));
	}
	
	public void testGetSourceResourceMapURI() {
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwSourceMap.setFile(sourceFile);
		URI theURI = lwSourceMap.getFile().toURI();
		lwWormhole.setSourceResourceMapURI(theURI);
		assertTrue(lwWormhole.getSourceResourceMapURI().equals(theURI));
	}
	
	public void testSetTargetResourceMapURI() {
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwTargetMap.setFile(targetFile);
		URI theURI = lwTargetMap.getFile().toURI();
		lwWormhole.setTargetResourceMapURI(theURI);
		assertTrue(lwWormhole.getTargetResourceMapURI().equals(theURI));
	}
	
	public void testGetTargetResourceMapURI() {
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwTargetMap.setFile(targetFile);
		URI theURI = lwTargetMap.getFile().toURI();
		lwWormhole.setTargetResourceMapURI(theURI);
		assertTrue(lwWormhole.getTargetResourceMapURI().equals(theURI));
	}	
	
	public void testSetSourceResourceComponentURI() {
		URI theURI = lwSourceNode.getURI();
		lwWormhole.setSourceResourceComponentURI(theURI);
		assertTrue(lwWormhole.getSourceResourceComponentURI().equals(theURI));
	}
	
	public void testGetSourceResourceComponentURIString() {
		URI theURI = lwSourceNode.getURI();
		String theString = theURI.toString();
		lwWormhole.setSourceResourceComponentURI(theURI);
		assertTrue(lwWormhole.getSourceResourceComponentURIString().equals(theString));
	}
	
	public void testGetTargetResourceComponentURIString() {
		URI theURI = lwTargetNode.getURI();
		String theString = theURI.toString();
		lwWormhole.setTargetResourceComponentURI(theURI);
		assertTrue(lwWormhole.getTargetResourceComponentURIString().equals(theString));
	}
	
	public void testGetSourceResourceComponentURI() {
		URI theURI = lwSourceNode.getURI();
		lwWormhole.setSourceResourceComponentURI(theURI);
		assertTrue(lwWormhole.getSourceResourceComponentURI().equals(theURI));
	}
	
	public void testSetTargetResourceComponentURI() {
		URI theURI = lwTargetNode.getURI();
		lwWormhole.setTargetResourceComponentURI(theURI);
		assertTrue(lwWormhole.getTargetResourceComponentURI().equals(theURI));
	}
	
	public void testGetTargetResourceComponentURI() {
		URI theURI = lwTargetNode.getURI();
		lwWormhole.setTargetResourceComponentURI(theURI);
		assertTrue(lwWormhole.getTargetResourceComponentURI().equals(theURI));
	}	
	
	public void testSetResourceURIs() {
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwSourceMap.setFile(sourceFile);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setSourceMapFile(sourceFile);
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwTargetMap.setFile(targetFile);
		lwWormhole.setTargetMap(lwTargetMap);
		lwWormhole.setTargetMapFile(targetFile);
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setResourceURIs();
		assertTrue(lwWormhole.getSourceResourceMapURI().equals(lwWormhole.getTargetMapFile().toURI()));
		assertTrue(lwWormhole.getTargetResourceMapURI().equals(lwWormhole.getSourceMapFile().toURI()));
		assertTrue(lwWormhole.getSourceResourceComponentURI().equals(lwWormhole.getTargetComponent().getURI()));
		assertTrue(lwWormhole.getTargetResourceComponentURI().equals(lwWormhole.getSourceComponent().getURI()));
		
	} 
	
	public void testCreateResources() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode sourceWormholeNode = lwWormhole.createSourceWormholeNode();
		lwWormhole.setSourceWormholeNode(sourceWormholeNode);
		LWWormholeNode targetWormholeNode = lwWormhole.createTargetWormholeNode();
		lwWormhole.setTargetWormholeNode(targetWormholeNode);	
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwWormhole.setSourceMapFile(sourceFile);
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwWormhole.setTargetMapFile(targetFile);
		lwWormhole.setResourceURIs();
		lwWormhole.createResources();
		assertFalse(lwWormhole.getSourceResource().equals(null));
		assertFalse(lwWormhole.getTargetResource().equals(null));
		
	}
	
	public void testSetSourceResource() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode sourceWormholeNode = lwWormhole.createSourceWormholeNode();
		lwWormhole.setSourceWormholeNode(sourceWormholeNode);
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwWormhole.setSourceMapFile(sourceFile);
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwWormhole.setTargetMapFile(targetFile);		
		lwWormhole.setResourceURIs();
		lwWormhole.setSourceResource(sourceWormholeNode.getResourceFactory().get(lwWormhole.getSourceResourceMapURI(), lwWormhole.getSourceResourceComponentURI()));
		assertFalse(lwWormhole.getSourceResource().equals(null));
	}
	
	public void testGetSourceResource() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode sourceWormholeNode = lwWormhole.createSourceWormholeNode();
		lwWormhole.setSourceWormholeNode(sourceWormholeNode);
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwWormhole.setSourceMapFile(sourceFile);
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwWormhole.setTargetMapFile(targetFile);		
		lwWormhole.setResourceURIs();
		lwWormhole.setSourceResource(sourceWormholeNode.getResourceFactory().get(lwWormhole.getSourceResourceMapURI(), lwWormhole.getSourceResourceComponentURI()));
		assertFalse(lwWormhole.getSourceResource().equals(null));
	}	
	
	public void testSetTargetResource() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode targetWormholeNode = lwWormhole.createTargetWormholeNode();
		lwWormhole.setTargetWormholeNode(targetWormholeNode);
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwWormhole.setSourceMapFile(sourceFile);
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwWormhole.setTargetMapFile(targetFile);		
		lwWormhole.setResourceURIs();
		lwWormhole.setTargetResource(targetWormholeNode.getResourceFactory().get(lwWormhole.getTargetResourceMapURI(), lwWormhole.getTargetResourceComponentURI()));
		assertFalse(lwWormhole.getTargetResource().equals(null));
	}	
	
	public void testGetTargetResource() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode targetWormholeNode = lwWormhole.createTargetWormholeNode();
		lwWormhole.setTargetWormholeNode(targetWormholeNode);
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwWormhole.setSourceMapFile(sourceFile);
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwWormhole.setTargetMapFile(targetFile);		
		lwWormhole.setResourceURIs();
		lwWormhole.setTargetResource(targetWormholeNode.getResourceFactory().get(lwWormhole.getTargetResourceMapURI(), lwWormhole.getTargetResourceComponentURI()));
		assertFalse(lwWormhole.getTargetResource().equals(null));
	}	
	
	public void testSetResources() {
		lwWormhole.setSourceComponent(lwSourceNode);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetComponent(lwTargetNode);
		lwWormhole.setTargetMap(lwTargetMap);
		LWWormholeNode sourceWormholeNode = lwWormhole.createSourceWormholeNode();
		lwWormhole.setSourceWormholeNode(sourceWormholeNode);
		LWWormholeNode targetWormholeNode = lwWormhole.createTargetWormholeNode();
		lwWormhole.setTargetWormholeNode(targetWormholeNode);	
		File sourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwWormhole.setSourceMapFile(sourceFile);
		File targetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwWormhole.setTargetMapFile(targetFile);
		lwWormhole.setResourceURIs();
		lwWormhole.createResources();
		lwWormhole.setResources();
		assertTrue(lwWormhole.getSourceWormholeNode().getResource().equals(lwWormhole.getSourceResource()));
		assertTrue(lwWormhole.getTargetWormholeNode().getResource().equals(lwWormhole.getTargetResource()));
	}
	
	public void testGetSourceComponent() {
		lwWormhole.setSourceComponent(lwSourceNode);
		assertTrue(lwWormhole.getSourceComponent().equals(lwSourceNode));
	}
	
	public void testGetSourceMap() {
		lwWormhole.setSourceMap(lwSourceMap);
		assertTrue(lwWormhole.getSourceMap().equals(lwSourceMap));
	}
	
	public void testGetTargetComponent() {
		lwWormhole.setTargetComponent(lwTargetNode);
		assertTrue(lwWormhole.getTargetComponent().equals(lwTargetNode));
	}
	
	public void testGetTargetMap() {
		lwWormhole.setTargetMap(lwTargetMap);
		assertTrue(lwWormhole.getTargetMap().equals(lwTargetMap));
	}	
	
	public void testIsMapFileChanged() {
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setTargetMap(lwTargetMap);
		File sourceFile = lwSourceMap.getFile();
		File targetFile = lwTargetMap.getFile();
		lwWormhole.setSourceMapFile(sourceFile);
		lwWormhole.setTargetMapFile(targetFile);
		
		boolean bChanged = lwWormhole.isMapFileChanged();
		assertTrue(bChanged == false);
		
		File newSourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwWormhole.setSourceMapFile(newSourceFile);
		bChanged = lwWormhole.isMapFileChanged();
		assertTrue(bChanged == true);
	} 
	
	public void testGetSourceMapFilename() {
		File newSourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwSourceMap.setFile(newSourceFile);
		String thePath = newSourceFile.getAbsolutePath();
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setSourceMapFile(newSourceFile);
		String thatPath = lwWormhole.getSourceMapFileName();
		assertTrue(thatPath.equals(thePath));
	}
	
	public void testGetTargetMapFilename() {
		File newTargetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwTargetMap.setFile(newTargetFile);
		String thePath = newTargetFile.getAbsolutePath();
		lwWormhole.setTargetMap(lwTargetMap);
		lwWormhole.setTargetMapFile(newTargetFile);
		String thatPath = lwWormhole.getTargetMapFileName();
		assertTrue(thatPath.equals(thePath));
	}
	
	public void testGetSourceResourceMapURIString() {
		File newTargetFile = new File(System.getProperty("user.dir") + "targetMapFile.vue");
		lwTargetMap.setFile(newTargetFile);
		lwWormhole.setTargetMap(lwTargetMap);
		lwWormhole.setTargetMapFile(newTargetFile);
		URI theURI = newTargetFile.toURI();
		String theString = theURI.toString();
		lwWormhole.setSourceResourceMapURI(theURI);
		String thatString = lwWormhole.getSourceResourceMapURIString();
		assertTrue(thatString.equals(theString));
	}
	
	public void testGetTargetResourceMapURIString() {
		File newSourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwSourceMap.setFile(newSourceFile);
		lwWormhole.setSourceMap(lwSourceMap);
		lwWormhole.setSourceMapFile(newSourceFile);
		URI theURI = newSourceFile.toURI();
		String theString = theURI.toString();
		lwWormhole.setTargetResourceMapURI(theURI);
		String thatString = lwWormhole.getTargetResourceMapURIString();
		assertTrue(thatString.equals(theString));
	}

	// difficult to test this one without having any open maps
	public void testFindAndSelectComponentAmongOpenMaps() {
		lwWormhole.findAndSelectComponentAmongOpenMaps(lwSourceNode);
		// well, there aren't any open maps, therefore
		// this component shouldn't have been selected...
		assertTrue(lwSourceNode.isSelected() == false);
	} 
	
	// difficult to test this one without having any open maps
	public void testReplaceExistingWormholeResource() {
		// still, let's try it with a null resource
		File newSourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwSourceMap.setFile(newSourceFile);
		LWWormholeNode newWormholeNode = new LWWormholeNode(lwSourceNode.getURI().toString(),
    			lwSourceMap.getURI().toString(), lwTargetNode.getURI().toString(),
    			lwTargetMap.getURI().toString(), LWWormholeNode.WormholeType.SOURCE);
		WormholeResource wr = (WormholeResource)newWormholeNode.getResourceFactory().get(newSourceFile.toURI(), lwSourceNode.getURI());
		lwWormhole.replaceExistingWormholeResource(newWormholeNode, wr, lwSourceMap);
		assertFalse(newWormholeNode.getResource() == null);
		assertTrue(newWormholeNode.getResource().equals(wr));
	}
	
	public void testCheckFileSavedAndReturn() {
		File newSourceFile = new File(System.getProperty("user.dir") + "sourceMapFile.vue");
		lwSourceMap.setFile(newSourceFile);
		File f = lwWormhole.checkFileSavedAndReturn(newSourceFile, lwSourceMap);
		assertTrue(f.equals(newSourceFile));
	}

}
