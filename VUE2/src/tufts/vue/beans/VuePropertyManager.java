/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */



package tufts.vue.beans;

import java.lang.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.beans.*;

public class VuePropertyManager extends PropertyEditorManager  {

	
	static private VuePropertyManager sMgr = new VuePropertyManager();
	
	/** editors keyed by Class **/
	private Map mClassEditors = new HashMap();
	
	/** editors keyed by our property names **/
	private Map mPropEditors = new HashMap();
	
	private Map mMappers = new HashMap();
	
	private VuePropertyManager() {
		super();
		registerOurEditors();
		registerOurMappers();
	}
	
	
	private void registerOurEditors() {
	
		
	
	}
	private void registerOurMappers() {
		
		VueLWCPropertyMapper lwcMapper = new VueLWCPropertyMapper();
		mMappers.put("tufts.vue.LWNode", lwcMapper);
		mMappers.put("tufts.vue.LWLink", lwcMapper);
		mMappers.put("tufts.vue.LWComponent", lwcMapper);
		mMappers.put("tufts.vue.LWImage", lwcMapper);
	}
	
	static public VuePropertyManager getManager() {
		
		return sMgr;
	}
	
	
	
	public VuePropertyMapper getPropertyMapperFromClass( Class pClass) {
		VuePropertyMapper mapper = null;
		
		Object obj = mMappers.get( pClass.getName() );
		if( obj != null) {
			mapper = (VuePropertyMapper) obj;
			}
		return mapper;
	}
	
	public VuePropertyMapper getPropertyMapper( Object pObject) {
		return getPropertyMapperFromClass( pObject.getClass() );
	}
}