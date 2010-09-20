package test.src.tufts.vue;

import java.lang.*;
import java.net.*;

import tufts.vue.*;
import tufts.vue.XLinkResource.XLinkType;
import junit.framework.*;

public class XLinkResourceTest extends TestCase {
	
	private XLinkResource xLinkResource;
	
	protected void setUp() {
		xLinkResource = new XLinkResource();
	}
	
	protected void tearDown() {
		xLinkResource = null;
	}

	public void testXLink () {
		assertTrue(!xLinkResource.equals(null));
	}
	
	public void testSetType() {
		xLinkResource.setType(XLinkResource.XLinkType.SIMPLE);
		assertTrue(xLinkResource.getType().toString().equals(XLinkType.SIMPLE.toString()));
	}
	
	public void testGetType() {
		xLinkResource.setType(XLinkResource.XLinkType.SIMPLE);
		String type = xLinkResource.getType();
		assertTrue(type.equals(XLinkType.SIMPLE.toString()));
	}
	
	public void testSetHref() {
		xLinkResource.setHref("http://www.google.co.uk/");
		assertTrue(xLinkResource.getHref().equals("http://www.google.co.uk/"));
	}
	
	public void testGetHref() {
		xLinkResource.setHref("http://www.google.co.uk/");
		assertTrue(xLinkResource.getHref().equals("http://www.google.co.uk/"));
	}
	
	public void testSetShow() {
		xLinkResource.setShow(XLinkResource.XLinkShow.NEW);
		assertTrue(xLinkResource.getShow().equals(XLinkResource.XLinkShow.NEW.toString()));
	}
	
	public void testGetShow() {
		xLinkResource.setShow(XLinkResource.XLinkShow.NEW);
		String theShow = xLinkResource.getShow();
		assertTrue(theShow.equals(XLinkResource.XLinkShow.NEW.toString()));
	}	
	
	public void testSetActuate() {
		xLinkResource.setActuate(XLinkResource.XLinkActuate.ON_REQUEST);
		assertTrue(xLinkResource.getActuate().equals(XLinkResource.XLinkActuate.ON_REQUEST.toString()));
	}
	
	public void testGetActuate() {
		xLinkResource.setActuate(XLinkResource.XLinkActuate.ON_LOAD);
		String theActuate = xLinkResource.getActuate();
		assertTrue(theActuate.equals(XLinkResource.XLinkActuate.ON_LOAD.toString()));
	}	
	/* public static Test suite() {
		return new TestSuite(XLinkHandlerTest.class);
	} */

}
