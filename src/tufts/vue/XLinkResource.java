/* HO 06/07/2010 BEGIN NEW CLASS */

package tufts.vue;

import java.lang.*;
import java.net.URL;
import java.util.ArrayList;

/**
 * @author helenoliver
 * XLinkResource, a handler for XLinks between one resource and another in Vue.
 * Also handles XPointers.
 *
 */
public class XLinkResource extends URLResource {
	/**
	 * The type of the XLink and/or XPointer: 
	 */
	private XLinkType type;
	/**
	 * the href property of the XLink
	 */
	private String href;
	
	/**
	 * the show property of the XLink
	 */
	private XLinkShow show;
	
	/**
	 * the actuate property of the XLink
	 */
	private XLinkActuate actuate;
	
	/**
	 * no-arg constructor for the XLink object
	 */
	public XLinkResource() {
		
	}
	
	/**
	 * @param theType, an XLinkType which will become the type of this XLink
	 */
	public void setType(XLinkType theType) {
		this.type = theType;
	}
	
	/**
	 * @return type, a String representation of the XLinkType representing the type of this XLinkHandler
	 */
	public String getType() {
		return type.toString();
	} 
	
	/**
	 * @param theShow, an XLinkShow representing the show property of this XLink
	 */
	public void setShow(XLinkShow theShow) {
		this.show = theShow;
	}
	
	/**
	 * @return actuate, a String representation of the XLinkActuate representing the actuate property of this XLink
	 */
	public String getActuate() {
		return actuate.toString();
	}	
	
	/**
	 * @param theActuate, an XLinkActuate representing the actuate property of this XLink
	 */
	public void setActuate(XLinkActuate theActuate) {
		this.actuate = theActuate;
	}
			
	/**
	 * @return show, a String representing the XLinkShow representing the show property of this XLink
	 */
	public String getShow() {
		return show.toString();
	}	
	
	/**
	 * @param theHref, a String representing the href property of this XLink
	 */
	public void setHref(String theHref) {
		this.href = theHref;
	}
	
	/**
	 * @return href, a String representing the href property of this XLink
	 */
	public String getHref() {
		return href;
	}	
	
	public enum XLinkType {
		SIMPLE("simple"), EXTENDED("extended"), LOCATOR("locator"), ARC("arc"), 
			RESOURCE("resource"), TITLE("title"), NONE("none"); 
		
		XLinkType(String strType) {
			this.type = strType;
		}
		
		private String type;
		
		public String getType() {
			return type;
		}
	}
	
	public enum XLinkShow { 
		EMBED("embed"), NEW("new"), REPLACE("replace"), OTHER("other"), NONE("none");
		
		XLinkShow(String strShow) {
			this.show = strShow;
		}
		
		private String show;
		
		public String getShow() {
			return show;
		}
	}
	
	public enum XLinkActuate { 
		ON_LOAD("onLoad"), ON_REQUEST("onRequest"), OTHER("other"), NONE("none");
		
		XLinkActuate(String strActuate) {
			this.actuate = strActuate;
		}
		
		private String actuate;
		
		public String getActuate() {
			return actuate;
		}
	}

}
/* HO 06/07/2010 END NEW CLASS */



