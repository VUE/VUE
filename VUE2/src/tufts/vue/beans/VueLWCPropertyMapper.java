
package tufts.vue.beans;


import java.awt.*;
import java.util.*;
import java.beans.*;
import javax.swing.*;
import tufts.vue.*;

/** This class describes a DynamicPropertyMapper: an object that
* can get and set dynamic properties of dynamic beans belonging to
* Java classes or interfaces for which it has registered.
*
*
**/

public class VueLWCPropertyMapper implements VuePropertyMapper {
	
	////////////////
	// Statics
	/////////////////
	
	static public final String kFillColor = "fillColor";
	static public final String kStrokeColor = "strokeColor";
	static public final String kTextColor = "textColor";
	static public final String kStrokeWeight = "stroke";
	static public final String kLinkArrowState = "linkArrows";
	static public final String kFont = "font";
	
	static final String [] sNodeProperties = {  kFillColor,
												kStrokeColor,
												kTextColor,
												kStrokeWeight,
												kFont
												};
												
	static final String  []  sLinkProperties = {  kStrokeColor,
												kStrokeWeight,
												kTextColor,
												kLinkArrowState,
												kFont
												};
												
	static final String [] sTextProperties = {  kTextColor,
												kFont
												};
												
												
	
	////////////
	// Fields
	/////////////
	
	VueBeanInfo mLWCInfo = null;
	VueBeanInfo mNodeInfo = null;
	VueBeanInfo mLinkInfo = null;
	
	
	////////////////
	// Constructors
	//////////////////
	
	
	//////////////
	// Methods
	///////////////
	
	
	
	//////////////////
	// VUePropertyMapper Interface
	//////////////////
	
	/**
	 * getPropertyValue
	 * Gets the value of the property from the specified object.
	 * @param Object - the object
	 * @param String name
	 * @return Object the value
	 **/
	public Object getPropertyValue(Object pBean, String pName)  {
		Object value = null;
		
		if(  pBean instanceof LWComponent) {
			LWComponent obj = (LWComponent) pBean;
			
			if( pName.equals( kFillColor) ) {
				return obj.getFillColor();
				}
			if( pName.equals(kStrokeColor) ) {
				return obj.getStrokeColor();
				}
			if( pName.equals(kTextColor) ) {
				return obj.getTextColor();
				}
			if( pName.equals( kStrokeWeight) ) {
				float f;
				Float width = new Float( obj.getStrokeWidth() );
				return width;
				}
			if( pName.equals( kFont) ) {
				return obj.getFont();
				}
			
			if( obj instanceof LWLink ) {
				LWLink link = (LWLink) obj;

				
				if( pName.equals( kLinkArrowState) ) {
					Integer state = new Integer( link.getArrowState() );
					return state;
					}
				}
			}
		else {
			// should never happen...
			System.out.println("Error - VueLWCPropertyMapper mapped to wrong class.");
			}
		
		return value;
	}
	
	

	/**
	 * setPropertyValuddddddddddddddddddddddddddddddddde
	 * This sets the property with of the object with the passed value
	 * @param OpObject - the object
	 * @param String pName the proeprty name
	 * @param pValue - the value of the named property
	 **/
	public void setPropertyValue(Object pBean, String pName, Object pValue)   {
		if(  pBean instanceof LWComponent) {
			LWComponent obj = (LWComponent) pBean;
			
			if( pName.equals( kFillColor) ) {
				obj.setFillColor( (Color) pValue);
				}
			else
			if( pName.equals(kStrokeColor) ) {
				obj.setStrokeColor( (Color) pValue);
				}
			else
			if( pName.equals(kTextColor) ) {
				obj.setTextColor( (Color) pValue);
				}
			else
			if( pName.equals( kStrokeWeight) ) {
				float value = ((Float) pValue).floatValue();
				obj.setStrokeWidth( value);
				}
			else
			if( pName.equals( kFont) ) {
				debug(" setting font");
				obj.setFont( (Font) pValue);
				}
			
			if( obj instanceof LWLink ) {
				LWLink link = (LWLink) obj;

				if( pName.equals( kLinkArrowState) ) {
					int state = ((Integer) pValue).intValue();
					link.setArrowState( state);
					}
				}
			}
		else {
			// should never happen...
			System.out.println("Error - VueLWCPropertyMapper mapped to wrong class.");
			}
		
	}


	/**
	 * getBeanInfo
	 * Returns the VueBeanInfo for the object.
	 * @param Object - the object
	 * @return VueBeanInfo the info for the object.
	 **/
	public VueBeanInfo getBeanInfo(Object pObject)  {
		VueBeanInfo beanInfo = null;
		if( pObject instanceof LWComponent) {
			beanInfo = new LWCBeanInfo( (LWComponent) pObject);
			}
		return beanInfo;
	}

	
	public class LWCBeanInfo implements VueBeanInfo {
		
		String [] mPropertyNames = null;
		VuePropertyDescriptor [] mDescriptors = null;
		Map mMap = null;
		
		
		LWCBeanInfo( LWComponent pLWC ) {
			
			if( pLWC instanceof LWNode) {
				if( false) {
					mPropertyNames = sNodeProperties;
					}
				else {
					mPropertyNames = sTextProperties;
					}
				}
			else
			if( pLWC instanceof LWLink) {
				mPropertyNames = sLinkProperties;
			}
			
			if( mPropertyNames != null) {
				mDescriptors = new VuePropertyDescriptor[ mPropertyNames.length];
				mMap = new HashMap();
				VuePropertyDescriptor desc = null;
				for(int i=0; i<mPropertyNames.length; i++) {
					desc = createDescriptor( mPropertyNames[i]);
					mDescriptors[i] = desc;
					mMap.put( mPropertyNames[i], desc);
					}
				}
		}
		
		public VuePropertyDescriptor[] getPropertyDescriptors() {
	 		return mDescriptors;
	 	}


		public boolean hasProperty( String pName) {
	 		boolean hasKey = mMap.containsKey( pName);
	 	return hasKey;
	 	}
	  

		public String [] getPropertyNames() {
	 		return mPropertyNames;
	 	}


		public VuePropertyDescriptor getPropertyDescriptor( String pName) {
	 		return (VuePropertyDescriptor) mMap.get( pName);
	 	}


	 			
	
	static public final String kStrokeWeight = "stroke";
	static public final String kLinkArrowState = "linkArrows";
	static public final String kFont = "font";
		
		private VuePropertyDescriptor createDescriptor( String pName) {
			VuePropertyDescriptor desc = null;
			String str = new String();
			Font font = new Font("Default",1,Font.PLAIN);
			Integer  i = new Integer(1);
			Color color = new Color(0,0,0);;
			Class theClass = null;
			
			if( pName.equals(kFillColor) ||
				pName.equals( kStrokeColor) ||
				pName.equals( kTextColor) ) {
				theClass = color.getClass();
				}
			else
			if( pName.equals( kStrokeWeight) ) {
				Float thefloat = new Float(0);
				theClass = thefloat.getClass();
			}
			else
			if( pName.equals( kLinkArrowState) ) {
				
				theClass = i.getClass();
				}
			else
			if( pName.equals( kFont) ) {
				theClass = font.getClass();
				}
				
			desc = new VuePropertyDescriptor( pName, theClass, null);
			
			return desc;
		}
	}
	
	boolean sDebug = true;
	private void debug( String s) {
		if( sDebug)
			System.out.println( s);
	}
}






