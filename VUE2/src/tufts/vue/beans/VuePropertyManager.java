

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